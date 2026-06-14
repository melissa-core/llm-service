package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.CustomerMemoryChatState;
import uz.melisa.domain.CustomerMemoryEpisode;
import uz.melisa.domain.CustomerMemoryProcessingJob;
import uz.melisa.domain.CustomerMemorySettings;
import uz.melisa.dto.memory.ClaimedSegment;
import uz.melisa.dto.memory.ExtractedMemory;
import uz.melisa.enums.MemoryProcessingJobStatus;
import uz.melisa.repository.CustomerMemoryChatStateRepository;
import uz.melisa.repository.CustomerMemoryProcessingJobRepository;
import uz.melisa.repository.CustomerMemorySettingsRepository;
import uz.melisa.repository.MessageRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The transactional core of segment processing: each public method is its own short transaction so
 * that the Haiku call (driven by {@code MemorySegmentProcessor}) happens strictly between the claim
 * and the commit, never inside a transaction or while holding a lock.
 *
 * <p>A retried range is always reclaimed by its original (chat, from, until) job row; lease claims
 * are atomic conditional updates that must affect exactly one row.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemorySegmentJobService {

    private static final long LEASE_DURATION_MS = 5L * 60 * 1000;
    private static final int MAX_SEGMENT_MESSAGES = 20;
    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_BACKOFF_BASE_MS = 60L * 1000;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final CustomerMemoryChatStateRepository chatStateRepository;
    private final CustomerMemoryProcessingJobRepository jobRepository;
    private final CustomerMemorySettingsRepository settingsRepository;
    private final MessageRepository messageRepository;
    private final MemoryWriteService memoryWriteService;
    private final MemoryPromotionService memoryPromotionService;

    /** Atomically claim the next range for a chat: reclaim the existing range job, or open a fresh one. */
    @Transactional
    public Optional<ClaimedSegment> claimNextSegment(Long chatId, String workerId) {
        CustomerMemoryChatState state = chatStateRepository.findByChatIdForUpdate(chatId).orElse(null);
        if (state == null) {
            return Optional.empty();
        }
        CustomerMemorySettings settings = settingsRepository.findById(state.getCustomerId()).orElse(null);
        if (settings == null || !settings.isMemoryEnabled()) {
            return Optional.empty();
        }
        if (hasActiveUnexpiredJob(state)) {
            return Optional.empty();   // overlap prevention
        }

        long fromSeq = state.getLastCompletedMessageSeq() + 1;   // gap prevention: always resume at the cursor
        Optional<CustomerMemoryProcessingJob> existingAtStart =
                jobRepository.findFirstByChatIdAndFromMessageSeqOrderByIdDesc(chatId, fromSeq);
        if (existingAtStart.isPresent()) {
            return reclaimSameRange(existingAtStart.get(), state, settings, workerId);
        }
        return claimFreshRange(chatId, state, settings, fromSeq, workerId);
    }

    private Optional<ClaimedSegment> reclaimSameRange(CustomerMemoryProcessingJob existing, CustomerMemoryChatState state,
                                                      CustomerMemorySettings settings, String workerId) {
        if (existing.getStatus() == MemoryProcessingJobStatus.DONE) {
            return Optional.empty();   // already summarized; the cursor advance will have moved past this range
        }
        UUID leaseToken = UUID.randomUUID();
        int updatedRows = jobRepository.reclaimLease(
                existing.getId(), workerId, leaseToken, future(LEASE_DURATION_MS), settings.getGeneration(), now());
        if (updatedRows != 1) {
            return Optional.empty();   // atomic lease claim: exactly one row, or nothing (active / backoff / poisoned)
        }
        return activate(state, settings, existing.getId(), existing.getFromMessageSeq(),
                existing.getUntilMessageSeq(), existing.getSegmentNumber(), leaseToken, workerId);
    }

    private Optional<ClaimedSegment> claimFreshRange(Long chatId, CustomerMemoryChatState state,
                                                     CustomerMemorySettings settings, long fromSeq, String workerId) {
        long watermark = messageRepository.findMaxMessageSeqByChatId(chatId);
        if (watermark < fromSeq) {
            return Optional.empty();   // nothing new
        }
        long untilSeq = Math.min(watermark, fromSeq + MAX_SEGMENT_MESSAGES - 1);
        String idempotencyKey = chatId + ":" + fromSeq + ":" + untilSeq;
        CustomerMemoryProcessingJob job = createJob(idempotencyKey, state, settings, fromSeq, untilSeq, workerId);
        jobRepository.flush();
        return activate(state, settings, job.getId(), fromSeq, untilSeq, job.getSegmentNumber(),
                job.getLeaseToken(), workerId);
    }

    private Optional<ClaimedSegment> activate(CustomerMemoryChatState state, CustomerMemorySettings settings, Long jobId,
                                              long fromSeq, long untilSeq, int segmentNumber, UUID leaseToken,
                                              String workerId) {
        state.setActiveJobId(jobId);
        chatStateRepository.save(state);
        return Optional.of(new ClaimedSegment(jobId, state.getChatId(), state.getCustomerId(),
                fromSeq, untilSeq, segmentNumber, leaseToken, settings.getGeneration(), workerId));
    }

    /** Heartbeat: extend the lease while extraction runs, only if this worker still holds the lease. */
    @Transactional
    public int renewLease(Long jobId, UUID leaseToken) {
        return jobRepository.renewLease(jobId, leaseToken, future(LEASE_DURATION_MS));
    }

    /** Persist episode + candidates/evidence, complete the job and advance the cursor in one short tx. */
    @Transactional
    public void commitSegment(ClaimedSegment segment, ExtractedMemory extraction) {
        Timestamp now = now();
        CustomerMemorySettings settings = settingsRepository.findByCustomerIdForUpdate(segment.customerId()).orElse(null);
        CustomerMemoryProcessingJob job = jobRepository.findById(segment.jobId()).orElse(null);
        CustomerMemoryChatState state = chatStateRepository.findByChatIdForUpdate(segment.chatId()).orElse(null);

        String failure = fenceFailure(segment, settings, job, state, now);
        if (failure != null) {
            abortJob(job, state, segment, failure);
            return;
        }

        CustomerMemoryEpisode episode = memoryWriteService.saveEpisode(segment, extraction);
        memoryPromotionService.applyFacts(segment, episode, extraction.facts());

        job.setStatus(MemoryProcessingJobStatus.DONE);
        job.setLastError(null);
        clearLease(job);
        jobRepository.save(job);

        state.setLastCompletedMessageSeq(segment.untilSeq());
        state.setNextSegmentNumber(state.getNextSegmentNumber() + 1);
        state.setActiveJobId(null);
        state.setPoisonAttempts(0);
        chatStateRepository.save(state);
    }

    /** Mark a job FAILED (retryable) or DEAD_LETTER and release the chat so the range can be retried. */
    @Transactional
    public void failJob(ClaimedSegment segment, String error) {
        CustomerMemoryProcessingJob job = jobRepository.findById(segment.jobId()).orElse(null);
        if (job == null || !ownsLease(job, segment)) {
            return;
        }
        int attempts = attempts(job);
        if (attempts >= job.getMaxAttempts()) {
            job.setStatus(MemoryProcessingJobStatus.DEAD_LETTER);
        } else {
            job.setStatus(MemoryProcessingJobStatus.FAILED);
            job.setNextRetryAt(future(RETRY_BACKOFF_BASE_MS * attempts));
        }
        job.setLastError(truncate(error));
        clearLease(job);
        jobRepository.save(job);
        releaseChatState(segment.chatId(), segment.jobId());
    }

    /** Reset IN_PROGRESS jobs whose lease expired so a healthy worker can re-claim the same range. */
    @Transactional
    public int recoverExpiredLeases() {
        Timestamp now = now();
        List<CustomerMemoryProcessingJob> expired =
                jobRepository.findExpiredLeases(MemoryProcessingJobStatus.IN_PROGRESS, now);
        for (CustomerMemoryProcessingJob job : expired) {
            if (attempts(job) >= job.getMaxAttempts()) {
                job.setStatus(MemoryProcessingJobStatus.DEAD_LETTER);
            } else {
                job.setStatus(MemoryProcessingJobStatus.FAILED);
                job.setNextRetryAt(now);
            }
            job.setLastError("lease expired; recovered");
            clearLease(job);
            jobRepository.save(job);
            releaseChatState(job.getChatId(), job.getId());
        }
        return expired.size();
    }

    private String fenceFailure(ClaimedSegment segment, CustomerMemorySettings settings,
                                CustomerMemoryProcessingJob job, CustomerMemoryChatState state, Timestamp now) {
        if (settings == null) {
            return "settings missing";
        }
        if (!settings.isMemoryEnabled()) {
            return "memory disabled";
        }
        if (settings.getGeneration() == null || settings.getGeneration() != segment.generation()) {
            return "generation changed";
        }
        if (job == null) {
            return "job missing";
        }
        if (job.getStatus() != MemoryProcessingJobStatus.IN_PROGRESS) {
            return "job not in progress";
        }
        if (!ownsLease(job, segment)) {
            return "lease token changed";
        }
        if (job.getLockedUntil() == null || job.getLockedUntil().before(now)) {
            return "lease expired";
        }
        if (state == null || !segment.jobId().equals(state.getActiveJobId())) {
            return "active job changed";
        }
        return null;
    }

    private void abortJob(CustomerMemoryProcessingJob job, CustomerMemoryChatState state,
                          ClaimedSegment segment, String reason) {
        log.warn("aborting memory job jobId={} chatId={} reason={}", segment.jobId(), segment.chatId(), reason);
        if (job != null && ownsLease(job, segment)) {
            job.setStatus(MemoryProcessingJobStatus.ABORTED);
            job.setLastError(truncate(reason));
            clearLease(job);
            jobRepository.save(job);
        }
        if (state != null && segment.jobId().equals(state.getActiveJobId())) {
            state.setActiveJobId(null);
            chatStateRepository.save(state);
        }
    }

    private CustomerMemoryProcessingJob createJob(String idempotencyKey, CustomerMemoryChatState state,
                                                  CustomerMemorySettings settings, long fromSeq, long untilSeq,
                                                  String workerId) {
        return jobRepository.save(CustomerMemoryProcessingJob.builder()
                .idempotencyKey(idempotencyKey)
                .customerId(state.getCustomerId())
                .chatId(state.getChatId())
                .segmentNumber(state.getNextSegmentNumber())
                .fromMessageSeq(fromSeq)
                .untilMessageSeq(untilSeq)
                .customerMemoryGeneration(settings.getGeneration())
                .status(MemoryProcessingJobStatus.IN_PROGRESS)
                .workerId(workerId)
                .leaseToken(UUID.randomUUID())
                .lockedUntil(future(LEASE_DURATION_MS))
                .attempts(1)
                .maxAttempts(MAX_ATTEMPTS)
                .build());
    }

    private boolean hasActiveUnexpiredJob(CustomerMemoryChatState state) {
        if (state.getActiveJobId() == null) {
            return false;
        }
        return jobRepository.findById(state.getActiveJobId())
                .filter(job -> job.getStatus() == MemoryProcessingJobStatus.IN_PROGRESS)
                .filter(job -> job.getLockedUntil() != null && job.getLockedUntil().after(now()))
                .isPresent();
    }

    private void releaseChatState(Long chatId, Long jobId) {
        chatStateRepository.findByChatIdForUpdate(chatId).ifPresent(state -> {
            if (jobId.equals(state.getActiveJobId())) {
                state.setActiveJobId(null);
                chatStateRepository.save(state);
            }
        });
    }

    private boolean ownsLease(CustomerMemoryProcessingJob job, ClaimedSegment segment) {
        return job.getLeaseToken() != null && job.getLeaseToken().equals(segment.leaseToken());
    }

    private void clearLease(CustomerMemoryProcessingJob job) {
        job.setLockedUntil(null);
        job.setLeaseToken(null);
    }

    private int attempts(CustomerMemoryProcessingJob job) {
        return job.getAttempts() == null ? 0 : job.getAttempts();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }

    private Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    private Timestamp future(long offsetMs) {
        return new Timestamp(System.currentTimeMillis() + offsetMs);
    }
}

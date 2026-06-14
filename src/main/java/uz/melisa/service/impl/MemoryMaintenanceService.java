package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.CustomerMemoryAudit;
import uz.melisa.domain.CustomerMemoryDeletionOutbox;
import uz.melisa.domain.CustomerMemoryProcessingJob;
import uz.melisa.enums.MemoryAuditAction;
import uz.melisa.enums.MemoryDeletionOutboxStatus;
import uz.melisa.enums.MemoryProcessingJobStatus;
import uz.melisa.repository.CustomerMemoryAuditRepository;
import uz.melisa.repository.CustomerMemoryCandidateEvidenceRepository;
import uz.melisa.repository.CustomerMemoryCandidateRepository;
import uz.melisa.repository.CustomerMemoryChatStateRepository;
import uz.melisa.repository.CustomerMemoryDeletionOutboxRepository;
import uz.melisa.repository.CustomerMemoryEpisodeRepository;
import uz.melisa.repository.CustomerMemoryProcessingJobRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scheduled maintenance for the memory subsystem: prune expired data, recover stuck leases, apply the
 * poison-segment policy (MVP = skip-with-audit + advance cursor), retain completed jobs, and drive the
 * deletion outbox. The outbox retries until DONE; after the fast-retry budget it moves to
 * NEEDS_ATTENTION (and alerts) but keeps retrying slowly — purge events are never silently discarded.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryMaintenanceService {

    private static final int MAX_FAST_RETRIES = 5;
    private static final long DONE_JOB_RETENTION_MS = 30L * 24 * 60 * 60 * 1000;
    private static final long FAST_RETRY_BACKOFF_MS = 60_000L;
    private static final long SLOW_RETRY_BACKOFF_MS = 30L * 60 * 1000;
    private static final String ACTION = "action";

    private final CustomerMemoryEpisodeRepository episodeRepository;
    private final CustomerMemoryCandidateEvidenceRepository evidenceRepository;
    private final CustomerMemoryCandidateRepository candidateRepository;
    private final CustomerMemoryProcessingJobRepository jobRepository;
    private final CustomerMemoryChatStateRepository chatStateRepository;
    private final CustomerMemoryDeletionOutboxRepository deletionOutboxRepository;
    private final CustomerMemoryAuditRepository auditRepository;
    private final MemorySegmentJobService jobService;
    private final MemoryFactCacheService factCacheService;
    private final MemoryMetrics metrics;

    @Scheduled(fixedDelayString = "${melisa.memory.cleanup-interval-ms:600000}")
    @Transactional
    public void cleanupExpired() {
        Timestamp now = now();
        int episodes = episodeRepository.deleteExpiredBefore(now);
        int evidence = evidenceRepository.deleteExpiredBefore(now);
        int candidates = candidateRepository.expirePendingWithoutActiveEvidence(now)
                + candidateRepository.expirePendingBefore(now);
        if (episodes + evidence + candidates > 0) {
            log.info("memory cleanup: episodesDeleted={} evidenceDeleted={} candidatesExpired={}",
                    episodes, evidence, candidates);
        }
    }

    @Scheduled(fixedDelayString = "${melisa.memory.lease-recovery-interval-ms:60000}")
    public void recoverLeases() {
        try {
            metrics.leaseRecovered(jobService.recoverExpiredLeases());
        } catch (Exception e) {
            log.warn("memory lease recovery failed", e);
        }
    }

    @Scheduled(fixedDelayString = "${melisa.memory.job-retention-interval-ms:3600000}")
    @Transactional
    public void cleanCompletedJobs() {
        int deleted = jobRepository.deleteDoneBefore(new Timestamp(System.currentTimeMillis() - DONE_JOB_RETENTION_MS));
        if (deleted > 0) {
            log.info("memory cleanup: removed {} completed job(s) past retention", deleted);
        }
    }

    /** Poison-segment policy (MVP, configurable): skip the dead-letter segment with an audit and advance the cursor. */
    @Scheduled(fixedDelayString = "${melisa.memory.poison-interval-ms:300000}")
    @Transactional
    public void applyPoisonPolicy() {
        for (CustomerMemoryProcessingJob job : jobRepository.findAllByStatus(MemoryProcessingJobStatus.DEAD_LETTER)) {
            metrics.jobDeadLetter();
            chatStateRepository.findByChatIdForUpdate(job.getChatId()).ifPresent(state -> {
                if (job.getUntilMessageSeq() > state.getLastCompletedMessageSeq()) {
                    state.setLastCompletedMessageSeq(job.getUntilMessageSeq());   // skip past the poison segment
                }
                if (job.getId().equals(state.getActiveJobId())) {
                    state.setActiveJobId(null);
                }
                state.setPoisonAttempts((state.getPoisonAttempts() == null ? 0 : state.getPoisonAttempts()) + 1);
                chatStateRepository.save(state);
            });
            job.setStatus(MemoryProcessingJobStatus.ABORTED);   // consumed; will not be re-picked
            jobRepository.save(job);
            auditRepository.save(CustomerMemoryAudit.builder()
                    .customerId(job.getCustomerId())
                    .action(MemoryAuditAction.SEGMENT_SKIPPED)
                    .metadata(Map.of(ACTION, MemoryAuditAction.SEGMENT_SKIPPED.name(),
                            "fromSeq", String.valueOf(job.getFromMessageSeq()),
                            "untilSeq", String.valueOf(job.getUntilMessageSeq())))
                    .build());
            log.warn("poison segment skipped (skip-with-audit) jobId={} chatId={} range={}-{}",
                    job.getId(), job.getChatId(), job.getFromMessageSeq(), job.getUntilMessageSeq());
        }
    }

    @Scheduled(fixedDelayString = "${melisa.memory.deletion-outbox-interval-ms:120000}")
    public void processDeletionOutbox() {
        Timestamp now = now();
        List<CustomerMemoryDeletionOutbox> events = new ArrayList<>();
        events.addAll(deletionOutboxRepository.findAllByStatus(MemoryDeletionOutboxStatus.PENDING));
        events.addAll(deletionOutboxRepository.findAllByStatus(MemoryDeletionOutboxStatus.FAILED));
        events.addAll(deletionOutboxRepository.findAllByStatus(MemoryDeletionOutboxStatus.NEEDS_ATTENTION));
        for (CustomerMemoryDeletionOutbox event : events) {
            if (event.getNextRetryAt() != null && event.getNextRetryAt().after(now)) {
                continue;   // honor backoff
            }
            processOutboxEvent(event);
        }
    }

    private void processOutboxEvent(CustomerMemoryDeletionOutbox event) {
        boolean purged = factCacheService.evictGeneration(event.getCustomerId(), event.getGeneration());
        if (purged) {
            event.setStatus(MemoryDeletionOutboxStatus.DONE);
            event.setLastError(null);
            event.setNextRetryAt(null);
            deletionOutboxRepository.save(event);
            return;
        }
        int attempts = (event.getAttempts() == null ? 0 : event.getAttempts()) + 1;
        event.setAttempts(attempts);
        event.setLastError("redis purge failed");
        metrics.deletionOutboxFailed();
        if (attempts >= MAX_FAST_RETRIES) {
            event.setStatus(MemoryDeletionOutboxStatus.NEEDS_ATTENTION);   // keep retrying slowly, never discard
            event.setNextRetryAt(new Timestamp(System.currentTimeMillis() + SLOW_RETRY_BACKOFF_MS));
            metrics.deletionOutboxNeedsAttention();
            log.error("ALERT memory deletion outbox needs attention customerId={} generation={} attempts={}",
                    event.getCustomerId(), event.getGeneration(), attempts);
        } else {
            event.setStatus(MemoryDeletionOutboxStatus.FAILED);
            event.setNextRetryAt(new Timestamp(System.currentTimeMillis() + FAST_RETRY_BACKOFF_MS));
        }
        deletionOutboxRepository.save(event);
    }

    private Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}

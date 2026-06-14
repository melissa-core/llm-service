package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.CustomerMemoryAudit;
import uz.melisa.domain.CustomerMemoryChatState;
import uz.melisa.domain.CustomerMemoryDeletionOutbox;
import uz.melisa.domain.CustomerMemorySettings;
import uz.melisa.enums.MemoryAuditAction;
import uz.melisa.enums.MemoryDeletionOutboxStatus;
import uz.melisa.projection.ChatMessageSeqWatermark;
import uz.melisa.repository.CustomerMemoryAuditRepository;
import uz.melisa.repository.CustomerMemoryCandidateEvidenceRepository;
import uz.melisa.repository.CustomerMemoryCandidateRepository;
import uz.melisa.repository.CustomerMemoryChatStateRepository;
import uz.melisa.repository.CustomerMemoryDeletionOutboxRepository;
import uz.melisa.repository.CustomerMemoryEpisodeRepository;
import uz.melisa.repository.CustomerMemoryFactRepository;
import uz.melisa.repository.CustomerMemoryProcessingJobRepository;
import uz.melisa.repository.CustomerMemorySettingsRepository;
import uz.melisa.repository.MessageRepository;

import java.sql.Timestamp;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Right-to-be-forgotten and consent operations. Every flow takes the per-customer settings row lock
 * first, bumps generation (so any in-flight segment commit is aborted by the generation fence) and
 * fact_version, retires the old generation through the deletion outbox, and never writes a personal
 * value into an audit record.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryDeletionService {

    private static final String ACTION = "action";

    private final CustomerMemorySettingsRepository settingsRepository;
    private final CustomerMemoryFactRepository factRepository;
    private final CustomerMemoryEpisodeRepository episodeRepository;
    private final CustomerMemoryCandidateRepository candidateRepository;
    private final CustomerMemoryCandidateEvidenceRepository evidenceRepository;
    private final CustomerMemoryProcessingJobRepository jobRepository;
    private final CustomerMemoryChatStateRepository chatStateRepository;
    private final CustomerMemoryAuditRepository auditRepository;
    private final CustomerMemoryDeletionOutboxRepository deletionOutboxRepository;
    private final MessageRepository messageRepository;
    private final CustomerMemoryFactService factService;
    private final MemoryFactCacheService factCacheService;
    private final MemoryMetrics metrics;

    /** Task 12: delete one fact, conservatively clearing the customer's L2/candidate/pending state. */
    @Transactional
    public void deleteFact(Long customerId, Long factId) {
        CustomerMemorySettings settings = settingsRepository.findByCustomerIdForUpdate(customerId).orElse(null);
        if (settings == null) {
            return;
        }
        long retiredGeneration = settings.getGeneration();
        settings.setGeneration(retiredGeneration + 1);
        settings.setFactVersion(settings.getFactVersion() + 1);
        settingsRepository.save(settings);

        deleteSelectedFact(customerId, factId);
        clearTransientMemory(customerId);
        advanceCursorsToWatermarks(customerId);
        enqueueGenerationPurge(customerId, retiredGeneration);
        rebuildSurvivingCache(customerId);
        writeTombstone(customerId, MemoryAuditAction.HARD_DELETED);
        metrics.factsDeleted(1);
        log.info("deleted memory fact customerId={} factId={} retiredGeneration={}", customerId, factId, retiredGeneration);
    }

    /** Task 13: full right-to-be-forgotten. Disables memory and hard-deletes everything. */
    @Transactional
    public void deleteAllMemory(Long customerId) {
        CustomerMemorySettings settings = settingsRepository.findByCustomerIdForUpdate(customerId).orElse(null);
        if (settings == null) {
            return;
        }
        long retiredGeneration = settings.getGeneration();
        settings.setMemoryEnabled(false);
        settings.setGeneration(retiredGeneration + 1);
        settings.setFactVersion(settings.getFactVersion() + 1);
        settings.setDeletedAt(now());
        settingsRepository.save(settings);

        auditRepository.detachAllByCustomerId(customerId);
        int deletedFacts = factRepository.deleteByCustomerId(customerId);
        clearTransientMemory(customerId);

        enqueueGenerationPurge(customerId, retiredGeneration);   // Redis purge handled by the outbox worker
        factCacheService.evictGeneration(customerId, retiredGeneration);
        writeTombstone(customerId, MemoryAuditAction.FULL_MEMORY_DELETED);
        metrics.factsDeleted(deletedFacts);
        log.info("fully deleted memory customerId={} retiredGeneration={} facts={}", customerId, retiredGeneration, deletedFacts);
    }

    /** Task 13: explicit opt-in / re-consent. Cursors start at the current watermark, never history. */
    @Transactional
    public void optIn(Long customerId) {
        CustomerMemorySettings settings = settingsRepository.findByCustomerIdForUpdate(customerId)
                .orElseGet(() -> CustomerMemorySettings.builder().customerId(customerId).build());
        settings.setMemoryEnabled(true);
        settings.setConsentAt(now());
        settings.setOptedOutAt(null);
        settingsRepository.save(settings);
        settingsRepository.flush();

        initialiseCursorsAtWatermarks(customerId);
        writeTombstone(customerId, MemoryAuditAction.MEMORY_ENABLED);
        log.info("memory enabled (opt-in) customerId={}", customerId);
    }

    // ----- internals -----

    private void deleteSelectedFact(Long customerId, Long factId) {
        factRepository.findById(factId)
                .filter(fact -> customerId.equals(fact.getCustomerId()))
                .ifPresent(fact -> {
                    auditRepository.detachFromFact(factId);
                    factRepository.delete(fact);
                });
    }

    /** Clear all L2 episodes, candidates, evidence and pending processing for the customer. */
    private void clearTransientMemory(Long customerId) {
        evidenceRepository.deleteByCustomerId(customerId);
        candidateRepository.deleteByCustomerId(customerId);
        episodeRepository.deleteByCustomerId(customerId);
        jobRepository.abortOpenByCustomerId(customerId);
        chatStateRepository.clearActiveJobsByCustomerId(customerId);
    }

    private void advanceCursorsToWatermarks(Long customerId) {
        Map<Long, Long> watermarks = watermarks(customerId);
        for (CustomerMemoryChatState state : chatStateRepository.findAllByCustomerId(customerId)) {
            Long watermark = watermarks.get(state.getChatId());
            if (watermark != null) {
                state.setLastCompletedMessageSeq(watermark);
            }
            state.setActiveJobId(null);
            chatStateRepository.save(state);
        }
    }

    private void initialiseCursorsAtWatermarks(Long customerId) {
        watermarks(customerId).forEach((chatId, watermark) -> {
            CustomerMemoryChatState state = chatStateRepository.findById(chatId)
                    .orElseGet(() -> CustomerMemoryChatState.builder()
                            .chatId(chatId)
                            .customerId(customerId)
                            .build());
            state.setLastCompletedMessageSeq(watermark);   // never process messages created before consent
            state.setActiveJobId(null);
            chatStateRepository.save(state);
        });
    }

    private Map<Long, Long> watermarks(Long customerId) {
        return messageRepository.findChatWatermarksByUserId(customerId).stream()
                .filter(row -> row.getChatId() != null && row.getWatermark() != null)
                .collect(Collectors.toMap(ChatMessageSeqWatermark::getChatId, ChatMessageSeqWatermark::getWatermark));
    }

    private void enqueueGenerationPurge(Long customerId, long retiredGeneration) {
        deletionOutboxRepository.save(CustomerMemoryDeletionOutbox.builder()
                .customerId(customerId)
                .generation(retiredGeneration)
                .status(MemoryDeletionOutboxStatus.PENDING)
                .build());
    }

    private void rebuildSurvivingCache(Long customerId) {
        // Surviving facts under the new generation/factVersion are rebuilt lazily on next read; warm it now.
        factService.getMemoryContext(customerId);
    }

    private void writeTombstone(Long customerId, MemoryAuditAction action) {
        auditRepository.save(CustomerMemoryAudit.builder()
                .customerId(customerId)
                .action(action)
                .metadata(Map.of(ACTION, action.name()))
                .build());
    }

    private Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}

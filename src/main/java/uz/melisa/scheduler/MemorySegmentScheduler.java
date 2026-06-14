package uz.melisa.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.melisa.repository.CustomerMemoryChatStateRepository;
import uz.melisa.service.impl.MemoryPromotionService;
import uz.melisa.service.impl.MemorySegmentJobService;
import uz.melisa.service.impl.MemorySegmentProcessor;

import java.util.List;
import java.util.UUID;

/**
 * Drives segment processing. Every instance polls; the lease, generation fence and per-chat lock in
 * {@link MemorySegmentProcessor}/{@link MemorySegmentJobService} keep concurrent workers safe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemorySegmentScheduler {

    private final MemorySegmentProcessor memorySegmentProcessor;
    private final MemorySegmentJobService memorySegmentJobService;
    private final MemoryPromotionService memoryPromotionService;
    private final CustomerMemoryChatStateRepository chatStateRepository;

    private final String workerId = "worker-" + UUID.randomUUID();

    @Scheduled(fixedDelayString = "${melisa.memory.recover-interval-ms:60000}")
    public void recoverExpiredLeases() {
        try {
            int recovered = memorySegmentJobService.recoverExpiredLeases();
            if (recovered > 0) {
                log.info("recovered {} expired memory job lease(s)", recovered);
            }
        } catch (Exception e) {
            log.warn("memory lease recovery failed", e);
        }
    }

    @Scheduled(fixedDelayString = "${melisa.memory.process-interval-ms:30000}")
    public void processPendingSegments() {
        List<Long> chatIds;
        try {
            chatIds = chatStateRepository.findChatIdsWithPendingMessages();
        } catch (Exception e) {
            log.warn("failed to list chats with pending memory segments", e);
            return;
        }
        for (Long chatId : chatIds) {
            try {
                memorySegmentProcessor.processChat(chatId, workerId);
            } catch (Exception e) {
                log.warn("memory segment processing failed chatId={}", chatId, e);
            }
        }
    }

    @Scheduled(fixedDelayString = "${melisa.memory.candidate-expiry-interval-ms:3600000}")
    public void expireStaleCandidates() {
        try {
            int expired = memoryPromotionService.expireStaleCandidates();
            if (expired > 0) {
                log.info("expired {} stale memory candidate(s)", expired);
            }
        } catch (Exception e) {
            log.warn("memory candidate expiry sweep failed", e);
        }
    }
}

package uz.melisa.service.impl;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.domain.ChatMemory;
import uz.melisa.domain.Message;
import uz.melisa.dto.memory.ClaimedSegment;
import uz.melisa.dto.memory.ExtractedMemory;
import uz.melisa.enums.MemoryEpisodeSentiment;
import uz.melisa.enums.MessageAuthorityType;
import uz.melisa.repository.ChatMemoryRepository;
import uz.melisa.repository.MessageRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Orchestrates one segment end to end: claim (transaction 1) -> Haiku extraction (NO transaction,
 * lease kept alive by a heartbeat) -> commit or fail (transaction 2). Lives in a separate bean from
 * {@link MemorySegmentJobService} so the transactional boundaries are honored through the Spring
 * proxy (no self-invocation).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemorySegmentProcessor {

    private static final long HEARTBEAT_INTERVAL_MS = 90_000L;
    private static final ExtractedMemory EMPTY_MEMORY =
            new ExtractedMemory("", List.of(), MemoryEpisodeSentiment.UNKNOWN, List.of());

    private final MemorySegmentJobService jobService;
    private final MemoryExtractionService memoryExtractionService;
    private final MessageRepository messageRepository;
    private final ChatMemoryRepository chatMemoryRepository;

    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "memory-lease-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdownNow();
    }

    public void processChat(Long chatId, String workerId) {
        Optional<ClaimedSegment> claimed = jobService.claimNextSegment(chatId, workerId);
        if (claimed.isEmpty()) {
            return;
        }
        ClaimedSegment segment = claimed.get();

        ExtractedMemory extraction;
        try {
            extraction = extractWithHeartbeat(segment);   // Haiku call, outside any transaction
        } catch (Exception e) {
            log.warn("memory segment extraction failed jobId={} chatId={}", segment.jobId(), chatId, e);
            jobService.failJob(segment, e.getMessage());
            return;
        }
        jobService.commitSegment(segment, extraction);
    }

    private ExtractedMemory extractWithHeartbeat(ClaimedSegment segment) {
        ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleAtFixedRate(
                () -> renewLease(segment), HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        try {
            return extractSegment(segment);
        } finally {
            heartbeat.cancel(false);
        }
    }

    private void renewLease(ClaimedSegment segment) {
        try {
            int updatedRows = jobService.renewLease(segment.jobId(), segment.leaseToken());
            if (updatedRows != 1) {
                log.warn("lease lost during extraction jobId={} chatId={}", segment.jobId(), segment.chatId());
            }
        } catch (Exception e) {
            log.warn("lease heartbeat failed jobId={}", segment.jobId(), e);
        }
    }

    private ExtractedMemory extractSegment(ClaimedSegment segment) {
        List<Message> messages = messageRepository.findByChatIdAndMessageSeqRange(
                segment.chatId(), segment.fromSeq(), segment.untilSeq());
        String userText = joinByAuthority(messages, MessageAuthorityType.USER);
        String assistantText = joinByAuthority(messages, MessageAuthorityType.MODEL);
        String previousSummary = chatMemoryRepository.findByChatId(segment.chatId())
                .map(ChatMemory::getSummary)
                .orElse("");

        return memoryExtractionService.extract(previousSummary, userText, assistantText)
                .orElse(EMPTY_MEMORY);
    }

    private String joinByAuthority(List<Message> messages, MessageAuthorityType authority) {
        return messages.stream()
                .filter(message -> message.getMessageAuthorityType() == authority)
                .map(Message::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));
    }
}

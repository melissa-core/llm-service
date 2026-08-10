package uz.melisa.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uz.melisa.domain.CustomerMemoryEpisode;
import uz.melisa.domain.Message;
import uz.melisa.dto.memory.ClaimedSegment;
import uz.melisa.dto.memory.ExtractedMemory;
import uz.melisa.enums.MemoryEpisodeSentiment;
import uz.melisa.enums.MessageAuthorityType;
import uz.melisa.repository.CustomerMemoryEpisodeRepository;
import uz.melisa.repository.MessageRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemorySegmentProcessorTest {

    private final MemorySegmentJobService jobService = mock(MemorySegmentJobService.class);
    private final MemoryExtractionService extractionService = mock(MemoryExtractionService.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final CustomerMemoryEpisodeRepository episodeRepository = mock(CustomerMemoryEpisodeRepository.class);

    private final MemorySegmentProcessor processor = new MemorySegmentProcessor(
            jobService,
            extractionService,
            messageRepository,
            episodeRepository
    );

    @AfterEach
    void shutdownProcessor() {
        processor.shutdown();
    }

    @Test
    void usesImmediatelyPreviousDurableEpisodeInsteadOfInlineChatSummary() {
        ClaimedSegment segment = segment(2, 21, 40);
        ExtractedMemory extracted = memory("summary through message 40");

        when(jobService.claimNextSegment(10L, "worker-1")).thenReturn(Optional.of(segment));
        when(messageRepository.findByChatIdAndMessageSeqRange(10L, 21, 40))
                .thenReturn(List.of(
                        message(21, MessageAuthorityType.USER, "My name is Jahongir"),
                        message(22, MessageAuthorityType.MODEL, "Nice to meet you")
                ));
        when(episodeRepository.findByChatIdAndSegmentNumber(10L, 1))
                .thenReturn(Optional.of(CustomerMemoryEpisode.builder()
                        .chatId(10L)
                        .segmentNumber(1)
                        .summarizedUntilMessageSeq(20L)
                        .summary("summary through message 20")
                        .build()));
        when(extractionService.extract(
                "summary through message 20",
                "My name is Jahongir",
                "Nice to meet you"
        )).thenReturn(Optional.of(extracted));

        processor.processChat(10L, "worker-1");

        verify(extractionService).extract(
                "summary through message 20",
                "My name is Jahongir",
                "Nice to meet you"
        );
        verify(jobService).commitSegment(segment, extracted);
    }

    @Test
    void firstSegmentStartsWithoutPreviousSummary() {
        ClaimedSegment segment = segment(1, 1, 20);
        ExtractedMemory extracted = memory("first summary");

        when(jobService.claimNextSegment(10L, "worker-1")).thenReturn(Optional.of(segment));
        when(messageRepository.findByChatIdAndMessageSeqRange(10L, 1, 20))
                .thenReturn(List.of(message(1, MessageAuthorityType.USER, "Hello")));
        when(extractionService.extract("", "Hello", ""))
                .thenReturn(Optional.of(extracted));

        processor.processChat(10L, "worker-1");

        verify(episodeRepository, never()).findByChatIdAndSegmentNumber(10L, 0);
        verify(extractionService).extract("", "Hello", "");
        verify(jobService).commitSegment(segment, extracted);
    }

    @Test
    void missingOrEmptyPreviousEpisodeDoesNotReadFutureContext() {
        ClaimedSegment segment = segment(3, 41, 60);
        ExtractedMemory extracted = memory("summary through message 60");

        when(jobService.claimNextSegment(10L, "worker-1")).thenReturn(Optional.of(segment));
        when(messageRepository.findByChatIdAndMessageSeqRange(10L, 41, 60))
                .thenReturn(List.of(message(41, MessageAuthorityType.USER, "New question")));
        when(episodeRepository.findByChatIdAndSegmentNumber(10L, 2)).thenReturn(Optional.empty());
        when(extractionService.extract("", "New question", ""))
                .thenReturn(Optional.of(extracted));

        processor.processChat(10L, "worker-1");

        verify(extractionService).extract("", "New question", "");
        verify(jobService).commitSegment(segment, extracted);
    }

    @Test
    void rejectsNonContiguousPreviousEpisodeInsteadOfMixingWrongHistory() {
        ClaimedSegment segment = segment(2, 21, 40);
        ExtractedMemory extracted = memory("fresh summary");

        when(jobService.claimNextSegment(10L, "worker-1")).thenReturn(Optional.of(segment));
        when(messageRepository.findByChatIdAndMessageSeqRange(10L, 21, 40))
                .thenReturn(List.of(message(21, MessageAuthorityType.USER, "Current segment")));
        when(episodeRepository.findByChatIdAndSegmentNumber(10L, 1))
                .thenReturn(Optional.of(CustomerMemoryEpisode.builder()
                        .chatId(10L)
                        .segmentNumber(1)
                        .summarizedUntilMessageSeq(999L)
                        .summary("wrong historical range")
                        .build()));
        when(extractionService.extract("", "Current segment", ""))
                .thenReturn(Optional.of(extracted));

        processor.processChat(10L, "worker-1");

        verify(extractionService).extract("", "Current segment", "");
        verify(jobService).commitSegment(segment, extracted);
    }

    private ClaimedSegment segment(int segmentNumber, long fromSeq, long untilSeq) {
        return new ClaimedSegment(
                99L,
                10L,
                7L,
                fromSeq,
                untilSeq,
                segmentNumber,
                UUID.randomUUID(),
                3L,
                "worker-1"
        );
    }

    private ExtractedMemory memory(String summary) {
        return new ExtractedMemory(summary, List.of(), MemoryEpisodeSentiment.UNKNOWN, List.of());
    }

    private Message message(long seq, MessageAuthorityType authority, String text) {
        return Message.builder()
                .chatId(10L)
                .messageSeq(seq)
                .messageAuthorityType(authority)
                .text(text)
                .build();
    }
}

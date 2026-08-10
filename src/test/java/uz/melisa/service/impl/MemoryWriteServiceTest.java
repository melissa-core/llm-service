package uz.melisa.service.impl;

import org.junit.jupiter.api.Test;
import uz.melisa.domain.CustomerMemoryEpisode;
import uz.melisa.dto.memory.ClaimedSegment;
import uz.melisa.dto.memory.ExtractedMemory;
import uz.melisa.enums.MemoryEpisodeSentiment;
import uz.melisa.repository.CustomerMemoryEpisodeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MemoryWriteServiceTest {

    private final CustomerMemoryEpisodeRepository episodeRepository = mock(CustomerMemoryEpisodeRepository.class);
    private final MemoryWriteService service = new MemoryWriteService(episodeRepository);

    @Test
    void saveEpisodeReturnsExistingEpisodeForSameSegmentIdentity() {
        ClaimedSegment segment = segment(100L, 10L, 21L, 40L, 2);
        CustomerMemoryEpisode existing = CustomerMemoryEpisode.builder()
                .id(55L)
                .customerId(100L)
                .chatId(10L)
                .segmentNumber(2)
                .summarizedFromMessageSeq(21L)
                .summarizedUntilMessageSeq(40L)
                .summary("existing")
                .build();
        when(episodeRepository.findByChatIdAndSegmentNumber(10L, 2)).thenReturn(Optional.of(existing));

        CustomerMemoryEpisode result = service.saveEpisode(segment, memory());

        assertSame(existing, result);
        verify(episodeRepository, never()).save(any());
    }

    @Test
    void saveEpisodeRejectsExistingEpisodeWithConflictingRange() {
        ClaimedSegment segment = segment(100L, 10L, 21L, 40L, 2);
        CustomerMemoryEpisode existing = CustomerMemoryEpisode.builder()
                .id(55L)
                .customerId(100L)
                .chatId(10L)
                .segmentNumber(2)
                .summarizedFromMessageSeq(1L)
                .summarizedUntilMessageSeq(20L)
                .summary("wrong segment identity")
                .build();
        when(episodeRepository.findByChatIdAndSegmentNumber(10L, 2)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.saveEpisode(segment, memory()));
        verify(episodeRepository, never()).save(any());
    }

    @Test
    void saveEpisodeCreatesNewRowWhenSegmentDoesNotExist() {
        ClaimedSegment segment = segment(100L, 10L, 1L, 20L, 1);
        when(episodeRepository.findByChatIdAndSegmentNumber(10L, 1)).thenReturn(Optional.empty());
        when(episodeRepository.save(any(CustomerMemoryEpisode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerMemoryEpisode result = service.saveEpisode(segment, memory());

        verify(episodeRepository).save(any(CustomerMemoryEpisode.class));
        org.junit.jupiter.api.Assertions.assertEquals(100L, result.getCustomerId());
        org.junit.jupiter.api.Assertions.assertEquals(10L, result.getChatId());
        org.junit.jupiter.api.Assertions.assertEquals(1, result.getSegmentNumber());
        org.junit.jupiter.api.Assertions.assertEquals(1L, result.getSummarizedFromMessageSeq());
        org.junit.jupiter.api.Assertions.assertEquals(20L, result.getSummarizedUntilMessageSeq());
    }

    private ClaimedSegment segment(Long customerId, Long chatId, long from, long until, int number) {
        return new ClaimedSegment(1L, chatId, customerId, from, until, number,
                UUID.randomUUID(), 1L, "test-worker");
    }

    private ExtractedMemory memory() {
        return new ExtractedMemory("summary", List.of("topic"), MemoryEpisodeSentiment.NEUTRAL, List.of());
    }
}

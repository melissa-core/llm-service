package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.melisa.domain.CustomerMemoryEpisode;
import uz.melisa.dto.memory.ClaimedSegment;
import uz.melisa.dto.memory.ExtractedMemory;
import uz.melisa.enums.MemoryEpisodeSentiment;
import uz.melisa.repository.CustomerMemoryEpisodeRepository;

import java.sql.Timestamp;
import java.util.List;

/**
 * Persists the L2 episode for a processed segment. Fact routing/promotion lives in
 * {@link MemoryPromotionService}. Runs inside the caller's commit transaction.
 */
@Service
@RequiredArgsConstructor
public class MemoryWriteService {

    private static final long EPISODE_TTL_MS = 30L * 24 * 60 * 60 * 1000;
    private static final String SUMMARIZATION_VERSION = "haiku-4-5-v1";
    private static final String EMPTY_SUMMARY = "(no summary)";

    private final CustomerMemoryEpisodeRepository episodeRepository;

    public CustomerMemoryEpisode saveEpisode(ClaimedSegment segment, ExtractedMemory memory) {
        var existing = episodeRepository.findByChatIdAndSegmentNumber(segment.chatId(), segment.segmentNumber());
        if (existing.isPresent()) {
            CustomerMemoryEpisode episode = existing.get();
            if (!segment.customerId().equals(episode.getCustomerId())
                    || episode.getSummarizedFromMessageSeq() == null
                    || episode.getSummarizedUntilMessageSeq() == null
                    || episode.getSummarizedFromMessageSeq() != segment.fromSeq()
                    || episode.getSummarizedUntilMessageSeq() != segment.untilSeq()) {
                throw new IllegalStateException(
                        "durable memory episode identity conflict for chat=" + segment.chatId()
                                + ", segment=" + segment.segmentNumber());
            }
            return episode;
        }

        String summary = memory.summary() == null || memory.summary().isBlank() ? EMPTY_SUMMARY : memory.summary();
        return episodeRepository.save(CustomerMemoryEpisode.builder()
                .customerId(segment.customerId())
                .chatId(segment.chatId())
                .segmentNumber(segment.segmentNumber())
                .summarizedFromMessageSeq(segment.fromSeq())
                .summarizedUntilMessageSeq(segment.untilSeq())
                .summary(summary)
                .topics(memory.topics() == null ? List.of() : memory.topics())
                .sentiment(memory.sentiment() == null ? MemoryEpisodeSentiment.UNKNOWN : memory.sentiment())
                .summarizationVersion(SUMMARIZATION_VERSION)
                .expiresAt(future(EPISODE_TTL_MS))
                .build());
    }

    private Timestamp future(long offsetMs) {
        return new Timestamp(System.currentTimeMillis() + offsetMs);
    }
}

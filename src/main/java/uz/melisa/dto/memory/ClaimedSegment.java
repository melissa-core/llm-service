package uz.melisa.dto.memory;

import java.util.UUID;

/**
 * The result of atomically claiming a message range for summarization. Carries everything the
 * generation fence needs at commit time: the lease token and the generation captured at claim.
 */
public record ClaimedSegment(
        Long jobId,
        Long chatId,
        Long customerId,
        long fromSeq,
        long untilSeq,
        int segmentNumber,
        UUID leaseToken,
        long generation,
        String workerId
) {
}

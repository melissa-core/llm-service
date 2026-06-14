package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.CustomerMemoryCandidate;
import uz.melisa.domain.CustomerMemoryCandidateEvidence;
import uz.melisa.domain.CustomerMemoryEpisode;
import uz.melisa.dto.memory.ClaimedSegment;
import uz.melisa.dto.memory.ExtractedFact;
import uz.melisa.enums.MemoryCandidateStatus;
import uz.melisa.enums.MemoryFactSourceType;
import uz.melisa.repository.CustomerMemoryCandidateEvidenceRepository;
import uz.melisa.repository.CustomerMemoryCandidateRepository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Routes each validated fact to L1 (via {@link CustomerMemoryFactService}) or to the candidate
 * pipeline, inside the segment commit transaction (which already holds a per-customer settings row
 * lock — that gives the cross-chat serialization which makes find-before-insert idempotent until
 * unique constraints exist).
 *
 * <p>Explicit (remember request / customer statement) and clearly-stated allergy/dietary -> immediate
 * L1. Ordinary inferred promotable preferences -> PENDING candidate + one evidence per chat, promoted
 * once non-expired evidence spans enough distinct chats. All L1 writes, fact_version bumps, audits and
 * cache invalidation are owned by {@link CustomerMemoryFactService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryPromotionService {

    private static final int PROMOTION_DISTINCT_CHAT_THRESHOLD = 2;
    private static final long CANDIDATE_TTL_MS = 90L * 24 * 60 * 60 * 1000;
    private static final long EVIDENCE_TTL_MS = 90L * 24 * 60 * 60 * 1000;

    private final CustomerMemoryFactService factService;
    private final CustomerMemoryCandidateRepository candidateRepository;
    private final CustomerMemoryCandidateEvidenceRepository evidenceRepository;

    public void applyFacts(ClaimedSegment segment, CustomerMemoryEpisode episode, List<ExtractedFact> facts) {
        for (ExtractedFact fact : facts) {
            applyFact(segment, episode, fact);
        }
    }

    /** Housekeeping for "candidate expires": mark PENDING candidates whose expiry has passed as EXPIRED. */
    @Transactional
    public int expireStaleCandidates() {
        return candidateRepository.expirePendingBefore(now());
    }

    private void applyFact(ClaimedSegment segment, CustomerMemoryEpisode episode, ExtractedFact fact) {
        if (isExplicit(fact.sourceType())) {
            factService.createFact(segment.customerId(), fact, fact.sourceType());
            return;
        }
        accumulateCandidate(segment, episode, fact);
    }

    private void accumulateCandidate(ClaimedSegment segment, CustomerMemoryEpisode episode, ExtractedFact fact) {
        if (!fact.promotableByRepeatedInference()) {
            return;   // ordinary non-promotable fact is never a candidate (registry-authoritative)
        }
        if (factService.confirmActiveFact(segment.customerId(), fact.factType(), fact.factKey(), fact.normalizedValue())) {
            return;   // already an active L1 fact: confirmed, don't churn duplicate candidates
        }

        Timestamp now = now();
        CustomerMemoryCandidate candidate = findOrCreateCandidate(segment.customerId(), fact, now);
        recordEvidenceOncePerChat(candidate, segment.chatId(), episode, fact);

        long distinctChats = evidenceRepository.countDistinctChatsByCandidateIdAndNotExpired(candidate.getId(), now);
        if (distinctChats < PROMOTION_DISTINCT_CHAT_THRESHOLD) {
            return;
        }
        factService.promoteFact(segment.customerId(), fact, candidate);
        candidate.setStatus(MemoryCandidateStatus.PROMOTED);
        candidateRepository.save(candidate);
    }

    private CustomerMemoryCandidate findOrCreateCandidate(Long customerId, ExtractedFact fact, Timestamp now) {
        Optional<CustomerMemoryCandidate> existing = candidateRepository.findMatchingCandidate(
                customerId, fact.factType(), fact.factKey(), fact.normalizedValue(), MemoryCandidateStatus.PENDING);
        if (existing.isPresent()) {
            CustomerMemoryCandidate candidate = existing.get();
            candidate.setStatus(MemoryCandidateStatus.PENDING);   // re-assert against a concurrent expiry sweep
            candidate.setLastSeenAt(now);
            candidate.setExpiresAt(future(CANDIDATE_TTL_MS));
            candidate.setMaxConfidence(maxConfidence(candidate.getMaxConfidence(), fact.confidence()));
            return candidateRepository.save(candidate);
        }
        return candidateRepository.save(CustomerMemoryCandidate.builder()
                .customerId(customerId)
                .factType(fact.factType())
                .factKey(fact.factKey())
                .valueJson(fact.valueJson())
                .normalizedValue(fact.normalizedValue())
                .maxConfidence(fact.confidence())
                .status(MemoryCandidateStatus.PENDING)
                .firstSeenAt(now)
                .lastSeenAt(now)
                .expiresAt(future(CANDIDATE_TTL_MS))
                .build());
    }

    private void recordEvidenceOncePerChat(CustomerMemoryCandidate candidate, Long chatId,
                                           CustomerMemoryEpisode episode, ExtractedFact fact) {
        Optional<CustomerMemoryCandidateEvidence> existing =
                evidenceRepository.findFirstByCandidateIdAndChatIdOrderByIdAsc(candidate.getId(), chatId);
        if (existing.isPresent()) {
            CustomerMemoryCandidateEvidence evidence = existing.get();
            evidence.setExpiresAt(future(EVIDENCE_TTL_MS));   // refresh so a repeated mention stays "recent"
            evidence.setConfidence(maxConfidence(evidence.getConfidence(), fact.confidence()));
            evidenceRepository.save(evidence);
            return;
        }
        evidenceRepository.save(CustomerMemoryCandidateEvidence.builder()
                .candidateId(candidate.getId())
                .chatId(chatId)
                .episodeId(episode == null ? null : episode.getId())
                .sourceType(fact.sourceType())
                .confidence(fact.confidence())
                .expiresAt(future(EVIDENCE_TTL_MS))
                .build());
    }

    private boolean isExplicit(MemoryFactSourceType sourceType) {
        return sourceType == MemoryFactSourceType.EXPLICIT_CUSTOMER_STATEMENT
                || sourceType == MemoryFactSourceType.EXPLICIT_REMEMBER_REQUEST;
    }

    private BigDecimal maxConfidence(BigDecimal current, BigDecimal candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate == null) {
            return current;
        }
        return current.compareTo(candidate) >= 0 ? current : candidate;
    }

    private Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    private Timestamp future(long offsetMs) {
        return new Timestamp(System.currentTimeMillis() + offsetMs);
    }
}

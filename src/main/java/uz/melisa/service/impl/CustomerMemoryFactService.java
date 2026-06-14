package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.CustomerMemoryAudit;
import uz.melisa.domain.CustomerMemoryCandidate;
import uz.melisa.domain.CustomerMemoryFact;
import uz.melisa.domain.CustomerMemorySettings;
import uz.melisa.dto.memory.ExtractedFact;
import uz.melisa.dto.memory.MemoryContext;
import uz.melisa.dto.memory.MemoryFactView;
import uz.melisa.enums.MemoryAuditAction;
import uz.melisa.enums.MemoryFactCardinality;
import uz.melisa.enums.MemoryFactSourceType;
import uz.melisa.enums.MemoryFactStatus;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.enums.MemoryRiskClass;
import uz.melisa.repository.CustomerMemoryAuditRepository;
import uz.melisa.repository.CustomerMemoryFactRepository;
import uz.melisa.repository.CustomerMemorySettingsRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Canonical L1 (permanent) fact operations. Single source of truth for the active-fact invariants:
 * single-valued facts keep exactly one active value (enforced in service logic since unique indexes
 * are deferred), explicit corrections supersede the previous value, every active-L1 change bumps
 * customer_memory_settings.fact_version and invalidates the Redis prompt-fact cache, and every change
 * writes an audit record whose metadata never contains a personal value.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerMemoryFactService {

    private static final String ACTION = "action";

    private final CustomerMemoryFactRepository factRepository;
    private final CustomerMemoryAuditRepository auditRepository;
    private final CustomerMemorySettingsRepository settingsRepository;
    private final MemoryFactCacheService factCacheService;

    // ----- writes -----

    /** Create a new active L1 fact (immediate/explicit path). SINGLE-valued supersedes the prior value. */
    @Transactional
    public Optional<CustomerMemoryFact> createFact(Long customerId, ExtractedFact fact, MemoryFactSourceType sourceType) {
        return upsertActiveFact(customerId, fact, sourceType, MemoryAuditAction.CREATED, null);
    }

    /** Promote a candidate's preference to an active L1 fact. */
    @Transactional
    public Optional<CustomerMemoryFact> promoteFact(Long customerId, ExtractedFact fact, CustomerMemoryCandidate fromCandidate) {
        return upsertActiveFact(customerId, fact, MemoryFactSourceType.REPEATED_INFERENCE, MemoryAuditAction.PROMOTED, fromCandidate);
    }

    /** Re-confirm an existing active fact (a re-mention is not an active-L1 change). Returns whether it existed. */
    @Transactional
    public boolean confirmActiveFact(Long customerId, MemoryFactType factType, String factKey, String normalizedValue) {
        Optional<CustomerMemoryFact> existing = factRepository.findMatchingFact(
                customerId, factType, factKey, normalizedValue, MemoryFactStatus.ACTIVE);
        existing.ifPresent(fact -> {
            fact.setLastConfirmedAt(now());
            factRepository.save(fact);
        });
        return existing.isPresent();
    }

    /** Explicit correction / retirement: mark a specific active fact SUPERSEDED. */
    @Transactional
    public void supersedeFact(Long factId, MemoryFactSourceType sourceType) {
        factRepository.findById(factId)
                .filter(fact -> fact.getStatus() == MemoryFactStatus.ACTIVE)
                .ifPresent(fact -> {
                    fact.setStatus(MemoryFactStatus.SUPERSEDED);
                    factRepository.save(fact);
                    audit(fact.getCustomerId(), fact.getId(), MemoryAuditAction.SUPERSEDED, sourceType);
                    onActiveL1Changed(fact.getCustomerId());
                });
    }

    /** Reject a fact. Counts as an active-L1 change only if it was active. */
    @Transactional
    public void rejectFact(Long factId, MemoryFactSourceType sourceType) {
        factRepository.findById(factId).ifPresent(fact -> {
            boolean wasActive = fact.getStatus() == MemoryFactStatus.ACTIVE;
            fact.setStatus(MemoryFactStatus.REJECTED);
            factRepository.save(fact);
            audit(fact.getCustomerId(), fact.getId(), MemoryAuditAction.REJECTED, sourceType);
            if (wasActive) {
                onActiveL1Changed(fact.getCustomerId());
            }
        });
    }

    /** Hard-delete (forget) a fact. The deleted personal value is never written to audit metadata. */
    @Transactional
    public void forgetFact(Long factId) {
        factRepository.findById(factId).ifPresent(fact -> {
            boolean wasActive = fact.getStatus() == MemoryFactStatus.ACTIVE;
            Long customerId = fact.getCustomerId();
            auditRepository.detachFromFact(factId);   // drop audit FK references so the delete cannot be blocked
            factRepository.delete(fact);
            auditRepository.save(CustomerMemoryAudit.builder()
                    .customerId(customerId)
                    .action(MemoryAuditAction.HARD_DELETED)
                    .metadata(Map.of(ACTION, MemoryAuditAction.HARD_DELETED.name()))
                    .build());
            if (wasActive) {
                onActiveL1Changed(customerId);
            }
        });
    }

    // ----- reads -----

    public List<CustomerMemoryFact> getActiveFacts(Long customerId) {
        return factRepository.findAllByCustomerIdAndStatus(customerId, MemoryFactStatus.ACTIVE);
    }

    /** Safety facts (HIGH risk). Read straight from Postgres — safety filtering must NEVER rely on Redis. */
    public List<CustomerMemoryFact> getSafetyFacts(Long customerId) {
        return getActiveFacts(customerId).stream()
                .filter(fact -> fact.getRiskClass() == MemoryRiskClass.HIGH)
                .toList();
    }

    /**
     * Read-through Redis cache of the assembled L1 memory context (active facts for prompt injection),
     * keyed by customer + generation + factVersion. Postgres stays the source of truth and this is
     * NEVER used for safety filtering (see {@link #getSafetyFacts}). Memory disabled -> empty context.
     */
    public MemoryContext getMemoryContext(Long customerId) {
        CustomerMemorySettings settings = settingsRepository.findById(customerId).orElse(null);
        if (settings == null || !settings.isMemoryEnabled()) {
            return emptyContext(customerId, settings);
        }
        long generation = settings.getGeneration();
        long factVersion = settings.getFactVersion();
        return factCacheService.get(customerId, generation, factVersion)
                .orElseGet(() -> rebuildContext(customerId, generation, factVersion));
    }

    /** Prompt-ready text block, formatted from the (cached) memory context. */
    public String getPromptFacts(Long customerId) {
        return buildPromptFacts(getMemoryContext(customerId).facts());
    }

    private MemoryContext rebuildContext(Long customerId, long generation, long factVersion) {
        MemoryContext context = new MemoryContext(customerId, generation, factVersion, toViews(getActiveFacts(customerId)), List.of());
        factCacheService.put(context);
        return context;
    }

    private MemoryContext emptyContext(Long customerId, CustomerMemorySettings settings) {
        long generation = settings == null ? 0L : settings.getGeneration();
        long factVersion = settings == null ? 0L : settings.getFactVersion();
        return new MemoryContext(customerId, generation, factVersion, List.of(), List.of());
    }

    private List<MemoryFactView> toViews(List<CustomerMemoryFact> facts) {
        return facts.stream()
                .map(fact -> new MemoryFactView(fact.getFactType(), fact.getFactKey(),
                        fact.getNormalizedValue(), fact.getValueJson()))
                .toList();
    }

    public boolean hasActiveFact(Long customerId, MemoryFactType factType, String factKey, String normalizedValue) {
        return factRepository.findMatchingFact(customerId, factType, factKey, normalizedValue, MemoryFactStatus.ACTIVE)
                .isPresent();
    }

    // ----- internals -----

    private Optional<CustomerMemoryFact> upsertActiveFact(Long customerId, ExtractedFact fact,
                                                          MemoryFactSourceType sourceType, MemoryAuditAction action,
                                                          CustomerMemoryCandidate fromCandidate) {
        Optional<CustomerMemoryFact> existing = factRepository.findMatchingFact(
                customerId, fact.factType(), fact.factKey(), fact.normalizedValue(), MemoryFactStatus.ACTIVE);
        if (existing.isPresent()) {
            CustomerMemoryFact current = existing.get();
            current.setLastConfirmedAt(now());
            factRepository.save(current);
            if (fromCandidate != null) {
                fromCandidate.setPromotedFactId(current.getId());
            }
            return Optional.of(current);   // already active: idempotent, no active-L1 change
        }
        if (fact.cardinality() == MemoryFactCardinality.SINGLE) {
            supersedeActiveSingleValued(customerId, fact, sourceType);
        }
        CustomerMemoryFact saved = factRepository.save(buildFact(customerId, fact, sourceType));
        if (fromCandidate != null) {
            fromCandidate.setPromotedFactId(saved.getId());
        }
        audit(customerId, saved.getId(), action, sourceType);
        onActiveL1Changed(customerId);
        return Optional.of(saved);
    }

    private void supersedeActiveSingleValued(Long customerId, ExtractedFact fact, MemoryFactSourceType sourceType) {
        List<CustomerMemoryFact> active = factRepository.findAllByCustomerIdAndStatus(customerId, MemoryFactStatus.ACTIVE);
        for (CustomerMemoryFact existing : active) {
            if (existing.getFactType() == fact.factType() && fact.factKey().equals(existing.getFactKey())) {
                existing.setStatus(MemoryFactStatus.SUPERSEDED);
                factRepository.save(existing);
                audit(customerId, existing.getId(), MemoryAuditAction.SUPERSEDED, sourceType);
            }
        }
    }

    private CustomerMemoryFact buildFact(Long customerId, ExtractedFact fact, MemoryFactSourceType sourceType) {
        Timestamp now = now();
        return CustomerMemoryFact.builder()
                .customerId(customerId)
                .factType(fact.factType())
                .factKey(fact.factKey())
                .cardinality(fact.cardinality())
                .riskClass(fact.riskClass())
                .valueJson(fact.valueJson())
                .normalizedValue(fact.normalizedValue())
                .sourceType(sourceType)
                .status(MemoryFactStatus.ACTIVE)
                .confidence(fact.confidence())
                .schemaVersion(1)
                .firstSeenAt(now)
                .lastConfirmedAt(now)
                .build();
    }

    /** Every active-L1 change: bump fact_version and invalidate the (old) cached prompt-fact block. */
    private void onActiveL1Changed(Long customerId) {
        CustomerMemorySettings settings = settingsRepository.findByCustomerIdForUpdate(customerId).orElse(null);
        if (settings == null) {
            return;
        }
        long previousVersion = settings.getFactVersion();
        settings.setFactVersion(previousVersion + 1);
        settingsRepository.save(settings);
        factCacheService.evict(customerId, settings.getGeneration(), previousVersion);
    }

    private void audit(Long customerId, Long factId, MemoryAuditAction action, MemoryFactSourceType sourceType) {
        auditRepository.save(CustomerMemoryAudit.builder()
                .customerId(customerId)
                .factId(factId)
                .action(action)
                .sourceType(sourceType)
                .metadata(Map.of(ACTION, action.name()))
                .build());
    }

    private String buildPromptFacts(List<MemoryFactView> facts) {
        if (facts.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("Known customer facts:\n");
        appendGroup(builder, facts, MemoryFactType.ALLERGY, "Allergies");
        appendGroup(builder, facts, MemoryFactType.DIETARY, "Dietary restrictions");
        appendGroup(builder, facts, MemoryFactType.PREFERENCE, "Preferences");
        appendGroup(builder, facts, MemoryFactType.EXCLUSION, "Exclusions");
        appendGroup(builder, facts, MemoryFactType.INSTRUCTION, "Instructions");
        return builder.toString().trim();
    }

    private void appendGroup(StringBuilder builder, List<MemoryFactView> facts, MemoryFactType factType, String label) {
        String values = facts.stream()
                .filter(view -> view.type() == factType)
                .map(MemoryFactView::normalizedValue)
                .collect(Collectors.joining(", "));
        if (!values.isBlank()) {
            builder.append("- ").append(label).append(": ").append(values).append('\n');
        }
    }

    private Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}

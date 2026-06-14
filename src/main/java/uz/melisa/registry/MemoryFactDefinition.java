package uz.melisa.registry;

import uz.melisa.enums.MemoryFactCardinality;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.enums.MemoryNormalizationStrategy;
import uz.melisa.enums.MemoryRiskClass;

import java.util.Set;

/**
 * Server-owned definition of one fact kind.
 *
 * <p>{@code cardinality} and {@code riskClass} are authoritative here and must never be taken from
 * AI output. {@code allowedCodes} is the closed set of structured codes for a {@code CODE_SET}
 * strategy (empty when the value is free text or an organization reference).
 */
public record MemoryFactDefinition(
        MemoryFactType factType,
        String factKey,
        MemoryFactCardinality cardinality,
        MemoryRiskClass riskClass,
        MemoryNormalizationStrategy normalizationStrategy,
        Set<String> allowedCodes,
        boolean promotableByRepeatedInference,
        boolean safetyCritical
) {

    public MemoryFactDefinition {
        allowedCodes = allowedCodes == null ? Set.of() : Set.copyOf(allowedCodes);
    }

    public boolean hasClosedCodeSet() {
        return !allowedCodes.isEmpty();
    }

    public boolean isCodeAllowed(String code) {
        return code != null && allowedCodes.contains(code);
    }

    public boolean requiresOrganizationResolution() {
        return normalizationStrategy == MemoryNormalizationStrategy.ORGANIZATION_REF;
    }
}

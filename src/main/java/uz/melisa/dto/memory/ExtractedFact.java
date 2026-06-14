package uz.melisa.dto.memory;

import uz.melisa.enums.MemoryFactCardinality;
import uz.melisa.enums.MemoryFactSourceType;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.enums.MemoryRiskClass;

import java.math.BigDecimal;
import java.util.Map;

/**
 * A fact that has passed validation: its (type, key) is registry-known, its triggering quote was
 * found in a customer-authored message, its value is normalized and (for code sets) verified, and
 * its source_type was assigned by application logic. cardinality, riskClass and safetyCritical come
 * from the registry, never from AI output.
 */
public record ExtractedFact(
        MemoryFactType factType,
        String factKey,
        String normalizedValue,
        Map<String, Object> valueJson,
        MemoryFactCardinality cardinality,
        MemoryRiskClass riskClass,
        MemoryFactSourceType sourceType,
        BigDecimal confidence,
        boolean stable,
        boolean safetyCritical,
        boolean promotableByRepeatedInference,
        String triggeringQuote
) {
}

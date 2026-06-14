package uz.melisa.dto.memory;

import uz.melisa.enums.MemoryFactType;

import java.util.Map;

/**
 * One active L1 fact as cached for prompt injection. Intentionally a minimal projection — no
 * status/audit/internal fields — since the cache only ever holds active, prompt-ready facts.
 */
public record MemoryFactView(
        MemoryFactType type,
        String key,
        String normalizedValue,
        Map<String, Object> valueJson
) {
}

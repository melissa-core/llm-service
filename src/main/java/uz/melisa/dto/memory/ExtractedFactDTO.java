package uz.melisa.dto.memory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * One fact exactly as returned by the Haiku extractor. Every field is advisory and must be
 * validated before use; {@code sourceHint} in particular is never trusted as the real source_type.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractedFactDTO(
        String type,
        String key,
        Map<String, Object> valueJson,
        String triggeringQuote,
        Double confidence,
        Boolean stable,
        String sourceHint
) {
}

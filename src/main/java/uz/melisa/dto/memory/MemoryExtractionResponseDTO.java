package uz.melisa.dto.memory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Raw structured response from the Haiku memory extractor, before any validation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MemoryExtractionResponseDTO(
        String summary,
        List<String> topics,
        String sentiment,
        List<ExtractedFactDTO> facts
) {
}

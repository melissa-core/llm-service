package uz.melisa.dto.memory;

import uz.melisa.enums.MemoryEpisodeSentiment;

import java.util.List;

/**
 * Validated output of one memory-extraction pass: the running summary plus the facts that survived
 * validation, quote verification, normalization and the uncertain-allergy/dietary safety rules.
 */
public record ExtractedMemory(
        String summary,
        List<String> topics,
        MemoryEpisodeSentiment sentiment,
        List<ExtractedFact> facts
) {
}

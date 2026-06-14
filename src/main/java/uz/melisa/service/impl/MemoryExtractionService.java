package uz.melisa.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.dto.memory.ExtractedFact;
import uz.melisa.dto.memory.ExtractedFactDTO;
import uz.melisa.dto.memory.ExtractedMemory;
import uz.melisa.dto.memory.MemoryExtractionResponseDTO;
import uz.melisa.enums.MemoryEpisodeSentiment;
import uz.melisa.enums.MemoryFactSourceType;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.exp.BadRequestException;
import uz.melisa.registry.MemoryFactDefinition;
import uz.melisa.registry.MemoryFactDefinitionRegistry;
import uz.melisa.service.AiChatService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static uz.melisa.util.StringUtil.trimToEmpty;

/**
 * Turns the Haiku summary call into a validated {@link ExtractedMemory}.
 *
 * <p>The pipeline parses the model's JSON safely (invalid JSON is ignored), then for every fact:
 * rejects unknown (type, key) pairs and transactional data via the registry, requires the triggering
 * quote to exist in the customer-authored message, normalizes the value and validates structured
 * codes, assigns the real source_type (the model's sourceHint is advisory only), and drops transient
 * ("session-only") facts and uncertain allergy/dietary inferences so they never reach L1 or candidates.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryExtractionService {

    private static final int MAX_NORMALIZED_VALUE_LENGTH = 500;
    private static final String CODE_FIELD = "code";
    private static final String VALUE_FIELD = "value";

    private final AiChatService aiChatService;
    private final MemoryFactDefinitionRegistry registry;
    private final ObjectMapper objectMapper;

    public Optional<ExtractedMemory> extract(String previousSummary, String userText, String assistantText) {
        String raw = aiChatService.extractMemoryRaw(previousSummary, userText, assistantText);
        MemoryExtractionResponseDTO parsed = parse(raw);
        if (parsed == null) {
            return Optional.empty();
        }

        ExtractedMemory memory = new ExtractedMemory(
                trimToEmpty(parsed.summary()),
                sanitizeTopics(parsed.topics()),
                parseSentiment(parsed.sentiment()),
                validateFacts(parsed.facts(), userText)
        );
        return Optional.of(memory);
    }

    /**
     * Summary-only path for the inline per-turn post-processing. Returns just the running summary and
     * intentionally does NOT run the fact-extraction pipeline (that is the segment job's job), so facts
     * are never extracted twice.
     */
    public String extractSummaryOnly(String previousSummary, String userText, String assistantText) {
        String raw = aiChatService.extractMemoryRaw(previousSummary, userText, assistantText);
        MemoryExtractionResponseDTO parsed = parse(raw);
        return parsed == null ? "" : trimToEmpty(parsed.summary());
    }

    private MemoryExtractionResponseDTO parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String json = extractJsonObject(raw);
        try {
            return objectMapper.readValue(json, MemoryExtractionResponseDTO.class);
        } catch (Exception e) {
            log.warn("Memory extraction output is not valid JSON; ignoring. head='{}'",
                    json.substring(0, Math.min(json.length(), 120)));
            return null;
        }
    }

    private List<ExtractedFact> validateFacts(List<ExtractedFactDTO> rawFacts, String userText) {
        if (rawFacts == null || rawFacts.isEmpty()) {
            return List.of();
        }
        return rawFacts.stream()
                .map(fact -> validateFact(fact, userText))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<ExtractedFact> validateFact(ExtractedFactDTO fact, String userText) {
        if (fact == null) {
            return Optional.empty();
        }

        MemoryFactType factType = parseFactType(fact.type());
        if (factType == null || fact.key() == null) {
            return Optional.empty();
        }

        MemoryFactDefinition definition = lookupDefinition(factType, fact.key());
        if (definition == null) {
            return Optional.empty();   // unknown key / transactional / non-registry data
        }

        if (!quoteIsCustomerAuthored(fact.triggeringQuote(), userText)) {
            return Optional.empty();   // anti-hallucination: quote must be in the user's message
        }

        Optional<String> normalizedValue = normalize(definition, fact);
        if (normalizedValue.isEmpty()) {
            return Optional.empty();   // missing/invalid code or unresolved value
        }

        if (!Boolean.TRUE.equals(fact.stable())) {
            return Optional.empty();   // transient request, e.g. "show me something without nuts"
        }

        MemoryFactSourceType sourceType = resolveSourceType(fact);
        if (definition.safetyCritical() && sourceType == MemoryFactSourceType.REPEATED_INFERENCE) {
            // A stable, quote-verified, valid-code safety-critical fact is a clear declaration by
            // construction. Never let a missing/advisory sourceHint silently drop a real allergy/dietary;
            // genuine uncertainty is already handled (transient -> !stable drop, unknown code -> drop).
            sourceType = MemoryFactSourceType.EXPLICIT_CUSTOMER_STATEMENT;
        }

        return Optional.of(new ExtractedFact(
                factType,
                definition.factKey(),
                normalizedValue.get(),
                fact.valueJson() == null ? Map.of() : fact.valueJson(),
                definition.cardinality(),
                definition.riskClass(),
                sourceType,
                clampConfidence(fact.confidence()),
                true,
                definition.safetyCritical(),
                definition.promotableByRepeatedInference(),
                fact.triggeringQuote().trim()
        ));
    }

    private MemoryFactDefinition lookupDefinition(MemoryFactType factType, String factKey) {
        try {
            return registry.require(factType, factKey);
        } catch (BadRequestException e) {
            return null;
        }
    }

    private Optional<String> normalize(MemoryFactDefinition definition, ExtractedFactDTO fact) {
        return switch (definition.normalizationStrategy()) {
            case CODE_SET -> normalizeCode(definition, fact);
            case LOWERCASE_TEXT -> normalizeText(fact);
            case ORGANIZATION_REF -> Optional.empty();   // authoritative org resolution is downstream
        };
    }

    private Optional<String> normalizeCode(MemoryFactDefinition definition, ExtractedFactDTO fact) {
        String code = stringField(fact.valueJson(), CODE_FIELD);
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String canonical = code.trim().toUpperCase(Locale.ROOT);
        try {
            registry.validateStructuredCode(definition, canonical);
        } catch (BadRequestException e) {
            return Optional.empty();   // unknown allergen / dietary / spice code
        }
        return Optional.of(canonical);
    }

    private Optional<String> normalizeText(ExtractedFactDTO fact) {
        String value = stringField(fact.valueJson(), VALUE_FIELD);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_NORMALIZED_VALUE_LENGTH) {
            normalized = normalized.substring(0, MAX_NORMALIZED_VALUE_LENGTH);
        }
        return Optional.of(normalized);
    }

    private MemoryFactSourceType resolveSourceType(ExtractedFactDTO fact) {
        if (containsRememberPhrase(fact.triggeringQuote())) {
            return MemoryFactSourceType.EXPLICIT_REMEMBER_REQUEST;
        }
        MemoryFactSourceType hinted = parseSourceHint(fact.sourceHint());
        if (hinted == MemoryFactSourceType.EXPLICIT_REMEMBER_REQUEST
                || hinted == MemoryFactSourceType.EXPLICIT_CUSTOMER_STATEMENT) {
            return MemoryFactSourceType.EXPLICIT_CUSTOMER_STATEMENT;
        }
        return MemoryFactSourceType.REPEATED_INFERENCE;
    }

    private MemoryFactSourceType parseSourceHint(String hint) {
        if (hint == null) {
            return MemoryFactSourceType.REPEATED_INFERENCE;
        }
        try {
            return MemoryFactSourceType.valueOf(hint.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MemoryFactSourceType.REPEATED_INFERENCE;
        }
    }

    private boolean quoteIsCustomerAuthored(String quote, String userText) {
        if (quote == null || quote.isBlank() || userText == null) {
            return false;
        }
        return userText.toLowerCase(Locale.ROOT).contains(quote.trim().toLowerCase(Locale.ROOT));
    }

    private boolean containsRememberPhrase(String quote) {
        if (quote == null) {
            return false;
        }
        String lower = quote.toLowerCase(Locale.ROOT);
        return lower.contains("remember") || lower.contains("don't forget") || lower.contains("do not forget");
    }

    private MemoryFactType parseFactType(String type) {
        if (type == null) {
            return null;
        }
        try {
            return MemoryFactType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private MemoryEpisodeSentiment parseSentiment(String sentiment) {
        if (sentiment == null) {
            return MemoryEpisodeSentiment.UNKNOWN;
        }
        try {
            return MemoryEpisodeSentiment.valueOf(sentiment.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MemoryEpisodeSentiment.UNKNOWN;
        }
    }

    private List<String> sanitizeTopics(List<String> topics) {
        if (topics == null) {
            return List.of();
        }
        return topics.stream()
                .filter(topic -> topic != null && !topic.isBlank())
                .map(String::trim)
                .toList();
    }

    private BigDecimal clampConfidence(Double confidence) {
        if (confidence == null) {
            return null;
        }
        double bounded = Math.max(0.0, Math.min(1.0, confidence));
        return BigDecimal.valueOf(bounded);
    }

    private String stringField(Map<String, Object> valueJson, String field) {
        if (valueJson == null) {
            return null;
        }
        Object value = valueJson.get(field);
        return value == null ? null : value.toString();
    }

    private String extractJsonObject(String raw) {
        String cleaned = raw.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1).trim();
        }
        return cleaned;
    }
}

package uz.melisa.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uz.melisa.dto.memory.ExtractedMemory;
import uz.melisa.enums.MemoryFactSourceType;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.registry.MemoryFactDefinitionRegistry;
import uz.melisa.service.AiChatService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemoryExtractionServiceTest {

    private final AiChatService aiChatService = mock(AiChatService.class);
    private final MemoryExtractionService service = new MemoryExtractionService(
            aiChatService,
            new MemoryFactDefinitionRegistry(),
            new ObjectMapper()
    );

    @Test
    void extractsExplicitNameAsPermanentOtherFactAndPreservesCase() {
        when(aiChatService.extractMemoryRaw("", "My name is Jahongir", "Nice to meet you"))
                .thenReturn("""
                        {
                          "summary": "The user's name is Jahongir.",
                          "topics": ["identity"],
                          "sentiment": "NEUTRAL",
                          "facts": [
                            {
                              "type": "OTHER",
                              "key": "name",
                              "valueJson": {"value": "Jahongir"},
                              "triggeringQuote": "My name is Jahongir",
                              "confidence": 1.0,
                              "stable": true,
                              "sourceHint": "EXPLICIT_CUSTOMER_STATEMENT"
                            }
                          ]
                        }
                        """);

        Optional<ExtractedMemory> extracted = service.extract("", "My name is Jahongir", "Nice to meet you");

        assertThat(extracted).isPresent();
        assertThat(extracted.orElseThrow().facts()).hasSize(1);
        var fact = extracted.orElseThrow().facts().getFirst();
        assertThat(fact.factType()).isEqualTo(MemoryFactType.OTHER);
        assertThat(fact.factKey()).isEqualTo("name");
        assertThat(fact.normalizedValue()).isEqualTo("Jahongir");
        assertThat(fact.sourceType()).isEqualTo(MemoryFactSourceType.EXPLICIT_CUSTOMER_STATEMENT);
        assertThat(fact.promotableByRepeatedInference()).isFalse();
    }

    @Test
    void nameDoesNotDependOnModelSourceHintForPersistence() {
        when(aiChatService.extractMemoryRaw("", "My name is Jahongir", "Nice to meet you"))
                .thenReturn("""
                        {
                          "summary": "The user's name is Jahongir.",
                          "topics": [],
                          "sentiment": "NEUTRAL",
                          "facts": [
                            {
                              "type": "OTHER",
                              "key": "name",
                              "valueJson": {"value": "Jahongir"},
                              "triggeringQuote": "My name is Jahongir",
                              "confidence": 0.9,
                              "stable": true,
                              "sourceHint": "REPEATED_INFERENCE"
                            }
                          ]
                        }
                        """);

        var fact = service.extract("", "My name is Jahongir", "Nice to meet you")
                .orElseThrow()
                .facts()
                .getFirst();

        assertThat(fact.sourceType()).isEqualTo(MemoryFactSourceType.EXPLICIT_CUSTOMER_STATEMENT);
    }

    @Test
    void rejectsNameWhenTriggeringQuoteIsNotCustomerAuthored() {
        when(aiChatService.extractMemoryRaw("", "Hello", "Nice to meet you"))
                .thenReturn("""
                        {
                          "summary": "Greeting.",
                          "topics": [],
                          "sentiment": "NEUTRAL",
                          "facts": [
                            {
                              "type": "OTHER",
                              "key": "name",
                              "valueJson": {"value": "Jahongir"},
                              "triggeringQuote": "My name is Jahongir",
                              "confidence": 1.0,
                              "stable": true,
                              "sourceHint": "EXPLICIT_CUSTOMER_STATEMENT"
                            }
                          ]
                        }
                        """);

        var memory = service.extract("", "Hello", "Nice to meet you").orElseThrow();

        assertThat(memory.facts()).isEmpty();
    }

    @Test
    void acceptsAndNormalizesAllPreviouslyMismatchedPreferenceAndExclusionFacts() {
        String userText = "I usually like Uzbek cuisine, chicken, Mazzali, budget around 50000, "
                + "my food goal is high protein, avoid onion and never recommend Burger House.";

        when(aiChatService.extractMemoryRaw("", userText, "Understood"))
                .thenReturn("""
                        {
                          "summary": "Stable food preferences were stated.",
                          "topics": ["preferences"],
                          "sentiment": "NEUTRAL",
                          "facts": [
                            {
                              "type": "PREFERENCE",
                              "key": "cuisine",
                              "valueJson": {"value": "Uzbek"},
                              "triggeringQuote": "Uzbek cuisine",
                              "confidence": 0.95,
                              "stable": true,
                              "sourceHint": "EXPLICIT_CUSTOMER_STATEMENT"
                            },
                            {
                              "type": "PREFERENCE",
                              "key": "ingredient",
                              "valueJson": {"value": "Chicken"},
                              "triggeringQuote": "chicken",
                              "confidence": 0.95,
                              "stable": true,
                              "sourceHint": "EXPLICIT_CUSTOMER_STATEMENT"
                            },
                            {
                              "type": "PREFERENCE",
                              "key": "organization",
                              "valueJson": {"value": "Mazzali"},
                              "triggeringQuote": "Mazzali",
                              "confidence": 0.95,
                              "stable": true,
                              "sourceHint": "EXPLICIT_CUSTOMER_STATEMENT"
                            },
                            {
                              "type": "PREFERENCE",
                              "key": "budget",
                              "valueJson": {"value": "Around 50000"},
                              "triggeringQuote": "budget around 50000",
                              "confidence": 0.95,
                              "stable": true,
                              "sourceHint": "EXPLICIT_CUSTOMER_STATEMENT"
                            },
                            {
                              "type": "PREFERENCE",
                              "key": "goal",
                              "valueJson": {"value": "High Protein"},
                              "triggeringQuote": "food goal is high protein",
                              "confidence": 0.95,
                              "stable": true,
                              "sourceHint": "EXPLICIT_CUSTOMER_STATEMENT"
                            },
                            {
                              "type": "EXCLUSION",
                              "key": "ingredient",
                              "valueJson": {"value": "Onion"},
                              "triggeringQuote": "avoid onion",
                              "confidence": 1.0,
                              "stable": true,
                              "sourceHint": "EXPLICIT_CUSTOMER_STATEMENT"
                            },
                            {
                              "type": "EXCLUSION",
                              "key": "organization",
                              "valueJson": {"value": "Burger House"},
                              "triggeringQuote": "never recommend Burger House",
                              "confidence": 1.0,
                              "stable": true,
                              "sourceHint": "EXPLICIT_CUSTOMER_STATEMENT"
                            }
                          ]
                        }
                        """);

        var facts = service.extract("", userText, "Understood").orElseThrow().facts();

        assertThat(facts).hasSize(7);
        assertThat(facts).extracting(fact -> fact.factKey() + "=" + fact.normalizedValue())
                .containsExactlyInAnyOrder(
                        "cuisine=uzbek",
                        "ingredient=chicken",
                        "organization=mazzali",
                        "budget=around 50000",
                        "goal=high protein",
                        "ingredient=onion",
                        "organization=burger house"
                );
    }
}

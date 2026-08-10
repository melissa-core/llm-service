package uz.melisa.registry;

import org.junit.jupiter.api.Test;
import uz.melisa.enums.MemoryFactCardinality;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.enums.MemoryNormalizationStrategy;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryFactDefinitionRegistryTest {

    private final MemoryFactDefinitionRegistry registry = new MemoryFactDefinitionRegistry();

    @Test
    void nameIsSingleValuedPreservedTextAndNotInferencePromotable() {
        MemoryFactDefinition definition = registry.require(MemoryFactType.OTHER, "name");

        assertThat(definition.cardinality()).isEqualTo(MemoryFactCardinality.SINGLE);
        assertThat(definition.normalizationStrategy()).isEqualTo(MemoryNormalizationStrategy.PRESERVE_TEXT);
        assertThat(definition.promotableByRepeatedInference()).isFalse();
        assertThat(definition.safetyCritical()).isFalse();
    }

    @Test
    void everyFactPairAdvertisedByExtractionPromptHasARegistryDefinition() {
        assertDefinition(MemoryFactType.ALLERGY, "allergen");
        assertDefinition(MemoryFactType.DIETARY, "dietary_restriction");
        assertDefinition(MemoryFactType.PREFERENCE, "spice_level");
        assertDefinition(MemoryFactType.PREFERENCE, "cuisine");
        assertDefinition(MemoryFactType.PREFERENCE, "ingredient");
        assertDefinition(MemoryFactType.PREFERENCE, "organization");
        assertDefinition(MemoryFactType.PREFERENCE, "budget");
        assertDefinition(MemoryFactType.PREFERENCE, "goal");
        assertDefinition(MemoryFactType.EXCLUSION, "ingredient");
        assertDefinition(MemoryFactType.EXCLUSION, "organization");
        assertDefinition(MemoryFactType.INSTRUCTION, "communication");
        assertDefinition(MemoryFactType.OTHER, "name");
        assertDefinition(MemoryFactType.OTHER, "preferred_name");
    }

    @Test
    void preferenceAndExclusionDefinitionsHaveExpectedCardinalityAndNormalization() {
        assertThat(registry.require(MemoryFactType.PREFERENCE, "cuisine").cardinality())
                .isEqualTo(MemoryFactCardinality.MULTI);
        assertThat(registry.require(MemoryFactType.PREFERENCE, "ingredient").normalizationStrategy())
                .isEqualTo(MemoryNormalizationStrategy.LOWERCASE_TEXT);
        assertThat(registry.require(MemoryFactType.PREFERENCE, "organization").normalizationStrategy())
                .isEqualTo(MemoryNormalizationStrategy.ORGANIZATION_REF);
        assertThat(registry.require(MemoryFactType.PREFERENCE, "budget").cardinality())
                .isEqualTo(MemoryFactCardinality.SINGLE);
        assertThat(registry.require(MemoryFactType.PREFERENCE, "goal").cardinality())
                .isEqualTo(MemoryFactCardinality.SINGLE);
        assertThat(registry.require(MemoryFactType.EXCLUSION, "ingredient").cardinality())
                .isEqualTo(MemoryFactCardinality.MULTI);
    }

    private void assertDefinition(MemoryFactType type, String key) {
        assertThat(registry.find(type, key))
                .as("registry definition for %s/%s", type, key)
                .isPresent();
    }
}

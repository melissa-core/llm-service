package uz.melisa.registry;

import org.springframework.stereotype.Component;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.exp.BadRequestException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static uz.melisa.enums.MemoryFactCardinality.MULTI;
import static uz.melisa.enums.MemoryFactCardinality.SINGLE;
import static uz.melisa.enums.MemoryFactType.ALLERGY;
import static uz.melisa.enums.MemoryFactType.DIETARY;
import static uz.melisa.enums.MemoryFactType.EXCLUSION;
import static uz.melisa.enums.MemoryFactType.INSTRUCTION;
import static uz.melisa.enums.MemoryFactType.OTHER;
import static uz.melisa.enums.MemoryFactType.PREFERENCE;
import static uz.melisa.enums.MemoryNormalizationStrategy.CODE_SET;
import static uz.melisa.enums.MemoryNormalizationStrategy.LOWERCASE_TEXT;
import static uz.melisa.enums.MemoryNormalizationStrategy.ORGANIZATION_REF;
import static uz.melisa.enums.MemoryNormalizationStrategy.PRESERVE_TEXT;
import static uz.melisa.enums.MemoryRiskClass.HIGH;
import static uz.melisa.enums.MemoryRiskClass.ORDINARY;

/**
 * Single, server-owned source of truth for which facts may be stored and how.
 *
 * <p>{@code cardinality}, {@code risk_class}, the set of allowed structured codes, promotability
 * and safety-criticality all come from here, never from AI output. Unknown {@code (factType,
 * factKey)} pairs and unknown structured codes are rejected. Safety-critical facts (allergy,
 * dietary) are deliberately not promotable by repeated inference: they require an explicit source.
 */
@Component
public class MemoryFactDefinitionRegistry {

    private static final Set<String> ALLERGEN_CODES = Set.of(
            "PEANUT", "TREE_NUT", "MILK", "EGG", "WHEAT", "GLUTEN", "SOY",
            "FISH", "SHELLFISH", "CRUSTACEAN", "MOLLUSC", "SESAME",
            "MUSTARD", "CELERY", "LUPIN", "SULPHITE"
    );

    private static final Set<String> DIETARY_CODES = Set.of(
            "VEGETARIAN", "VEGAN", "PESCATARIAN", "HALAL", "KOSHER",
            "GLUTEN_FREE", "DAIRY_FREE", "LACTOSE_FREE", "NUT_FREE", "KETO", "LOW_CARB"
    );

    private static final Set<String> SPICE_LEVEL_CODES = Set.of(
            "NONE", "MILD", "MEDIUM", "HOT", "EXTRA_HOT"
    );

    private final Map<String, MemoryFactDefinition> definitions = buildDefinitions();

    public Optional<MemoryFactDefinition> find(MemoryFactType factType, String factKey) {
        if (factType == null || factKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitions.get(key(factType, factKey)));
    }

    public MemoryFactDefinition require(MemoryFactType factType, String factKey) {
        return find(factType, factKey)
                .orElseThrow(() -> new BadRequestException(
                        "Unknown memory fact: type=" + factType + ", key=" + factKey));
    }

    public void validateStructuredCode(MemoryFactDefinition definition, String code) {
        if (definition.hasClosedCodeSet() && !definition.isCodeAllowed(code)) {
            throw new BadRequestException(
                    "Unknown code '" + code + "' for memory fact key '" + definition.factKey() + "'");
        }
    }

    private static Map<String, MemoryFactDefinition> buildDefinitions() {
        Map<String, MemoryFactDefinition> map = new HashMap<>();
        register(map, new MemoryFactDefinition(
                ALLERGY, "allergen", MULTI, HIGH, CODE_SET, ALLERGEN_CODES, false, true));
        register(map, new MemoryFactDefinition(
                DIETARY, "dietary_restriction", MULTI, HIGH, CODE_SET, DIETARY_CODES, false, true));
        register(map, new MemoryFactDefinition(
                PREFERENCE, "spice_level", SINGLE, ORDINARY, CODE_SET, SPICE_LEVEL_CODES, true, false));
        register(map, new MemoryFactDefinition(
                PREFERENCE, "cuisine", MULTI, ORDINARY, LOWERCASE_TEXT, Set.of(), true, false));
        register(map, new MemoryFactDefinition(
                PREFERENCE, "ingredient", MULTI, ORDINARY, LOWERCASE_TEXT, Set.of(), true, false));
        register(map, new MemoryFactDefinition(
                PREFERENCE, "organization", MULTI, ORDINARY, ORGANIZATION_REF, Set.of(), true, false));
        register(map, new MemoryFactDefinition(
                PREFERENCE, "budget", SINGLE, ORDINARY, LOWERCASE_TEXT, Set.of(), true, false));
        register(map, new MemoryFactDefinition(
                PREFERENCE, "goal", SINGLE, ORDINARY, LOWERCASE_TEXT, Set.of(), true, false));
        register(map, new MemoryFactDefinition(
                EXCLUSION, "ingredient", MULTI, ORDINARY, LOWERCASE_TEXT, Set.of(), false, false));
        register(map, new MemoryFactDefinition(
                EXCLUSION, "organization", MULTI, ORDINARY, ORGANIZATION_REF, Set.of(), false, false));
        register(map, new MemoryFactDefinition(
                INSTRUCTION, "communication", SINGLE, ORDINARY, LOWERCASE_TEXT, Set.of(), true, false));
        register(map, new MemoryFactDefinition(
                OTHER, "name", SINGLE, ORDINARY, PRESERVE_TEXT, Set.of(), false, false));
        register(map, new MemoryFactDefinition(
                OTHER, "preferred_name", SINGLE, ORDINARY, PRESERVE_TEXT, Set.of(), false, false));
        return Map.copyOf(map);
    }

    private static void register(Map<String, MemoryFactDefinition> map, MemoryFactDefinition definition) {
        map.put(key(definition.factType(), definition.factKey()), definition);
    }

    private static String key(MemoryFactType factType, String factKey) {
        return factType.name() + ':' + factKey;
    }
}

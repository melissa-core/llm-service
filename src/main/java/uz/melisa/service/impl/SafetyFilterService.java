package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.domain.CustomerMemoryFact;
import uz.melisa.dto.client.catalog.ProductDTO;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.exp.BadRequestException;
import uz.melisa.registry.MemoryFactDefinition;
import uz.melisa.registry.MemoryFactDefinitionRegistry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic allergy/dietary/exclusion safety filter. The AI prompt is NOT a safety boundary:
 * unsafe products are removed here, in code, BEFORE they can reach the model input. Safety facts are
 * read straight from Postgres ({@link CustomerMemoryFactService#getActiveFacts}), never from Redis, so
 * stale cache can never make an unsafe item eligible. Every code is validated against the authoritative
 * registry before it is used.
 *
 * <p><b>LIMITATION — tag-based filtering CANNOT guarantee allergen or dietary safety.</b> {@code ProductDTO}
 * exposes only a free-form {@code Set<String> tags} plus an {@code organizationName}; it has no structured
 * allergen codes, dietary attributes or organization id. Matching customer codes against product tags is a
 * temporary, best-effort heuristic: a product whose tags omit an allergen it actually contains is NOT excluded
 * (false negative), and tag vocabularies are not guaranteed to align with the registry codes. This filter
 * reduces, but does not eliminate, exposure of unsafe items.
 *
 * <p>TODO(memory-safety): migrate to authoritative structured catalog fields and canonical-code filtering —
 * add {@code allergenCodes: Set<String>}, {@code dietaryAttributes: Set<String>} and {@code organizationId: Long}
 * to the product model (catalog schema + ProductDTO + the embeddingToProduct API), then match on those canonical
 * codes/ids instead of free-text tags and drop the heuristic DIETARY_FORBIDDEN_TAGS map. Deferred deliberately;
 * ProductDTO / catalog schemas / upstream APIs are intentionally NOT changed as part of the memory work.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SafetyFilterService {

    /** Server-owned dietary -> forbidden product-tag registry (uppercased codes). */
    private static final Map<String, Set<String>> DIETARY_FORBIDDEN_TAGS = Map.ofEntries(
            Map.entry("VEGAN", Set.of("MEAT", "BEEF", "PORK", "CHICKEN", "LAMB", "FISH", "SEAFOOD", "SHELLFISH",
                    "DAIRY", "MILK", "CHEESE", "EGG", "HONEY", "GELATIN")),
            Map.entry("VEGETARIAN", Set.of("MEAT", "BEEF", "PORK", "CHICKEN", "LAMB", "FISH", "SEAFOOD",
                    "SHELLFISH", "GELATIN")),
            Map.entry("PESCATARIAN", Set.of("MEAT", "BEEF", "PORK", "CHICKEN", "LAMB")),
            Map.entry("HALAL", Set.of("PORK", "ALCOHOL", "GELATIN")),
            Map.entry("KOSHER", Set.of("PORK", "SHELLFISH")),
            Map.entry("GLUTEN_FREE", Set.of("GLUTEN", "WHEAT")),
            Map.entry("DAIRY_FREE", Set.of("DAIRY", "MILK", "CHEESE", "LACTOSE")),
            Map.entry("LACTOSE_FREE", Set.of("DAIRY", "MILK", "CHEESE", "LACTOSE")),
            Map.entry("NUT_FREE", Set.of("PEANUT", "TREE_NUT", "NUT", "NUTS"))
    );

    private final CustomerMemoryFactService factService;
    private final MemoryFactDefinitionRegistry registry;

    /** Remove every product that is unsafe for the customer's confirmed active facts. */
    public List<ProductDTO> filterSafe(Long customerId, List<ProductDTO> products) {
        if (customerId == null || products == null || products.isEmpty()) {
            return products == null ? List.of() : products;
        }
        List<CustomerMemoryFact> activeFacts = factService.getActiveFacts(customerId);   // Postgres-direct, never Redis
        if (activeFacts.isEmpty()) {
            return products;
        }
        Set<String> forbiddenAllergens = validatedCodes(activeFacts, MemoryFactType.ALLERGY);
        Set<String> forbiddenDietaryTags = dietaryForbiddenTags(activeFacts);
        Set<String> excludedOrganizations = organizationExclusions(activeFacts);
        Set<String> excludedIngredients = ingredientExclusions(activeFacts);

        List<ProductDTO> safe = products.stream()
                .filter(product -> isSafe(
                        product,
                        forbiddenAllergens,
                        forbiddenDietaryTags,
                        excludedOrganizations,
                        excludedIngredients
                ))
                .toList();
        if (safe.size() < products.size()) {
            log.info("safety filter removed {} unsafe product(s) for customerId={}", products.size() - safe.size(), customerId);
        }
        return safe;
    }

    private boolean isSafe(ProductDTO product, Set<String> forbiddenAllergens,
                           Set<String> forbiddenDietaryTags, Set<String> excludedOrganizations,
                           Set<String> excludedIngredients) {
        if (!excludedOrganizations.isEmpty() && excludedOrganizations.contains(normalize(product.getOrganizationName()))) {
            return false;
        }
        for (String tag : normalizedTags(product)) {
            if (forbiddenAllergens.contains(tag)
                    || forbiddenDietaryTags.contains(tag)
                    || excludedIngredients.contains(tag)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> validatedCodes(List<CustomerMemoryFact> facts, MemoryFactType factType) {
        return facts.stream()
                .filter(fact -> fact.getFactType() == factType)
                .map(this::validatedCode)
                .filter(code -> code != null)
                .collect(Collectors.toSet());
    }

    private Set<String> dietaryForbiddenTags(List<CustomerMemoryFact> facts) {
        return validatedCodes(facts, MemoryFactType.DIETARY).stream()
                .map(DIETARY_FORBIDDEN_TAGS::get)
                .filter(tags -> tags != null)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private Set<String> organizationExclusions(List<CustomerMemoryFact> facts) {
        return facts.stream()
                .filter(fact -> fact.getFactType() == MemoryFactType.EXCLUSION)
                .filter(fact -> "organization".equals(fact.getFactKey()))
                .map(fact -> normalize(fact.getNormalizedValue()))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private Set<String> ingredientExclusions(List<CustomerMemoryFact> facts) {
        return facts.stream()
                .filter(fact -> fact.getFactType() == MemoryFactType.EXCLUSION)
                .filter(fact -> "ingredient".equals(fact.getFactKey()))
                .map(fact -> normalize(fact.getNormalizedValue()))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    /** Validate the fact's code against the authoritative registry; ignore (and log) anything unknown. */
    private String validatedCode(CustomerMemoryFact fact) {
        String code = normalize(fact.getNormalizedValue());
        MemoryFactDefinition definition = registry.find(fact.getFactType(), fact.getFactKey()).orElse(null);
        if (definition == null) {
            return null;
        }
        try {
            registry.validateStructuredCode(definition, code);
            return code;
        } catch (BadRequestException e) {
            log.warn("safety filter ignoring unknown code '{}' for fact key '{}'", code, fact.getFactKey());
            return null;
        }
    }

    private Set<String> normalizedTags(ProductDTO product) {
        if (product.getTags() == null) {
            return Set.of();
        }
        return product.getTags().stream().map(this::normalize).collect(Collectors.toSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

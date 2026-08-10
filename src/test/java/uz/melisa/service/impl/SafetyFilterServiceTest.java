package uz.melisa.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.melisa.domain.CustomerMemoryFact;
import uz.melisa.dto.client.catalog.ProductDTO;
import uz.melisa.enums.MemoryFactCardinality;
import uz.melisa.enums.MemoryFactSourceType;
import uz.melisa.enums.MemoryFactStatus;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.enums.MemoryRiskClass;
import uz.melisa.registry.MemoryFactDefinitionRegistry;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafetyFilterServiceTest {

    private static final long CUSTOMER_ID = 1L;

    @Mock
    private CustomerMemoryFactService factService;

    private SafetyFilterService safetyFilterService;

    @BeforeEach
    void setUp() {
        safetyFilterService = new SafetyFilterService(factService, new MemoryFactDefinitionRegistry());
    }

    @Test
    void excludesProductsTaggedWithAConfirmedAllergen() {
        when(factService.getActiveFacts(CUSTOMER_ID)).thenReturn(List.of(allergy("PEANUT")));
        ProductDTO unsafe = product(10L, Set.of("PEANUT", "SPICY"));
        ProductDTO safe = product(11L, Set.of("VEGETARIAN"));

        List<ProductDTO> result = safetyFilterService.filterSafe(CUSTOMER_ID, List.of(unsafe, safe));

        assertThat(result).extracting(ProductDTO::getId).containsExactly(11L);
    }

    @Test
    void excludesProductsViolatingAConfirmedDietaryRestriction() {
        when(factService.getActiveFacts(CUSTOMER_ID)).thenReturn(List.of(dietary("VEGAN")));
        ProductDTO unsafe = product(20L, Set.of("BEEF", "CHEESE"));
        ProductDTO safe = product(21L, Set.of("FRUIT"));

        List<ProductDTO> result = safetyFilterService.filterSafe(CUSTOMER_ID, List.of(unsafe, safe));

        assertThat(result).extracting(ProductDTO::getId).containsExactly(21L);
    }

    @Test
    void matchesAllergenCodesCaseInsensitively() {
        when(factService.getActiveFacts(CUSTOMER_ID)).thenReturn(List.of(allergy("TREE_NUT")));
        ProductDTO unsafe = product(30L, Set.of("tree_nut"));

        List<ProductDTO> result = safetyFilterService.filterSafe(CUSTOMER_ID, List.of(unsafe));

        assertThat(result).isEmpty();
    }

    @Test
    void keepsEveryProductWhenTheCustomerHasNoFacts() {
        when(factService.getActiveFacts(CUSTOMER_ID)).thenReturn(List.of());
        ProductDTO a = product(40L, Set.of("PEANUT"));
        ProductDTO b = product(41L, Set.of("BEEF"));

        List<ProductDTO> result = safetyFilterService.filterSafe(CUSTOMER_ID, List.of(a, b));

        assertThat(result).extracting(ProductDTO::getId).containsExactly(40L, 41L);
    }

    @Test
    void excludesAConfirmedOrganizationExclusionButDoesNotTreatIngredientAsOrganization() {
        when(factService.getActiveFacts(CUSTOMER_ID)).thenReturn(List.of(
                fact(MemoryFactType.EXCLUSION, "organization", "burger house"),
                fact(MemoryFactType.EXCLUSION, "ingredient", "onion")
        ));
        ProductDTO blockedOrganization = product(50L, Set.of("BEEF"));
        blockedOrganization.setOrganizationName("Burger House");
        ProductDTO safeOrganization = product(51L, Set.of("CHEESE"));
        safeOrganization.setOrganizationName("Onion");

        List<ProductDTO> result = safetyFilterService.filterSafe(
                CUSTOMER_ID,
                List.of(blockedOrganization, safeOrganization)
        );

        assertThat(result).extracting(ProductDTO::getId).containsExactly(51L);
    }

    @Test
    void excludesProductsTaggedWithAConfirmedIngredientExclusion() {
        when(factService.getActiveFacts(CUSTOMER_ID)).thenReturn(List.of(
                fact(MemoryFactType.EXCLUSION, "ingredient", "onion")
        ));
        ProductDTO blocked = product(60L, Set.of("ONION", "BEEF"));
        ProductDTO safe = product(61L, Set.of("CHEESE"));

        List<ProductDTO> result = safetyFilterService.filterSafe(CUSTOMER_ID, List.of(blocked, safe));

        assertThat(result).extracting(ProductDTO::getId).containsExactly(61L);
    }

    private CustomerMemoryFact allergy(String code) {
        return fact(MemoryFactType.ALLERGY, "allergen", code);
    }

    private CustomerMemoryFact dietary(String code) {
        return fact(MemoryFactType.DIETARY, "dietary_restriction", code);
    }

    private CustomerMemoryFact fact(MemoryFactType type, String key, String value) {
        return CustomerMemoryFact.builder()
                .customerId(CUSTOMER_ID)
                .factType(type)
                .factKey(key)
                .cardinality(MemoryFactCardinality.MULTI)
                .riskClass(MemoryRiskClass.HIGH)
                .normalizedValue(value)
                .sourceType(MemoryFactSourceType.EXPLICIT_CUSTOMER_STATEMENT)
                .status(MemoryFactStatus.ACTIVE)
                .build();
    }

    private ProductDTO product(Long id, Set<String> tags) {
        ProductDTO product = new ProductDTO();
        product.setId(id);
        product.setTags(tags);
        return product;
    }
}

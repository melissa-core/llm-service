package uz.melisa.service.impl;

import org.junit.jupiter.api.Test;
import uz.melisa.config.AiProperties;
import uz.melisa.dto.memory.MemoryContext;
import uz.melisa.dto.memory.MemoryFactView;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.repository.CustomerMemoryEpisodeRepository;
import uz.melisa.repository.CustomerMemorySettingsRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MemoryContextAssemblerTest {

    @Test
    void rendersOtherFactsWithTheirKeys() {
        MemoryContextAssembler assembler = new MemoryContextAssembler(
                mock(CustomerMemoryFactService.class),
                mock(CustomerMemoryEpisodeRepository.class),
                mock(CustomerMemorySettingsRepository.class),
                mock(AiProperties.class)
        );
        MemoryContext context = new MemoryContext(
                42L,
                1L,
                3L,
                List.of(
                        new MemoryFactView(MemoryFactType.OTHER, "name", "Jahongir", Map.of("value", "Jahongir")),
                        new MemoryFactView(MemoryFactType.OTHER, "preferred_name", "Jaha", Map.of("value", "Jaha"))
                ),
                List.of()
        );

        String prompt = assembler.renderPromptBlock(context);

        assertThat(prompt).contains("Other durable facts:");
        assertThat(prompt).contains("name: Jahongir");
        assertThat(prompt).contains("preferred_name: Jaha");
    }

    @Test
    void rendersPreferenceAndExclusionFactsWithKeysSoValuesRemainUnambiguous() {
        MemoryContextAssembler assembler = new MemoryContextAssembler(
                mock(CustomerMemoryFactService.class),
                mock(CustomerMemoryEpisodeRepository.class),
                mock(CustomerMemorySettingsRepository.class),
                mock(AiProperties.class)
        );
        MemoryContext context = new MemoryContext(
                42L,
                1L,
                3L,
                List.of(
                        new MemoryFactView(MemoryFactType.PREFERENCE, "cuisine", "uzbek", Map.of("value", "Uzbek")),
                        new MemoryFactView(MemoryFactType.PREFERENCE, "budget", "around 50000", Map.of("value", "Around 50000")),
                        new MemoryFactView(MemoryFactType.EXCLUSION, "ingredient", "onion", Map.of("value", "Onion"))
                ),
                List.of()
        );

        String prompt = assembler.renderPromptBlock(context);

        assertThat(prompt).contains("Preferences:");
        assertThat(prompt).contains("cuisine: uzbek");
        assertThat(prompt).contains("budget: around 50000");
        assertThat(prompt).contains("Exclusions:");
        assertThat(prompt).contains("ingredient: onion");
    }
}

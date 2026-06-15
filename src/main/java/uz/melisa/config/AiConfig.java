package uz.melisa.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatProperties;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient geminiChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Spring AI's Anthropic auto-configuration stamps a framework default temperature
     * ({@code AnthropicChatModel.DEFAULT_TEMPERATURE = 0.8}) onto the chat model's
     * default options, and merges those defaults into every request — filling in any
     * sampling parameter the per-request options leave null. Newer Claude models
     * (opus-4-7/4-8, fable-5, mythos-5) reject {@code temperature}/{@code top_p} with
     * HTTP 400, so the PRODUCT route (claude-opus-4-8) failed even though
     * {@link uz.melisa.service.AiChatService#buildOptions} never sets them.
     *
     * <p>Clear the framework defaults before the chat model is built (the properties
     * bean is fully initialized before the model that depends on it). Routes that DO
     * accept sampling params still receive them explicitly from {@code buildOptions}
     * (which guards by model via {@code supportsSamplingParams}), so this only removes
     * the erroneous fallback for the newer models — it does not change behaviour for
     * haiku/sonnet, whose temperatures are always set per request.
     */
    @Bean
    static BeanPostProcessor anthropicDefaultSamplingParamsCleaner() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof AnthropicChatProperties properties && properties.getOptions() != null) {
                    properties.getOptions().setTemperature(null);
                    properties.getOptions().setTopP(null);
                }
                return bean;
            }
        };
    }
}

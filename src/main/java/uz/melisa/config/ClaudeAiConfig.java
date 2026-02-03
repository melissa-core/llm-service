package uz.melisa.config;

import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static uz.melisa.constants.PrivacyConstants.SYSTEM_COMMAND;

@Configuration
public class ClaudeAiConfig {

    @Bean
    public ChatClient claudeChatClient(ChatClient.Builder builder, ClaudeProperties props) {
        ClaudeProperties.Claude claude = props.getClaude();
        var optionsBuilder = AnthropicChatOptions.builder()
                .model(claude.getModel())
                .maxTokens(claude.getMaxOutputTokens());

        if (claude.getTemperature() != null) {
            optionsBuilder.temperature(claude.getTemperature());
        }
        return builder
                .defaultSystem(SYSTEM_COMMAND)
                .defaultOptions(optionsBuilder.build())
                .build();
    }
}

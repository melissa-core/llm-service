package uz.melisa.config;

import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClaudeAiConfig {

    @Bean
    public ChatClient claudeChatClient(ChatClient.Builder builder, ClaudeProperties props) {

        var optionsBuilder = AnthropicChatOptions.builder()
                .model(props.getModel())
                .maxTokens(props.getMaxOutputTokens());

        if (props.getTemperature() != null) {
            optionsBuilder.temperature(props.getTemperature());
        }
        var options = optionsBuilder.build();

        return builder
                .defaultSystem(props.getSystem())
                .defaultOptions(options)
                .build();
    }
}

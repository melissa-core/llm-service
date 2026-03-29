package uz.melisa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import uz.melisa.config.AiProperties;
import uz.melisa.dto.claude.AIResult;
import uz.melisa.enums.AiRoute;

import static uz.melisa.util.StringUtil.trimToEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final ChatClient chatClient;
    private final AiProperties props;
    private final AiPromptResolver aiPromptResolver;
    private final ChatMemory chatMemory;
    private final ObjectMapper objectMapper;

    public boolean isProductBased(String userText) {
        String safeInput = enforceMaxChars(userText, props.getClassifier().getMaxInputChars());

        ChatResponse resp = chatClient.prompt()
                .system(aiPromptResolver.resolve(AiRoute.CLASSIFIER))
                .options(buildOptions(props.getClassifier()))
                .user(safeInput)
                .call()
                .chatResponse();

        log.info("The chat response : {}", resp);
        String text = extractContent(resp);
        log.info("Classifier RAW output: >>>{}<<<",
                text == null ? "NULL" : text.substring(0, Math.min(text.length(), 400)));

        return parseProductBased(text);
    }

    public String summarize(String previousSummary, String userText, String assistantText) {
        String input = buildSummaryInput(previousSummary, userText, assistantText);
        AIResult result = chat(AiRoute.SUMMARY, null, input);
        return result == null ? "" : trimToEmpty(result.getText());
    }

    public AIResult chat(AiRoute route, String conversationId, String userText) {
        AiProperties.ModelProfile profile = profile(route);
        String safeInput = enforceMaxChars(userText, profile.getMaxInputChars());

        var spec = chatClient.prompt()
                .system(aiPromptResolver.resolve(route))
                .options(buildOptions(profile));

        if (conversationId != null && !conversationId.isBlank() && route != AiRoute.CLASSIFIER) {
            spec = spec.advisors(
                    MessageChatMemoryAdvisor.builder(chatMemory)
                            .conversationId(conversationId)
                            .build()
            );
        }

        ChatResponse resp = spec.user(safeInput)
                .call()
                .chatResponse();

        String text = extractContent(resp);
        return toAiResult(resp, text);
    }

    private AiProperties.ModelProfile profile(AiRoute route) {
        return switch (route) {
            case CLASSIFIER -> props.getClassifier();
            case GENERAL -> props.getGeneral();
            case PRODUCT -> props.getProduct();
            case SUMMARY -> props.getSummary();
        };
    }

    private AnthropicChatOptions buildOptions(AiProperties.ModelProfile profile) {
        var builder = AnthropicChatOptions.builder()
                .temperature(0.0)
                .model(profile.getModel())
                .maxTokens(profile.getMaxOutputTokens());

        if (profile.getTemperature() != null) {
            builder.temperature(profile.getTemperature());
        }
        if (profile.getTopP() != null) {
            builder.topP(profile.getTopP());
        }
        return builder.build();
    }

    private boolean parseProductBased(String text) {
        if (text == null) return false;

        String cleaned = text.trim();

        cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
        cleaned = cleaned.replaceFirst("\\s*```$", "");

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1).trim();
        }

        try {
            JsonNode root = objectMapper.readTree(cleaned);
            return root.path("productBased").asBoolean(false);
        } catch (Exception e) {
            log.warn("Classifier output is not valid JSON. head='{}'",
                    cleaned.substring(0, Math.min(cleaned.length(), 120)));
            return false;
        }
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.getResults().isEmpty()) {
            return "";
        }
        var output = response.getResults().getFirst().getOutput();
        return output.getText() == null ? "" : output.getText();
    }

    private String buildSummaryInput(String previousSummary, String userText, String assistantText) {
        return """
                Previous summary:
                %s
                
                Latest exchange:
                User: %s
                Assistant: %s
                
                Write the updated summary.
                """.formatted(
                trimToEmpty(previousSummary),
                trimToEmpty(userText),
                trimToEmpty(assistantText)
        );
    }

    private String enforceMaxChars(String text, Integer maxChars) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (maxChars == null || maxChars <= 0 || trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxChars);
    }

    private AIResult toAiResult(ChatResponse resp, String text) {
        return new AIResult(text, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
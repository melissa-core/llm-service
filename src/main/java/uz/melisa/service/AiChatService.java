package uz.melisa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
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

        String text = extractContent(resp);
        log.info("Classifier RAW output: >>>{}<<<",
                text == null ? "NULL" : text.substring(0, Math.min(text.length(), 400)));

        return parseProductBased(text);
    }

    public String extractMemoryRaw(String previousSummary, String userText, String assistantText) {
        String input = buildSummaryInput(previousSummary, userText, assistantText);
        AIResult result = chat(AiRoute.SUMMARY, null, input);
        return result == null ? "" : trimToEmpty(result.getText());
    }

    public AIResult chat(AiRoute route, String conversationId, String userText) {
        ChatResponse resp = buildRequestSpec(route, conversationId, userText)
                .call()
                .chatResponse();

        String text = extractContent(resp);
        return toAiResult(resp, resp, text);
    }

    public Flux<ChatResponse> chatStream(AiRoute route, String conversationId, String userText) {
        return buildRequestSpec(route, conversationId, userText)
                .stream()
                .chatResponse();
    }

    private ChatClient.ChatClientRequestSpec buildRequestSpec(AiRoute route, String conversationId, String userText) {
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

        return spec.user(safeInput);
    }

    private AiProperties.ModelProfile profile(AiRoute route) {
        return switch (route) {
            case CLASSIFIER -> props.getClassifier();
            case GENERAL -> props.getGeneral();
            case PRODUCT -> props.getProduct();
            case SUMMARY -> props.getSummary();
        };
    }

    private ChatOptions buildOptions(AiProperties.ModelProfile profile) {
        var builder = ChatOptions.builder()
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
        if (text == null || text.isBlank()) {
            return false;
        }
        String cleaned = text.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1).trim();
        }

        try {
            JsonNode root = objectMapper.readTree(cleaned);

            JsonNode node = root.get("productBased");
            if (node == null) {
                node = root.get("product_based");
            }

            if (node == null) {
                return false;
            }

            if (node.isBoolean()) {
                return node.asBoolean(false);
            }

            if (node.isTextual()) {
                return Boolean.parseBoolean(node.asText().trim());
            }

            return false;
        } catch (Exception e) {
            log.warn("Classifier output is not valid JSON. head='{}'",
                    cleaned.substring(0, Math.min(cleaned.length(), 120)));
            return false;
        }
    }

    public String extractContent(ChatResponse response) {
        return extractContentRaw(response).trim();
    }

    /**
     * Returns the chunk text without trimming. Streaming must use this so that
     * inter-token whitespace (leading spaces, newlines) is preserved across
     * deltas; trimming each chunk would collapse spacing in the rendered and
     * persisted answer. The blocking path uses {@link #extractContent} which
     * trims the full response once.
     */
    public String extractContentRaw(ChatResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return "";
        }

        var output = response.getResults().getFirst().getOutput();
        if (output == null || output.getText() == null) {
            return "";
        }

        return output.getText();
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
        if (text == null) {
            return "";
        }

        String trimmed = text.trim();
        if (maxChars == null || maxChars <= 0 || trimmed.length() <= maxChars) {
            return trimmed;
        }

        return trimmed.substring(0, maxChars);
    }

    /**
     * Builds an AIResult from response metadata. For streaming, pass the first
     * chunk (carries model/id from message_start) and the last chunk (carries
     * final usage from message_delta); for blocking calls pass the same response twice.
     */
    public AIResult toAiResult(ChatResponse first, ChatResponse last, String text) {
        ChatResponseMetadata firstMeta = first == null ? null : first.getMetadata();
        ChatResponseMetadata lastMeta = last == null ? null : last.getMetadata();

        String model = firstNonBlank(metadataModel(lastMeta), metadataModel(firstMeta));
        String responseId = firstNonBlank(metadataId(lastMeta), metadataId(firstMeta));
        String finishReason = extractFinishReason(last);

        Usage usage = pickUsage(lastMeta, firstMeta);
        Integer promptTokens = usage == null ? null : usage.getPromptTokens();
        Integer completionTokens = usage == null ? null : usage.getCompletionTokens();
        Integer totalTokens = usage == null ? null : usage.getTotalTokens();

        Integer cacheCreationInputTokens = null;
        Integer cacheReadInputTokens = null;
        Object nativeUsage = usage == null ? null : usage.getNativeUsage();
        if (nativeUsage instanceof AnthropicApi.Usage anthropicUsage) {
            cacheCreationInputTokens = anthropicUsage.cacheCreationInputTokens();
            cacheReadInputTokens = anthropicUsage.cacheReadInputTokens();
        }

        return new AIResult(
                text,
                responseId,
                model,
                finishReason,
                promptTokens,
                completionTokens,
                totalTokens,
                promptTokens,
                completionTokens,
                cacheCreationInputTokens,
                cacheReadInputTokens,
                null,
                toJsonSafe(nativeUsage),
                null
        );
    }

    private String metadataModel(ChatResponseMetadata metadata) {
        return metadata == null ? null : metadata.getModel();
    }

    private String metadataId(ChatResponseMetadata metadata) {
        return metadata == null ? null : metadata.getId();
    }

    private String extractFinishReason(ChatResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return null;
        }
        var generationMetadata = response.getResults().getFirst().getMetadata();
        return generationMetadata == null ? null : generationMetadata.getFinishReason();
    }

    private Usage pickUsage(ChatResponseMetadata preferred, ChatResponseMetadata fallback) {
        Usage usage = preferred == null ? null : preferred.getUsage();
        if (hasTokenCounts(usage)) {
            return usage;
        }
        Usage fallbackUsage = fallback == null ? null : fallback.getUsage();
        return hasTokenCounts(fallbackUsage) ? fallbackUsage : usage;
    }

    private boolean hasTokenCounts(Usage usage) {
        return usage != null && usage.getTotalTokens() != null && usage.getTotalTokens() > 0;
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private String toJsonSafe(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }
}

package uz.melisa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import uz.melisa.config.ClaudeProperties;
import uz.melisa.dto.claude.ClaudeResult;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeChatService {

    private final ChatClient claudeChatClient;
    private final ClaudeProperties props;
    private final ChatMemory chatMemory;
    private final ObjectMapper objectMapper;

    public ClaudeResult chatWithClaude(String conversationId, String userText) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId must not be null/blank");
        }
        String safeInput = enforceMaxChars(userText, props.getClaude().getMaxInputChars());

        ChatResponse resp = claudeChatClient.prompt()
                .advisors(getMemoryAdvisor(conversationId))
                .user(safeInput)
                .call()
                .chatResponse();

        String text = extractContent(resp);
        return getClaudeResult(resp, text);
    }

    private MessageChatMemoryAdvisor getMemoryAdvisor(String conversationId) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(conversationId)
                .build();
    }

    private String extractContent(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResults() == null || chatResponse.getResults().isEmpty()) return "";
        var out = chatResponse.getResults().getFirst().getOutput();
        return (out != null && out.getText() != null) ? out.getText() : "";
    }

    private String enforceMaxChars(String text, int maxChars) {
        if (text == null) return "";
        String t = text.trim();
        return (t.length() <= maxChars) ? t : t.substring(0, maxChars);
    }

    private ClaudeResult getClaudeResult(ChatResponse resp, String text) {
        JsonNode root = objectMapper.valueToTree(resp);

        String responseId = textAt(root, "metadata", "id");
        String model = textAt(root, "metadata", "model");
        String finishReason = firstNonBlank(
                textAt(root, "result", "metadata", "finishReason"),
                textAt(root, "results", "0", "metadata", "finishReason")
        );

        Integer promptTokens = intAt(root, "metadata", "usage", "promptTokens");
        Integer completionTokens = intAt(root, "metadata", "usage", "completionTokens");
        Integer totalTokens = intAt(root, "metadata", "usage", "totalTokens");

        Integer inputTokens = intAt(root, "metadata", "usage", "nativeUsage", "input_tokens");
        Integer outputTokens = intAt(root, "metadata", "usage", "nativeUsage", "output_tokens");
        Integer cacheCreation = intAt(root, "metadata", "usage", "nativeUsage", "cache_creation_input_tokens");
        Integer cacheRead = intAt(root, "metadata", "usage", "nativeUsage", "cache_read_input_tokens");

        String rateLimitRaw = toJson(root.path("metadata").path("rateLimit"));
        String usageRaw = toJson(root.path("metadata").path("usage"));
        String responseRaw = toJson(root);

        return new ClaudeResult(
                trimToEmpty(text),
                responseId,
                trimToEmpty(model),
                finishReason,
                promptTokens,
                completionTokens,
                totalTokens,
                inputTokens,
                outputTokens,
                cacheCreation,
                cacheRead,
                rateLimitRaw,
                usageRaw,
                responseRaw
        );
    }

    private String toJson(JsonNode node) {
        try {
            if (node == null || node.isMissingNode() || node.isNull()) return null;
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    private String textAt(JsonNode root, String... path) {
        JsonNode current = root;
        for (String p : path) current = current.path(p);
        if (current.isMissingNode() || current.isNull()) return null;
        String v = current.asText();
        return v == null || v.isBlank() ? null : v;
    }

    private Integer intAt(JsonNode root, String... path) {
        JsonNode current = root;
        for (String p : path) current = current.path(p);
        if (current.isMissingNode() || current.isNull()) return null;
        if (current.isNumber()) return current.asInt();
        String v = current.asText();
        if (v == null || v.isBlank()) return null;
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

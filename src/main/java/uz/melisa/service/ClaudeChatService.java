package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import uz.melisa.config.ClaudeProperties;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeChatService {

    private final ChatClient claudeChatClient;
    private final ClaudeProperties props;
    private final ChatMemory chatMemory;

    public String chatWithClaude(String conversationId, String userText) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId must not be null/blank");
        }
        String safeInput = enforceMaxChars(userText, props.getClaude().getMaxInputChars());

        ChatResponse resp = claudeChatClient.prompt()
                .advisors(getMemoryAdvisor(conversationId))
                .user(safeInput)
                .call()
                .chatResponse();

        String content = extractContent(resp);

        log.info("Claude conversationId={} content={}", conversationId, content);
        return content;
    }

    private MessageChatMemoryAdvisor getMemoryAdvisor(String conversationId) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(conversationId)
                .build();
    }

    private String extractContent(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResults() == null || chatResponse.getResults().isEmpty()) return "";
        var out = chatResponse.getResults().get(0).getOutput();
        return (out != null && out.getText() != null) ? out.getText() : "";
    }

    private String enforceMaxChars(String text, int maxChars) {
        if (text == null) return "";
        String t = text.trim();
        return (t.length() <= maxChars) ? t : t.substring(0, maxChars);
    }
}

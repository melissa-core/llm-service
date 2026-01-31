package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import uz.melisa.config.ClaudeProperties;

import static uz.melisa.constants.PrivacyConstants.SYSTEM_COMMAND;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeChatService {

    private final ChatClient claudeChatClient;
    private final ClaudeProperties props;

    public String chatWithClaude(String userText) {
        String safeInput = enforceMaxChars(userText, props.getMaxInputChars());

        ChatClient.CallResponseSpec call = claudeChatClient.prompt()
                .system(SYSTEM_COMMAND)
                .user(safeInput)
                .call();

        var chatResponse = call.chatResponse();

        String content = extractContent(chatResponse);

        try {
            log.info("Claude chatResponse json:\n{}",
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .findAndRegisterModules()
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(chatResponse));
        } catch (Exception e) {
            log.warn("Cannot serialize chatResponse. Err={}", e.toString());
        }

        log.info("Claude content: {}", content);
        return content;
    }

    private static String extractContent(org.springframework.ai.chat.model.ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResults() == null || chatResponse.getResults().isEmpty()) {
            return "";
        }
        var output = chatResponse.getResults().get(0).getOutput();
        return (output != null && output.getText() != null) ? output.getText() : "";
    }

    private static String enforceMaxChars(String text, int maxChars) {
        if (text == null) return "";
        String t = text.trim();
        if (t.length() <= maxChars) return t;
        return t.substring(0, maxChars);
    }
}

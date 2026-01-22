package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import uz.melisa.config.ClaudeProperties;

@Service
@RequiredArgsConstructor
public class ClaudeChatService {

    private final ChatClient claudeChatClient;
    private final ClaudeProperties props;

    public String chatWithClaude(String userText) {
        String safeInput = enforceMaxChars(userText, props.getMaxInputChars());

        return claudeChatClient.prompt()
                .user(safeInput)
                .call()
                .content();
    }

    private static String enforceMaxChars(String text, int maxChars) {
        if (text == null) return "";
        String t = text.trim();
        if (t.length() <= maxChars) return t;
        return t.substring(0, maxChars);
    }
}

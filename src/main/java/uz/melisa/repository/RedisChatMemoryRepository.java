package uz.melisa.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uz.melisa.config.ClaudeProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chatmem:";
    private static final String IDS_KEY = "chatmem:ids";

    private final StringRedisTemplate redis;
    private final ClaudeProperties claudeProperties;
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

    @Override
    public List<String> findConversationIds() {
        Set<String> ids = redis.opsForSet().members(IDS_KEY);
        return (ids == null) ? List.of() : new ArrayList<>(ids);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = key(conversationId);
        List<String> rows = redis.opsForList().range(key, 0, -1);
        if (rows == null || rows.isEmpty()) return List.of();

        List<Message> out = new ArrayList<>(rows.size());
        for (String row : rows) {
            try {
                StoredMsg m = om.readValue(row, StoredMsg.class);
                Message restored = toMessage(m);
                if (restored != null) out.add(restored);
            } catch (Exception ignore) {
            }
        }
        return out;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = key(conversationId);

        redis.delete(key);
        if (messages != null && !messages.isEmpty()) {
            List<String> rows = new ArrayList<>(messages.size());
            for (Message m : messages) {
                if (m instanceof SystemMessage) continue;
                rows.add(serialize(fromMessage(m)));
            }
            if (!rows.isEmpty()) {
                redis.opsForList().rightPushAll(key, rows);
            }
        }

        redis.expire(key, java.time.Duration.ofSeconds(claudeProperties.getMemory().getTtlSeconds()));
        redis.opsForSet().add(IDS_KEY, conversationId);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redis.delete(key(conversationId));
        redis.opsForSet().remove(IDS_KEY, conversationId);
    }

    private String key(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    private String serialize(StoredMsg msg) {
        try {
            return om.writeValueAsString(msg);
        } catch (Exception e) {
            return "{\"t\":\"SKIP\",\"c\":\"\"}";
        }
    }

    private StoredMsg fromMessage(Message m) {
        String type = m.getMessageType().name();
        String content = (m.getText() == null) ? "" : m.getText();
        return new StoredMsg(type, content, Instant.now().toEpochMilli());
    }

    private Message toMessage(StoredMsg m) {
        if (m == null) return null;
        String t = (m.t == null) ? "" : m.t.toUpperCase();

        return switch (t) {
            case "USER" -> new UserMessage(m.c);
            case "ASSISTANT", "AI", "MODEL" -> new AssistantMessage(m.c);
            default -> null;
        };
    }

    private record StoredMsg(String t, String c, long ts) {
    }
}

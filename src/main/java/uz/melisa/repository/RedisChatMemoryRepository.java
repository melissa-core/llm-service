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
import uz.melisa.config.AiProperties;

import java.time.Duration;
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
    private final AiProperties aiProperties;
    private final ObjectMapper om;

    @Override
    public List<String> findConversationIds() {
        Set<String> ids = redis.opsForSet().members(IDS_KEY);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String id : ids) {
            if (redis.hasKey(key(id))) {
                result.add(id);
            } else {
                redis.opsForSet().remove(IDS_KEY, id);
            }
        }
        return result;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = key(conversationId);
        List<String> rows = redis.opsForList().range(key, 0, -1);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<Message> out = new ArrayList<>(rows.size());
        for (String row : rows) {
            try {
                StoredMsg stored = om.readValue(row, StoredMsg.class);
                Message restored = toMessage(stored);
                if (restored != null) {
                    out.add(restored);
                }
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
            for (Message message : messages) {
                if (message instanceof SystemMessage) {
                    continue;
                }
                rows.add(serialize(fromMessage(message)));
            }

            if (!rows.isEmpty()) {
                redis.opsForList().rightPushAll(key, rows);
            }
        }

        redis.expire(key, Duration.ofSeconds(aiProperties.getMemory().getTtlSeconds()));
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

    private StoredMsg fromMessage(Message message) {
        String type = message.getMessageType().name();
        String content = message.getText() == null ? "" : message.getText();
        return new StoredMsg(type, content, Instant.now().toEpochMilli());
    }

    private Message toMessage(StoredMsg stored) {
        if (stored == null) {
            return null;
        }

        String type = stored.t == null ? "" : stored.t.toUpperCase();

        return switch (type) {
            case "USER" -> new UserMessage(stored.c);
            case "ASSISTANT", "AI", "MODEL" -> new AssistantMessage(stored.c);
            default -> null;
        };
    }

    private record StoredMsg(String t, String c, long ts) {
    }
}
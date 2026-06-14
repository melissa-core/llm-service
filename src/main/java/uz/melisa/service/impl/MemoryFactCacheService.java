package uz.melisa.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import uz.melisa.dto.memory.MemoryContext;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Read-through Redis cache of the assembled L1 {@link MemoryContext}, keyed by
 * {@code customer:{customerId}:memory:{generation}:{factVersion}}. Postgres is the source of truth;
 * the cache only ever holds active facts formatted for prompt injection. Every operation is
 * best-effort — Redis errors are logged and swallowed, and the context is always rebuildable from DB.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryFactCacheService {

    private static final Duration TTL = Duration.ofHours(6);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public Optional<MemoryContext> get(Long customerId, long generation, long factVersion) {
        try {
            String json = redis.opsForValue().get(key(customerId, generation, factVersion));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, MemoryContext.class));
        } catch (Exception e) {
            log.warn("memory cache read failed customerId={}", customerId, e);
            return Optional.empty();
        }
    }

    public void put(MemoryContext context) {
        try {
            String key = key(context.customerId(), context.generation(), context.factVersion());
            redis.opsForValue().set(key, objectMapper.writeValueAsString(context), TTL);
        } catch (Exception e) {
            log.warn("memory cache write failed customerId={}", context.customerId(), e);
        }
    }

    /** Best-effort delete of one obsolete key (the prior factVersion) after an L1 change. */
    public void evict(Long customerId, long generation, long factVersion) {
        try {
            redis.delete(key(customerId, generation, factVersion));
        } catch (Exception e) {
            log.warn("memory cache evict failed customerId={}", customerId, e);
        }
    }

    /**
     * Best-effort purge of every cached key for a retired generation. Invoked by the deletion-outbox
     * flow on opt-out / re-consent; the generation bump alone already invalidates reads, this just
     * reclaims the orphaned keys.
     */
    public boolean evictGeneration(Long customerId, long generation) {
        try {
            Set<String> keys = redis.keys("customer:" + customerId + ":memory:" + generation + ":*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
            return true;
        } catch (Exception e) {
            log.warn("memory cache generation purge failed customerId={} generation={}", customerId, generation, e);
            return false;
        }
    }

    private String key(Long customerId, long generation, long factVersion) {
        return "customer:" + customerId + ":memory:" + generation + ":" + factVersion;
    }
}

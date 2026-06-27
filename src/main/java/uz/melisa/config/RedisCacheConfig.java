package uz.melisa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        // Cache name is used as the Redis key prefix (R4): "llm:<name>:<key>".
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("llm:message-product-suggestions", base.entryTtl(Duration.ofHours(1)));
        perCache.put("llm:embedding", base.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    /**
     * Resilient cache error handler: when Redis is unavailable, cache operations must not
     * break business APIs. Get/put/evict/clear failures are logged at WARN and swallowed so
     * the caller transparently falls back to the underlying datasource/service.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new ResilientCacheErrorHandler();
    }

    static class ResilientCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Redis cache GET failed; bypassing cache and reading from source. cache='{}' key='{}' error='{}'",
                    cacheName(cache), key, safeError(exception));
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("Redis cache PUT failed; value not cached. cache='{}' key='{}' error='{}'",
                    cacheName(cache), key, safeError(exception));
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Redis cache EVICT failed; entry not evicted. cache='{}' key='{}' error='{}'",
                    cacheName(cache), key, safeError(exception));
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.warn("Redis cache CLEAR failed; cache not cleared. cache='{}' error='{}'",
                    cacheName(cache), safeError(exception));
        }

        private static String cacheName(Cache cache) {
            return cache != null ? cache.getName() : "<unknown>";
        }

        private static String safeError(RuntimeException exception) {
            return exception != null ? exception.toString() : "<unknown error>";
        }
    }
}

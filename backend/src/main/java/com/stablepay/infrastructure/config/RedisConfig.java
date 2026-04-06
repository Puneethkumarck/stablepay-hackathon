package com.stablepay.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisConfig implements CachingConfigurer {

    private static final Duration FX_RATE_TTL = Duration.ofMinutes(5);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        var fxRateConfig = defaultConfig.entryTtl(FX_RATE_TTL);

        log.info("Configuring Redis cache manager with fxRate TTL: {}", FX_RATE_TTL);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("fxRate", fxRateConfig)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new GracefulCacheErrorHandler();
    }

    private static class GracefulCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache get failed for cache '{}', key '{}': {}", cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("Cache put failed for cache '{}', key '{}': {}", cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.warn(
                    "Cache evict failed for cache '{}', key '{}': {}", cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.warn("Cache clear failed for cache '{}': {}", cache.getName(), exception.getMessage());
        }
    }
}

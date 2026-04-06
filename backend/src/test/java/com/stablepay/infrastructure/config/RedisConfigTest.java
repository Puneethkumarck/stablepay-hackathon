package com.stablepay.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@ExtendWith(MockitoExtension.class)
class RedisConfigTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private Cache cache;

    private final RedisConfig redisConfig = new RedisConfig();

    @Test
    void shouldCreateCacheManagerAsRedisCacheManager() {
        // given
        // no special setup - connectionFactory mock is sufficient

        // when
        var cacheManager = redisConfig.cacheManager(connectionFactory);

        // then
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    void shouldReturnGracefulCacheErrorHandler() {
        // given
        // no special setup needed

        // when
        var errorHandler = redisConfig.errorHandler();

        // then
        assertThat(errorHandler).isNotNull().isInstanceOf(CacheErrorHandler.class);
    }

    @Test
    void shouldHandleCacheGetErrorGracefully() {
        // given
        var errorHandler = redisConfig.errorHandler();
        given(cache.getName()).willReturn("fxRate");
        var exception = new RuntimeException("Redis connection refused");

        // when / then
        assertThatCode(() -> errorHandler.handleCacheGetError(exception, cache, "testKey"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleCachePutErrorGracefully() {
        // given
        var errorHandler = redisConfig.errorHandler();
        given(cache.getName()).willReturn("fxRate");
        var exception = new RuntimeException("Redis connection refused");

        // when / then
        assertThatCode(() -> errorHandler.handleCachePutError(exception, cache, "testKey", "testValue"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleCacheEvictErrorGracefully() {
        // given
        var errorHandler = redisConfig.errorHandler();
        given(cache.getName()).willReturn("fxRate");
        var exception = new RuntimeException("Redis connection refused");

        // when / then
        assertThatCode(() -> errorHandler.handleCacheEvictError(exception, cache, "testKey"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleCacheClearErrorGracefully() {
        // given
        var errorHandler = redisConfig.errorHandler();
        given(cache.getName()).willReturn("fxRate");
        var exception = new RuntimeException("Redis connection refused");

        // when / then
        assertThatCode(() -> errorHandler.handleCacheClearError(exception, cache))
                .doesNotThrowAnyException();
    }
}

package com.ahmed.iptvapp.configuration;

import com.ahmed.iptvapp.cache.CacheService;
import com.ahmed.iptvapp.cache.InMemoryCacheService;
import com.ahmed.iptvapp.cache.ValkeyCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration for selecting the cache implementation based on application properties.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CacheConfig {

    private final InMemoryCacheService inMemoryCacheService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${app.cache.type:memory}")
    private String cacheType;
    
    /**
     * Creates the primary cache service bean based on the configured cache type.
     * 
     * @return The configured CacheService implementation
     */
    @Bean
    @Primary
    public CacheService cacheService() {
        switch (cacheType.toLowerCase()) {
            case "valkey":
            case "redis":
                log.info("Using Valkey/Redis cache implementation");
                return new ValkeyCacheService(redisTemplate, objectMapper());
            case "memory":
            default:
                log.info("Using in-memory cache implementation");
                return inMemoryCacheService;
        }
    }
    
    /**
     * Creates a bean for the ObjectMapper if it doesn't exist.
     * 
     * @return A configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

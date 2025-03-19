package com.ahmed.iptvapp.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Valkey/Redis implementation of the CacheService interface.
 * This implementation uses Redis for distributed caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValkeyCacheService implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final long DEFAULT_TTL_SECONDS = 300; // 5 minutes

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
            log.debug("Stored in Valkey cache: {}", key);
        } catch (Exception e) {
            log.error("Error storing value in Valkey cache: {}", key, e);
        }
    }

    @Override
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            
            // If the value is already of the required type, return it directly
            if (clazz.isInstance(value)) {
                return Optional.of((T) value);
            }
            
            // Otherwise, try to convert it using Jackson
            return Optional.of(objectMapper.convertValue(value, clazz));
        } catch (Exception e) {
            log.error("Error retrieving value from Valkey cache: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void remove(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Removed from Valkey cache: {}", key);
        } catch (Exception e) {
            log.error("Error removing value from Valkey cache: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking existence in Valkey cache: {}", key, e);
            return false;
        }
    }

    @Override
    public void clear() {
        try {
            // Warning: This flushes the entire Redis database
            // In production, you might want to only clear keys with a specific prefix
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            log.info("Cleared entire Valkey cache");
        } catch (Exception e) {
            log.error("Error clearing Valkey cache", e);
        }
    }
}

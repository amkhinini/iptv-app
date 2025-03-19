package com.ahmed.iptvapp.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory cache implementation.
 * This implementation stores cache entries in a ConcurrentHashMap.
 */
@Service
@Slf4j
public class InMemoryCacheService implements CacheService {

    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    
    private static final long DEFAULT_TTL_SECONDS = 300; // 5 minutes
    
    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit timeUnit) {
        long expirationTime = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        cache.put(key, new CacheEntry<>(value, expirationTime));
        log.debug("Stored in memory cache: {} (TTL: {} {})", key, ttl, timeUnit);
    }
    
    @Override
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> clazz) {
        CacheEntry<?> entry = cache.get(key);
        
        if (entry == null) {
            log.trace("Cache miss: {}", key);
            return Optional.empty();
        }
        
        // Check if expired
        if (entry.isExpired()) {
            log.debug("Cache entry expired: {}", key);
            cache.remove(key);
            return Optional.empty();
        }
        
        log.trace("Cache hit: {}", key);
        return Optional.of((T) entry.getValue());
    }
    
    @Override
    public void remove(String key) {
        cache.remove(key);
        log.debug("Removed from memory cache: {}", key);
    }
    
    @Override
    public boolean exists(String key) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        
        if (entry.isExpired()) {
            log.debug("Cache entry expired on exists check: {}", key);
            cache.remove(key);
            return false;
        }
        
        return true;
    }
    
    @Override
    public void clear() {
        int size = cache.size();
        cache.clear();
        log.info("Cleared memory cache ({} entries)", size);
    }
    
    /**
     * Internal class to store cached entries with expiration
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long expirationTime;
        
        public CacheEntry(T value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }
        
        public T getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}

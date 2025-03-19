package com.ahmed.iptvapp.cache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service interface for caching operations.
 * This abstraction will allow us to easily switch between different cache implementations
 * like in-memory caching now and Valkey/Redis in the future.
 */
public interface CacheService {

    /**
     * Store a value in the cache
     * 
     * @param key Cache key
     * @param value Value to store
     * @param ttl Time to live
     * @param timeUnit Time unit for TTL
     * @param <T> Type of value
     */
    <T> void put(String key, T value, long ttl, TimeUnit timeUnit);
    
    /**
     * Store a value in the cache with default TTL
     * 
     * @param key Cache key
     * @param value Value to store
     * @param <T> Type of value
     */
    <T> void put(String key, T value);
    
    /**
     * Get a value from the cache
     * 
     * @param key Cache key
     * @param clazz Class of the stored value
     * @param <T> Type of value
     * @return Optional containing the value if found
     */
    <T> Optional<T> get(String key, Class<T> clazz);
    
    /**
     * Remove a value from the cache
     * 
     * @param key Cache key
     */
    void remove(String key);
    
    /**
     * Check if a key exists in the cache
     * 
     * @param key Cache key
     * @return true if key exists
     */
    boolean exists(String key);
    
    /**
     * Clear all cached entries
     */
    void clear();
}
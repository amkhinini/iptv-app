package com.ahmed.iptvapp.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of the RateLimiter interface.
 * Uses a sliding window algorithm to limit operations within a time window.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiter implements RateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // Default rate limit settings
    private static final int DEFAULT_MAX_REQUESTS = 5;  // Maximum 5 requests
    private static final int DEFAULT_WINDOW_SECONDS = 300;  // Within 5 minutes
    
    // Resource-specific rate limit configurations
    private static final int PLAYLIST_REFRESH_MAX_REQUESTS = 3;  // Maximum 3 refreshes
    private static final int PLAYLIST_REFRESH_WINDOW_SECONDS = 600;  // Within 10 minutes
    
    /**
     * Get the rate limit key for storing in Redis
     */
    private String getRateLimitKey(String key, String resource) {
        return String.format("rate-limit:%s:%s", resource, key);
    }

    @Override
    public boolean allowRequest(String key, String resource) {
        String redisKey = getRateLimitKey(key, resource);
        long now = Instant.now().getEpochSecond();
        int maxRequests = getMaxRequestsForResource(resource);
        int windowSeconds = getWindowSecondsForResource(resource);
        
        try {
            // Remove expired timestamps
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, now - windowSeconds);
            
            // Get current count
            Long currentCount = redisTemplate.opsForZSet().size(redisKey);
            if (currentCount == null) {
                currentCount = 0L;
            }
            
            // Check if the rate limit is exceeded
            if (currentCount >= maxRequests) {
                log.debug("Rate limit exceeded for {}:{}, count={}, limit={}", 
                          resource, key, currentCount, maxRequests);
                return false;
            }
            
            // Add the current timestamp to the sorted set
            redisTemplate.opsForZSet().add(redisKey, now, (double) now);
            
            // Set expiry on the key to auto-cleanup
            redisTemplate.expire(redisKey, windowSeconds * 2, TimeUnit.SECONDS);
            
            return true;
        } catch (Exception e) {
            log.error("Error checking rate limit", e);
            // In case of error, allow the request to avoid blocking legitimate operations
            return true;
        }
    }
    
    @Override
    public long getTimeToNextAllowedRequest(String key, String resource) {
        String redisKey = getRateLimitKey(key, resource);
        long now = Instant.now().getEpochSecond();
        int windowSeconds = getWindowSecondsForResource(resource);
        
        try {
            // Get the oldest timestamp in the window
            Double oldestScore = redisTemplate.opsForZSet().range(redisKey, 0, 0)
                    .stream()
                    .map(ts -> redisTemplate.opsForZSet().score(redisKey, ts))
                    .filter(s -> s != null)
                    .findFirst()
                    .orElse(0.0);
            
            // Calculate when this oldest entry will expire
            long resetTime = oldestScore.longValue() + windowSeconds;
            
            // Return seconds until reset, or 0 if already reset
            return Math.max(0, resetTime - now);
        } catch (Exception e) {
            log.error("Error calculating time to next allowed request", e);
            return 0;
        }
    }
    
    @Override
    public long getCurrentCount(String key, String resource) {
        String redisKey = getRateLimitKey(key, resource);
        long now = Instant.now().getEpochSecond();
        int windowSeconds = getWindowSecondsForResource(resource);
        
        try {
            // Remove expired timestamps
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, now - windowSeconds);
            
            // Get current count
            Long currentCount = redisTemplate.opsForZSet().size(redisKey);
            return currentCount != null ? currentCount : 0;
        } catch (Exception e) {
            log.error("Error getting current count", e);
            return 0;
        }
    }
    
    /**
     * Get maximum requests allowed for a resource
     */
    private int getMaxRequestsForResource(String resource) {
        if ("playlist-refresh".equals(resource)) {
            return PLAYLIST_REFRESH_MAX_REQUESTS;
        }
        return DEFAULT_MAX_REQUESTS;
    }
    
    /**
     * Get time window in seconds for a resource
     */
    private int getWindowSecondsForResource(String resource) {
        if ("playlist-refresh".equals(resource)) {
            return PLAYLIST_REFRESH_WINDOW_SECONDS;
        }
        return DEFAULT_WINDOW_SECONDS;
    }
}

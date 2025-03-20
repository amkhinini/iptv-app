package com.ahmed.iptvapp.cache;

/**
 * Interface for rate limiting operations
 */
public interface RateLimiter {

    /**
     * Check if an operation is allowed to proceed based on rate limits
     *
     * @param key The identifier for the rate limit (e.g., user ID, IP address)
     * @param resource The resource being accessed (e.g., "playlist-refresh")
     * @return true if operation is allowed, false if rate limit is exceeded
     */
    boolean allowRequest(String key, String resource);

    /**
     * Get the time remaining (in seconds) until the next allowed request
     *
     * @param key The identifier for the rate limit
     * @param resource The resource being accessed
     * @return Seconds remaining until the rate limit resets, or 0 if no limit is applied
     */
    long getTimeToNextAllowedRequest(String key, String resource);
    
    /**
     * Get the current count of operations performed against the rate limit
     *
     * @param key The identifier for the rate limit
     * @param resource The resource being accessed
     * @return Current count of operations
     */
    long getCurrentCount(String key, String resource);
}

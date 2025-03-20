package com.ahmed.iptvapp.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO representing the current rate limit status
 */
@Data
@Builder
public class RateLimitStatus {
    private int limit;       // Maximum number of requests allowed
    private int remaining;   // Number of requests remaining
    private long resetSeconds; // Time in seconds until the rate limit resets
}

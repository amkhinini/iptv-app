package com.ahmed.iptvapp.service;

import com.ahmed.iptvapp.cache.CacheService;
import com.ahmed.iptvapp.cache.RateLimiter;
import com.ahmed.iptvapp.dto.PlaylistDto;
import com.ahmed.iptvapp.dto.PlaylistImportResponse;
import com.ahmed.iptvapp.dto.RateLimitStatus;
import com.ahmed.iptvapp.exception.RateLimitExceededException;
import com.ahmed.iptvapp.model.Channel;
import com.ahmed.iptvapp.model.Movie;
import com.ahmed.iptvapp.model.Playlist;
import com.ahmed.iptvapp.model.Series;
import com.ahmed.iptvapp.repository.ChannelRepository;
import com.ahmed.iptvapp.repository.MovieRepository;
import com.ahmed.iptvapp.repository.PlaylistRepository;
import com.ahmed.iptvapp.repository.SeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final ChannelRepository channelRepository;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;
    private final M3UParserService m3uParserService;
    private final CacheService cacheService;
    private final AsyncTaskTrackerService taskTrackerService;
    private final RateLimiter rateLimiter;
    
    // Rate limit resource identifier
    private static final String REFRESH_RATE_LIMIT_RESOURCE = "playlist-refresh";
    
    // Cache key patterns
    private static final String USER_PLAYLISTS_CACHE_KEY = "user:%s:playlists";
    private static final String PLAYLIST_CACHE_KEY = "playlist:%s";
    private static final String PLAYLIST_CHANNELS_COUNT_CACHE_KEY = "playlist:%s:channels:count";
    private static final String PLAYLIST_MOVIES_COUNT_CACHE_KEY = "playlist:%s:movies:count";
    private static final String PLAYLIST_SERIES_COUNT_CACHE_KEY = "playlist:%s:series:count";

    /**
     * Get all playlists for a user
     */
    public List<PlaylistDto> getUserPlaylists(String userId) {
        String cacheKey = String.format(USER_PLAYLISTS_CACHE_KEY, userId);
        
        // Try to get from cache first
        Optional<List> cachedPlaylists = cacheService.get(cacheKey, List.class);
        if (cachedPlaylists.isPresent()) {
            log.debug("Retrieved user playlists from cache for user: {}", userId);
            return (List<PlaylistDto>) cachedPlaylists.get();
        }
        
        // Not in cache, get from database
        List<PlaylistDto> playlists = playlistRepository.findByUserId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        // Store in cache
        cacheService.put(cacheKey, playlists, 5, TimeUnit.MINUTES);
        
        return playlists;
    }

    /**
     * Get a specific playlist by ID
     */
    public Optional<PlaylistDto> getPlaylist(String playlistId, String userId) {
        String cacheKey = String.format(PLAYLIST_CACHE_KEY, playlistId);
        
        // Try to get from cache first
        Optional<PlaylistDto> cachedPlaylist = cacheService.get(cacheKey, PlaylistDto.class);
        if (cachedPlaylist.isPresent()) {
            // Verify this user can access this playlist
            if (cachedPlaylist.get().getUserId().equals(userId)) {
                log.debug("Retrieved playlist from cache: {}", playlistId);
                return cachedPlaylist;
            }
        }
        
        // Not in cache or not authorized, get from database
        return playlistRepository.findById(playlistId)
                .filter(playlist -> playlist.getUserId().equals(userId))
                .map(playlist -> {
                    PlaylistDto dto = convertToDto(playlist);
                    // Store in cache
                    cacheService.put(cacheKey, dto, 10, TimeUnit.MINUTES);
                    return dto;
                });
    }

    /**
     * Create a new playlist from URL - Synchronous version
     */
    @Transactional
    public PlaylistDto createFromUrl(String url, String userId) {
        try {
            Playlist playlist = m3uParserService.parseFromUrl(url, userId);
            PlaylistDto playlistDto = savePlaylistWithContent(playlist);
            
            // Invalidate user playlists cache
            cacheService.remove(String.format(USER_PLAYLISTS_CACHE_KEY, userId));
            
            return playlistDto;
        } catch (IOException e) {
            log.error("Error creating playlist from URL: {}", url, e);
            throw new RuntimeException("Failed to load playlist from URL: " + e.getMessage());
        }
    }
    
    /**
     * Create a new playlist from URL - Asynchronous version
     */
    @Async("taskExecutor")
    public CompletableFuture<PlaylistImportResponse> createFromUrlAsync(String url, String userId, String taskId) {
        try {
            // Update status to processing
            taskTrackerService.updateTaskStatus(taskId, PlaylistImportResponse.processing(taskId, 10));
            
            // Parse the playlist
            Playlist playlist = m3uParserService.parseFromUrl(url, userId);
            
            // Update progress
            taskTrackerService.updateTaskStatus(taskId, PlaylistImportResponse.processing(taskId, 50));
            
            // Save the playlist with content
            PlaylistDto playlistDto = savePlaylistWithContent(playlist);
            
            // Invalidate user playlists cache
            cacheService.remove(String.format(USER_PLAYLISTS_CACHE_KEY, userId));
            
            // Create completed response
            PlaylistImportResponse response = PlaylistImportResponse.completed(taskId, playlistDto.getId(), playlistDto);
            
            // Update status to completed
            taskTrackerService.updateTaskStatus(taskId, response);
            
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Error creating playlist from URL: {}", url, e);
            PlaylistImportResponse failedResponse = PlaylistImportResponse.failed(taskId, e.getMessage());
            taskTrackerService.updateTaskStatus(taskId, failedResponse);
            return CompletableFuture.completedFuture(failedResponse);
        }
    }

    /**
     * Update an existing playlist
     */
    public PlaylistDto updatePlaylist(String playlistId, PlaylistDto dto, String userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        playlist.setName(dto.getName());
        playlist.setActive(dto.getActive());
        playlist.setUpdatedAt(LocalDateTime.now());
        
        if (dto.getUrl() != null && !dto.getUrl().equals(playlist.getUrl())) {
            playlist.setUrl(dto.getUrl());
            try {
                // Re-parse the playlist if URL is changed
                refreshPlaylist(playlistId, userId);
                return getPlaylist(playlistId, userId).get();
            } catch (Exception e) {
                log.error("Error updating playlist URL", e);
                throw new RuntimeException("Failed to load content from new URL: " + e.getMessage());
            }
        }
        
        Playlist savedPlaylist = playlistRepository.save(playlist);
        PlaylistDto updatedDto = convertToDto(savedPlaylist);
        
        // Update cache
        cacheService.put(String.format(PLAYLIST_CACHE_KEY, playlistId), updatedDto);
        // Invalidate user playlists cache
        cacheService.remove(String.format(USER_PLAYLISTS_CACHE_KEY, userId));
        
        return updatedDto;
    }

    /**
     * Refresh playlist content from its URL - Synchronous version
     * Now with rate limiting
     */
    @Transactional
    public PlaylistDto refreshPlaylist(String playlistId, String userId) {
        // Check rate limit before proceeding
        if (!rateLimiter.allowRequest(userId, REFRESH_RATE_LIMIT_RESOURCE)) {
            long retryAfter = rateLimiter.getTimeToNextAllowedRequest(userId, REFRESH_RATE_LIMIT_RESOURCE);
            throw new RateLimitExceededException(
                    "Rate limit exceeded for playlist refresh. Please try again later.", 
                    retryAfter);
        }
        
        Playlist playlist = playlistRepository.findById(playlistId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (playlist.getUrl() == null || playlist.getUrl().isEmpty()) {
            throw new RuntimeException("Cannot refresh a playlist without URL");
        }

        try {
            // Delete existing content
            channelRepository.deleteAll(channelRepository.findByPlaylistId(playlistId));
            movieRepository.deleteAll(movieRepository.findByPlaylistId(playlistId));
            seriesRepository.deleteAll(seriesRepository.findByPlaylistId(playlistId));

            // Re-parse from URL
            Playlist refreshedPlaylist = m3uParserService.parseFromUrl(playlist.getUrl(), userId);
            refreshedPlaylist.setId(playlistId);
            refreshedPlaylist.setName(playlist.getName());
            refreshedPlaylist.setCreatedAt(playlist.getCreatedAt());
            refreshedPlaylist.setLastRefreshed(LocalDateTime.now());
            
            PlaylistDto refreshedDto = savePlaylistWithContent(refreshedPlaylist);
            
            // Clear caches related to this playlist
            invalidatePlaylistCaches(playlistId, userId);
            
            return refreshedDto;
        } catch (IOException e) {
            log.error("Error refreshing playlist", e);
            throw new RuntimeException("Failed to refresh playlist: " + e.getMessage());
        }
    }
    
    /**
     * Refresh playlist content from its URL - Asynchronous version
     * Now with rate limiting
     */
    @Async("taskExecutor")
    public CompletableFuture<PlaylistImportResponse> refreshPlaylistAsync(String playlistId, String userId, String taskId) {
        try {
            // Check rate limit before proceeding
            if (!rateLimiter.allowRequest(userId, REFRESH_RATE_LIMIT_RESOURCE)) {
                long retryAfter = rateLimiter.getTimeToNextAllowedRequest(userId, REFRESH_RATE_LIMIT_RESOURCE);
                PlaylistImportResponse rateLimitExceededResponse = PlaylistImportResponse.builder()
                        .taskId(taskId)
                        .status("RATE_LIMITED")
                        .progress(0)
                        .startTime(LocalDateTime.now())
                        .endTime(LocalDateTime.now())
                        .errorMessage("Rate limit exceeded for playlist refresh. Please try again after " + 
                                      retryAfter + " seconds.")
                        .build();
                
                taskTrackerService.updateTaskStatus(taskId, rateLimitExceededResponse);
                return CompletableFuture.completedFuture(rateLimitExceededResponse);
            }
            
            Playlist playlist = playlistRepository.findById(playlistId)
                    .filter(p -> p.getUserId().equals(userId))
                    .orElseThrow(() -> new RuntimeException("Playlist not found"));

            if (playlist.getUrl() == null || playlist.getUrl().isEmpty()) {
                throw new RuntimeException("Cannot refresh a playlist without URL");
            }
            
            // Update status to processing
            taskTrackerService.updateTaskStatus(taskId, PlaylistImportResponse.processing(taskId, 10));
            
            // Delete existing content
            channelRepository.deleteAll(channelRepository.findByPlaylistId(playlistId));
            movieRepository.deleteAll(movieRepository.findByPlaylistId(playlistId));
            seriesRepository.deleteAll(seriesRepository.findByPlaylistId(playlistId));
            
            // Update progress
            taskTrackerService.updateTaskStatus(taskId, PlaylistImportResponse.processing(taskId, 30));

            // Re-parse from URL
            Playlist refreshedPlaylist = m3uParserService.parseFromUrl(playlist.getUrl(), userId);
            refreshedPlaylist.setId(playlistId);
            refreshedPlaylist.setName(playlist.getName());
            refreshedPlaylist.setCreatedAt(playlist.getCreatedAt());
            refreshedPlaylist.setLastRefreshed(LocalDateTime.now());
            
            // Update progress
            taskTrackerService.updateTaskStatus(taskId, PlaylistImportResponse.processing(taskId, 70));
            
            PlaylistDto refreshedDto = savePlaylistWithContent(refreshedPlaylist);
            
            // Clear caches related to this playlist
            invalidatePlaylistCaches(playlistId, userId);
            
            // Create completed response
            PlaylistImportResponse response = PlaylistImportResponse.completed(taskId, playlistId, refreshedDto);
            
            // Update status to completed
            taskTrackerService.updateTaskStatus(taskId, response);
            
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Error refreshing playlist", e);
            PlaylistImportResponse failedResponse = PlaylistImportResponse.failed(taskId, e.getMessage());
            taskTrackerService.updateTaskStatus(taskId, failedResponse);
            return CompletableFuture.completedFuture(failedResponse);
        }
    }

    /**
     * Delete a playlist and all its content
     */
    @Transactional
    public void deletePlaylist(String playlistId, String userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        // Delete all related content
        channelRepository.deleteAll(channelRepository.findByPlaylistId(playlistId));
        movieRepository.deleteAll(movieRepository.findByPlaylistId(playlistId));
        seriesRepository.deleteAll(seriesRepository.findByPlaylistId(playlistId));
        
        // Delete the playlist
        playlistRepository.delete(playlist);
        
        // Clear caches related to this playlist
        invalidatePlaylistCaches(playlistId, userId);
    }

    /**
     * Save playlist and its related content
     */
    private PlaylistDto savePlaylistWithContent(Playlist playlist) {
        // First save the playlist to get an ID
        Playlist savedPlaylist = playlistRepository.save(playlist);
        String playlistId = savedPlaylist.getId();
        
        // Set the playlist ID for all content
        List<Channel> channels = savedPlaylist.getChannels();
        List<Movie> movies = savedPlaylist.getMovies();
        List<Series> series = savedPlaylist.getSeries();
        
        // Set the playlist ID
        channels.forEach(channel -> channel.setPlaylistId(playlistId));
        movies.forEach(movie -> movie.setPlaylistId(playlistId));
        series.forEach(s -> s.setPlaylistId(playlistId));
        
        // Save the content
        if (!channels.isEmpty()) {
            channelRepository.saveAll(channels);
        }
        
        if (!movies.isEmpty()) {
            movieRepository.saveAll(movies);
        }
        
        if (!series.isEmpty()) {
            seriesRepository.saveAll(series);
        }
        
        PlaylistDto dto = convertToDto(savedPlaylist);
        
        // Cache the playlist
        cacheService.put(String.format(PLAYLIST_CACHE_KEY, playlistId), dto);
        
        return dto;
    }

    /**
     * Convert Playlist entity to DTO
     */
    private PlaylistDto convertToDto(Playlist playlist) {
        String playlistId = playlist.getId();
        
        // Try to get counts from cache
        int channelsCount = getCachedOrComputeCount(
            String.format(PLAYLIST_CHANNELS_COUNT_CACHE_KEY, playlistId),
            () -> playlist.getChannels() != null ? playlist.getChannels().size() : 
                channelRepository.findByPlaylistId(playlistId).size()
        );
        
        int moviesCount = getCachedOrComputeCount(
            String.format(PLAYLIST_MOVIES_COUNT_CACHE_KEY, playlistId),
            () -> playlist.getMovies() != null ? playlist.getMovies().size() : 
                movieRepository.findByPlaylistId(playlistId).size()
        );
        
        int seriesCount = getCachedOrComputeCount(
            String.format(PLAYLIST_SERIES_COUNT_CACHE_KEY, playlistId),
            () -> playlist.getSeries() != null ? playlist.getSeries().size() : 
                seriesRepository.findByPlaylistId(playlistId).size()
        );
        
        return PlaylistDto.builder()
                .id(playlistId)
                .name(playlist.getName())
                .url(playlist.getUrl())
                .userId(playlist.getUserId())
                .createdAt(playlist.getCreatedAt())
                .updatedAt(playlist.getUpdatedAt())
                .lastRefreshed(playlist.getLastRefreshed())
                .active(playlist.getActive())
                .channelsCount(channelsCount)
                .moviesCount(moviesCount)
                .seriesCount(seriesCount)
                .build();
    }
    
    /**
     * Helper method to get a count from cache or compute it
     */
    private int getCachedOrComputeCount(String cacheKey, Callable<Integer> countSupplier) {
        return cacheService.get(cacheKey, Integer.class)
                .orElseGet(() -> {
                    try {
                        int count = countSupplier.call();
                        cacheService.put(cacheKey, count, 30, TimeUnit.MINUTES);
                        return count;
                    } catch (Exception e) {
                        log.error("Error computing count", e);
                        return 0;
                    }
                });
    }
    
    /**
     * Invalidate all caches related to a playlist
     */
    private void invalidatePlaylistCaches(String playlistId, String userId) {
        cacheService.remove(String.format(PLAYLIST_CACHE_KEY, playlistId));
        cacheService.remove(String.format(PLAYLIST_CHANNELS_COUNT_CACHE_KEY, playlistId));
        cacheService.remove(String.format(PLAYLIST_MOVIES_COUNT_CACHE_KEY, playlistId));
        cacheService.remove(String.format(PLAYLIST_SERIES_COUNT_CACHE_KEY, playlistId));
        cacheService.remove(String.format(USER_PLAYLISTS_CACHE_KEY, userId));
    }
    
    /**
     * Get the current rate limit status for a user
     * 
     * @param userId The user ID
     * @return RateLimitStatus with rate limit information (limit, remaining, reset)
     */
    public RateLimitStatus getRateLimitStatus(String userId) {
        long currentCount = rateLimiter.getCurrentCount(userId, REFRESH_RATE_LIMIT_RESOURCE);
        long timeToReset = rateLimiter.getTimeToNextAllowedRequest(userId, REFRESH_RATE_LIMIT_RESOURCE);
        int maxRequests = 3; // This should match the PLAYLIST_REFRESH_MAX_REQUESTS in RedisRateLimiter
        
        return RateLimitStatus.builder()
                .limit(maxRequests)
                .remaining((int) Math.max(0, maxRequests - currentCount))
                .resetSeconds(timeToReset)
                .build();
    }

    /**
     * Get the status of a playlist import task
     */
    public Optional<PlaylistImportResponse> getTaskStatus(String taskId) {
        return taskTrackerService.getTaskStatus(taskId);
    }
    
    /**
     * Functional interface for computing counts
     */
    @FunctionalInterface
    private interface Callable<T> {
        T call() throws Exception;
    }
}

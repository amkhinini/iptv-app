package com.ahmed.iptvapp.service;

import com.ahmed.iptvapp.dto.PageResponse;
import com.ahmed.iptvapp.model.Channel;
import com.ahmed.iptvapp.repository.ChannelRepository;
import com.ahmed.iptvapp.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {
    
    private final ChannelRepository channelRepository;
    private final PlaylistRepository playlistRepository;
    private final MongoTemplate mongoTemplate;
    
    // Cache key patterns for pagination
    private static final String CHANNEL_LIST_CACHE_KEY = "playlist:%s:channels:page:%d:size:%d";
    private static final String CHANNEL_GROUP_LIST_CACHE_KEY = "playlist:%s:group:%s:channels:page:%d:size:%d";
    private static final String CHANNEL_FAVORITES_CACHE_KEY = "playlist:%s:favorites:page:%d:size:%d";
    private static final String CHANNEL_COUNT_CACHE_KEY = "playlist:%s:channels:count";
    private static final String CHANNEL_GROUP_COUNT_CACHE_KEY = "playlist:%s:group:%s:channels:count";
    private static final String CHANNEL_FAVORITES_COUNT_CACHE_KEY = "playlist:%s:favorites:count";
    
    /**
     * Get paginated channels for a playlist
     */
    public PageResponse<Channel> getChannelsByPlaylistPaginated(String playlistId, String userId, int page, int size) {
        // Verify the user owns the playlist
        verifyPlaylistAccess(playlistId, userId);
        
        // Get from database with pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        
        Query query = new Query()
                .addCriteria(Criteria.where("playlistId").is(playlistId))
                .with(pageable);
        
        List<Channel> channels = mongoTemplate.find(query, Channel.class);
        long total = getChannelCount(playlistId);
        
        return PageResponse.of(channels, page, size, total);
    }
    
    /**
     * Legacy method for backward compatibility
     * Get all channels for a playlist
     */
    public List<Channel> getChannelsByPlaylist(String playlistId, String userId) {
        PageResponse<Channel> response = getChannelsByPlaylistPaginated(playlistId, userId, 0, Integer.MAX_VALUE);
        return response.getContent();
    }
    
    /**
     * Get paginated channels by group
     */
    public PageResponse<Channel> getChannelsByGroupPaginated(String playlistId, String group, String userId, int page, int size) {
        // Verify the user owns the playlist
        verifyPlaylistAccess(playlistId, userId);
        
        // Get from database with pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        
        Query query = new Query()
                .addCriteria(Criteria.where("playlistId").is(playlistId))
                .addCriteria(Criteria.where("group").is(group))
                .with(pageable);
        
        List<Channel> channels = mongoTemplate.find(query, Channel.class);
        long total = getChannelCountByGroup(playlistId, group);
        
        return PageResponse.of(channels, page, size, total);
    }
    
    /**
     * Legacy method for backward compatibility
     * Get channels by group
     */
    public List<Channel> getChannelsByGroup(String playlistId, String group, String userId) {
        PageResponse<Channel> response = getChannelsByGroupPaginated(playlistId, group, userId, 0, Integer.MAX_VALUE);
        return response.getContent();
    }
    
    /**
     * Get a specific channel
     */
    public Optional<Channel> getChannel(String channelId, String userId) {
        Optional<Channel> channelOpt = channelRepository.findById(channelId);
        
        return channelOpt.flatMap(channel -> {
            // Verify the user owns the playlist the channel belongs to
            return playlistRepository.findById(channel.getPlaylistId())
                    .filter(playlist -> playlist.getUserId().equals(userId))
                    .map(playlist -> channel);
        });
    }
    
    /**
     * Toggle favorite status
     */
    public Channel toggleFavorite(String channelId, String userId) {
        Channel channel = getChannel(channelId, userId)
                .orElseThrow(() -> new RuntimeException("Channel not found or access denied"));
        
        channel.setFavorite(!channel.getFavorite());
        Channel updatedChannel = channelRepository.save(channel);
        
        // Invalidate relevant caches
        invalidateChannelCaches(channel.getPlaylistId());
        
        return updatedChannel;
    }
    
    /**
     * Get paginated favorite channels
     */
    public PageResponse<Channel> getFavoritesPaginated(String playlistId, String userId, int page, int size) {
        // Verify the user owns the playlist
        verifyPlaylistAccess(playlistId, userId);
        
        // Get from database with pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        
        Query query = new Query()
                .addCriteria(Criteria.where("playlistId").is(playlistId))
                .addCriteria(Criteria.where("favorite").is(true))
                .with(pageable);
        
        List<Channel> channels = mongoTemplate.find(query, Channel.class);
        long total = getFavoriteChannelCount(playlistId);
        
        return PageResponse.of(channels, page, size, total);
    }
    
    /**
     * Legacy method for backward compatibility
     * Get all favorite channels
     */
    public List<Channel> getFavorites(String playlistId, String userId) {
        PageResponse<Channel> response = getFavoritesPaginated(playlistId, userId, 0, Integer.MAX_VALUE);
        return response.getContent();
    }
    
    /**
     * Get all available channel groups
     */
    public List<String> getChannelGroups(String playlistId, String userId) {
        // Verify the user owns the playlist
        verifyPlaylistAccess(playlistId, userId);
        
        return channelRepository.findByPlaylistId(playlistId).stream()
                .map(Channel::getGroup)
                .distinct()
                .sorted()
                .toList();
    }
    
    /**
     * Helper method to verify playlist access
     */
    private void verifyPlaylistAccess(String playlistId, String userId) {
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
    }
    
    /**
     * Get channel count for a playlist
     */
    private long getChannelCount(String playlistId) {
        Query query = new Query().addCriteria(Criteria.where("playlistId").is(playlistId));
        return mongoTemplate.count(query, Channel.class);
    }
    
    /**
     * Get channel count for a playlist and group
     */
    private long getChannelCountByGroup(String playlistId, String group) {
        Query query = new Query()
                .addCriteria(Criteria.where("playlistId").is(playlistId))
                .addCriteria(Criteria.where("group").is(group));
        return mongoTemplate.count(query, Channel.class);
    }
    
    /**
     * Get favorite channel count for a playlist
     */
    private long getFavoriteChannelCount(String playlistId) {
        Query query = new Query()
                .addCriteria(Criteria.where("playlistId").is(playlistId))
                .addCriteria(Criteria.where("favorite").is(true));
        return mongoTemplate.count(query, Channel.class);
    }
    
    /**
     * Invalidate channel-related caches for a playlist
     */
    private void invalidateChannelCaches(String playlistId) {
        // This is a placeholder for cache invalidation
        // In a real implementation, you would clear relevant cache entries
        log.debug("Invalidating channel caches for playlist: {}", playlistId);
    }
}

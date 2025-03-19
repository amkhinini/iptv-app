package com.ahmed.iptvapp.service;

import com.ahmed.iptvapp.model.Channel;
import com.ahmed.iptvapp.repository.ChannelRepository;
import com.ahmed.iptvapp.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {
    
    private final ChannelRepository channelRepository;
    private final PlaylistRepository playlistRepository;
    
    /**
     * Get all channels for a playlist
     */
    public List<Channel> getChannelsByPlaylist(String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return channelRepository.findByPlaylistId(playlistId);
    }
    
    /**
     * Get channels by group
     */
    public List<Channel> getChannelsByGroup(String playlistId, String group, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return channelRepository.findByPlaylistIdAndGroup(playlistId, group);
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
        return channelRepository.save(channel);
    }
    
    /**
     * Get all favorite channels
     */
    public List<Channel> getFavorites(String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return channelRepository.findByPlaylistIdAndFavorite(playlistId, true);
    }
    
    /**
     * Get all available channel groups
     */
    public List<String> getChannelGroups(String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return channelRepository.findByPlaylistId(playlistId).stream()
                .map(Channel::getGroup)
                .distinct()
                .sorted()
                .toList();
    }
}

package com.ahmed.iptvapp.controller;

import com.ahmed.iptvapp.configuration.PaginationConfig;
import com.ahmed.iptvapp.dto.PageResponse;
import com.ahmed.iptvapp.model.Channel;
import com.ahmed.iptvapp.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists/{playlistId}/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final PaginationConfig paginationConfig;
    
    @GetMapping
    public ResponseEntity<PageResponse<Channel>> getChannels(
            @PathVariable String playlistId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        int validPage = page == null || page < 0 ? 0 : page;
        int validSize = paginationConfig.validatePageSize(size, paginationConfig.getChannelsPageSize());
        
        return ResponseEntity.ok(channelService.getChannelsByPlaylistPaginated(playlistId, userId, validPage, validSize));
    }
    
    @GetMapping("/groups")
    public ResponseEntity<List<String>> getChannelGroups(@PathVariable String playlistId,
                                                      Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(channelService.getChannelGroups(playlistId, userId));
    }
    
    @GetMapping("/group/{group}")
    public ResponseEntity<PageResponse<Channel>> getChannelsByGroup(
            @PathVariable String playlistId,
            @PathVariable String group,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        int validPage = page == null || page < 0 ? 0 : page;
        int validSize = paginationConfig.validatePageSize(size, paginationConfig.getChannelsPageSize());
        
        return ResponseEntity.ok(channelService.getChannelsByGroupPaginated(playlistId, group, userId, validPage, validSize));
    }
    
    @GetMapping("/favorites")
    public ResponseEntity<PageResponse<Channel>> getFavoriteChannels(
            @PathVariable String playlistId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        int validPage = page == null || page < 0 ? 0 : page;
        int validSize = paginationConfig.validatePageSize(size, paginationConfig.getChannelsPageSize());
        
        return ResponseEntity.ok(channelService.getFavoritesPaginated(playlistId, userId, validPage, validSize));
    }
    
    @GetMapping("/{channelId}")
    public ResponseEntity<Channel> getChannel(@PathVariable String channelId,
                                            Authentication authentication) {
        String userId = authentication.getName();
        return channelService.getChannel(channelId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{channelId}/favorite")
    public ResponseEntity<Channel> toggleFavorite(@PathVariable String channelId,
                                               Authentication authentication) {
        String userId = authentication.getName();
        Channel updatedChannel = channelService.toggleFavorite(channelId, userId);
        return ResponseEntity.ok(updatedChannel);
    }
    
    @GetMapping("/{channelId}/stream")
    public ResponseEntity<String> getStreamUrl(@PathVariable String channelId,
                                             Authentication authentication) {
        String userId = authentication.getName();
        return channelService.getChannel(channelId, userId)
                .map(channel -> ResponseEntity.ok(channel.getStreamUrl()))
                .orElse(ResponseEntity.notFound().build());
    }
}

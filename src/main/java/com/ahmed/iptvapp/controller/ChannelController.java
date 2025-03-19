package com.ahmed.iptvapp.controller;

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
    
    @GetMapping
    public ResponseEntity<List<Channel>> getChannels(@PathVariable String playlistId,
                                                   Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(channelService.getChannelsByPlaylist(playlistId, userId));
    }
    
    @GetMapping("/groups")
    public ResponseEntity<List<String>> getChannelGroups(@PathVariable String playlistId,
                                                      Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(channelService.getChannelGroups(playlistId, userId));
    }
    
    @GetMapping("/group/{group}")
    public ResponseEntity<List<Channel>> getChannelsByGroup(@PathVariable String playlistId,
                                                         @PathVariable String group,
                                                         Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(channelService.getChannelsByGroup(playlistId, group, userId));
    }
    
    @GetMapping("/favorites")
    public ResponseEntity<List<Channel>> getFavoriteChannels(@PathVariable String playlistId,
                                                          Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(channelService.getFavorites(playlistId, userId));
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
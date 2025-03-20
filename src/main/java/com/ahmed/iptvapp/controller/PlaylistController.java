package com.ahmed.iptvapp.controller;

import com.ahmed.iptvapp.dto.PlaylistDto;
import com.ahmed.iptvapp.dto.PlaylistImportResponse;
import com.ahmed.iptvapp.service.AsyncTaskTrackerService;
import com.ahmed.iptvapp.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;
    private final AsyncTaskTrackerService taskTrackerService;
    
    @GetMapping
    public ResponseEntity<List<PlaylistDto>> getUserPlaylists(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(playlistService.getUserPlaylists(userId));
    }
    
    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> getPlaylist(@PathVariable String playlistId, Authentication authentication) {
        String userId = authentication.getName();
        return playlistService.getPlaylist(playlistId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Synchronous playlist creation from URL
     */
    @PostMapping("/url")
    public ResponseEntity<PlaylistDto> createFromUrl(@RequestParam String url, Authentication authentication) {
        String userId = authentication.getName();
        PlaylistDto createdPlaylist = playlistService.createFromUrl(url, userId);
        return new ResponseEntity<>(createdPlaylist, HttpStatus.CREATED);
    }
    
    /**
     * Asynchronous playlist creation from URL
     */
    @PostMapping("/async")
    public ResponseEntity<PlaylistImportResponse> createFromUrlAsync(@RequestParam String url, Authentication authentication) {
        String userId = authentication.getName();
        String taskId = taskTrackerService.createTask();
        
        // Start async task
        playlistService.createFromUrlAsync(url, userId, taskId);
        
        // Return initial response with task ID
        PlaylistImportResponse response = PlaylistImportResponse.pending(taskId);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }
    
    /**
     * Get status of an asynchronous playlist import task
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<PlaylistImportResponse> getTaskStatus(@PathVariable String taskId) {
        return playlistService.getTaskStatus(taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> updatePlaylist(@PathVariable String playlistId,
                                                    @RequestBody PlaylistDto playlistDto,
                                                    Authentication authentication) {
        String userId = authentication.getName();
        PlaylistDto updatedPlaylist = playlistService.updatePlaylist(playlistId, playlistDto, userId);
        return ResponseEntity.ok(updatedPlaylist);
    }
    
    /**
     * Synchronous playlist refresh
     */
    @PostMapping("/{playlistId}/refresh")
    public ResponseEntity<PlaylistDto> refreshPlaylist(@PathVariable String playlistId, 
                                                     Authentication authentication) {
        String userId = authentication.getName();
        PlaylistDto refreshedPlaylist = playlistService.refreshPlaylist(playlistId, userId);
        return ResponseEntity.ok(refreshedPlaylist);
    }
    
    /**
     * Asynchronous playlist refresh
     */
    @PostMapping("/{playlistId}/refresh/async")
    public ResponseEntity<PlaylistImportResponse> refreshPlaylistAsync(@PathVariable String playlistId, 
                                                                     Authentication authentication) {
        String userId = authentication.getName();
        String taskId = taskTrackerService.createTask();
        
        // Start async task
        playlistService.refreshPlaylistAsync(playlistId, userId, taskId);
        
        // Return initial response with task ID
        PlaylistImportResponse response = PlaylistImportResponse.pending(taskId);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }
    
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable String playlistId, 
                                             Authentication authentication) {
        String userId = authentication.getName();
        playlistService.deletePlaylist(playlistId, userId);
        return ResponseEntity.noContent().build();
    }
}

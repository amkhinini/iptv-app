package com.ahmed.iptvapp.controller;

import com.ahmed.iptvapp.dto.PlaylistDto;
import com.ahmed.iptvapp.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;
    
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
    
    @PostMapping("/url")
    public ResponseEntity<PlaylistDto> createFromUrl(@RequestParam String url, Authentication authentication) {
        String userId = authentication.getName();
        PlaylistDto createdPlaylist = playlistService.createFromUrl(url, userId);
        return new ResponseEntity<>(createdPlaylist, HttpStatus.CREATED);
    }
    
    @PostMapping("/file")
    public ResponseEntity<PlaylistDto> createFromFile(@RequestParam("file") MultipartFile file, 
                                                     Authentication authentication) {
        String userId = authentication.getName();
        PlaylistDto createdPlaylist = playlistService.createFromFile(file, userId);
        return new ResponseEntity<>(createdPlaylist, HttpStatus.CREATED);
    }
    
    @PutMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> updatePlaylist(@PathVariable String playlistId,
                                                    @RequestBody PlaylistDto playlistDto,
                                                    Authentication authentication) {
        String userId = authentication.getName();
        PlaylistDto updatedPlaylist = playlistService.updatePlaylist(playlistId, playlistDto, userId);
        return ResponseEntity.ok(updatedPlaylist);
    }
    
    @PostMapping("/{playlistId}/refresh")
    public ResponseEntity<PlaylistDto> refreshPlaylist(@PathVariable String playlistId, 
                                                     Authentication authentication) {
        String userId = authentication.getName();
        PlaylistDto refreshedPlaylist = playlistService.refreshPlaylist(playlistId, userId);
        return ResponseEntity.ok(refreshedPlaylist);
    }
    
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable String playlistId, 
                                             Authentication authentication) {
        String userId = authentication.getName();
        playlistService.deletePlaylist(playlistId, userId);
        return ResponseEntity.noContent().build();
    }
}

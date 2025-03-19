package com.ahmed.iptvapp.controller;

import com.ahmed.iptvapp.model.Episode;
import com.ahmed.iptvapp.model.Series;
import com.ahmed.iptvapp.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists/{playlistId}/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;
    
    @GetMapping
    public ResponseEntity<List<Series>> getAllSeries(@PathVariable String playlistId,
                                                   Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(seriesService.getSeriesByPlaylist(playlistId, userId));
    }
    
    @GetMapping("/genres")
    public ResponseEntity<List<String>> getSeriesGenres(@PathVariable String playlistId,
                                                      Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(seriesService.getSeriesGenres(playlistId, userId));
    }
    
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<Series>> getSeriesByGenre(@PathVariable String playlistId,
                                                       @PathVariable String genre,
                                                       Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(seriesService.getSeriesByGenre(playlistId, genre, userId));
    }
    
    @GetMapping("/favorites")
    public ResponseEntity<List<Series>> getFavoriteSeries(@PathVariable String playlistId,
                                                        Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(seriesService.getFavorites(playlistId, userId));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Series>> searchSeries(@PathVariable String playlistId,
                                                   @RequestParam String query,
                                                   Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(seriesService.searchSeries(query, playlistId, userId));
    }
    
    @GetMapping("/{seriesId}")
    public ResponseEntity<Series> getSeries(@PathVariable String seriesId,
                                          Authentication authentication) {
        String userId = authentication.getName();
        return seriesService.getSeries(seriesId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{seriesId}/favorite")
    public ResponseEntity<Series> toggleFavorite(@PathVariable String seriesId,
                                               Authentication authentication) {
        String userId = authentication.getName();
        Series updatedSeries = seriesService.toggleFavorite(seriesId, userId);
        return ResponseEntity.ok(updatedSeries);
    }
    
    @GetMapping("/{seriesId}/seasons")
    public ResponseEntity<List<Integer>> getAvailableSeasons(@PathVariable String seriesId,
                                                          Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(seriesService.getAvailableSeasons(seriesId, userId));
    }
    
    @GetMapping("/{seriesId}/episodes")
    public ResponseEntity<List<Episode>> getAllEpisodes(@PathVariable String seriesId,
                                                      Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(seriesService.getEpisodes(seriesId, userId));
    }
    
    @GetMapping("/{seriesId}/season/{seasonNumber}")
    public ResponseEntity<List<Episode>> getSeasonEpisodes(@PathVariable String seriesId,
                                                        @PathVariable int seasonNumber,
                                                        Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(seriesService.getSeasonEpisodes(seriesId, seasonNumber, userId));
    }
    
    @GetMapping("/{seriesId}/season/{seasonNumber}/episode/{episodeNumber}")
    public ResponseEntity<Episode> getEpisode(@PathVariable String seriesId,
                                            @PathVariable int seasonNumber,
                                            @PathVariable int episodeNumber,
                                            Authentication authentication) {
        String userId = authentication.getName();
        return seriesService.getEpisode(seriesId, seasonNumber, episodeNumber, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{seriesId}/season/{seasonNumber}/episode/{episodeNumber}/stream")
    public ResponseEntity<String> getEpisodeStreamUrl(@PathVariable String seriesId,
                                                    @PathVariable int seasonNumber,
                                                    @PathVariable int episodeNumber,
                                                    Authentication authentication) {
        String userId = authentication.getName();
        return seriesService.getEpisode(seriesId, seasonNumber, episodeNumber, userId)
                .map(episode -> ResponseEntity.ok(episode.getStreamUrl()))
                .orElse(ResponseEntity.notFound().build());
    }
}
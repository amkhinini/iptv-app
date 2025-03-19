package com.ahmed.iptvapp.controller;

import com.ahmed.iptvapp.model.Movie;
import com.ahmed.iptvapp.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists/{playlistId}/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    
    @GetMapping
    public ResponseEntity<List<Movie>> getMovies(@PathVariable String playlistId,
                                               Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(movieService.getMoviesByPlaylist(playlistId, userId));
    }
    
    @GetMapping("/genres")
    public ResponseEntity<List<String>> getMovieGenres(@PathVariable String playlistId,
                                                    Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(movieService.getMovieGenres(playlistId, userId));
    }
    
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<Movie>> getMoviesByGenre(@PathVariable String playlistId,
                                                      @PathVariable String genre,
                                                      Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(movieService.getMoviesByGenre(playlistId, genre, userId));
    }
    
    @GetMapping("/favorites")
    public ResponseEntity<List<Movie>> getFavoriteMovies(@PathVariable String playlistId,
                                                       Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(movieService.getFavorites(playlistId, userId));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Movie>> searchMovies(@PathVariable String playlistId,
                                                  @RequestParam String query,
                                                  Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(movieService.searchMovies(query, playlistId, userId));
    }
    
    @GetMapping("/{movieId}")
    public ResponseEntity<Movie> getMovie(@PathVariable String movieId,
                                        Authentication authentication) {
        String userId = authentication.getName();
        return movieService.getMovie(movieId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{movieId}/favorite")
    public ResponseEntity<Movie> toggleFavorite(@PathVariable String movieId,
                                              Authentication authentication) {
        String userId = authentication.getName();
        Movie updatedMovie = movieService.toggleFavorite(movieId, userId);
        return ResponseEntity.ok(updatedMovie);
    }
    
    @GetMapping("/{movieId}/stream")
    public ResponseEntity<String> getStreamUrl(@PathVariable String movieId,
                                             Authentication authentication) {
        String userId = authentication.getName();
        return movieService.getMovie(movieId, userId)
                .map(movie -> ResponseEntity.ok(movie.getStreamUrl()))
                .orElse(ResponseEntity.notFound().build());
    }
}
package com.ahmed.iptvapp.controller;

import com.ahmed.iptvapp.configuration.PaginationConfig;
import com.ahmed.iptvapp.dto.PageResponse;
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
    private final PaginationConfig paginationConfig;
    
    @GetMapping
    public ResponseEntity<PageResponse<Movie>> getMovies(
            @PathVariable String playlistId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        int validPage = page == null || page < 0 ? 0 : page;
        int validSize = paginationConfig.validatePageSize(size, paginationConfig.getMoviesPageSize());
        
        return ResponseEntity.ok(movieService.getMoviesByPlaylistPaginated(playlistId, userId, validPage, validSize));
    }
    
    @GetMapping("/genres")
    public ResponseEntity<List<String>> getMovieGenres(@PathVariable String playlistId,
                                                    Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(movieService.getMovieGenres(playlistId, userId));
    }
    
    @GetMapping("/genre/{genre}")
    public ResponseEntity<PageResponse<Movie>> getMoviesByGenre(
            @PathVariable String playlistId,
            @PathVariable String genre,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        int validPage = page == null || page < 0 ? 0 : page;
        int validSize = paginationConfig.validatePageSize(size, paginationConfig.getMoviesPageSize());
        
        return ResponseEntity.ok(movieService.getMoviesByGenrePaginated(playlistId, genre, userId, validPage, validSize));
    }
    
    @GetMapping("/favorites")
    public ResponseEntity<PageResponse<Movie>> getFavoriteMovies(
            @PathVariable String playlistId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        int validPage = page == null || page < 0 ? 0 : page;
        int validSize = paginationConfig.validatePageSize(size, paginationConfig.getMoviesPageSize());
        
        return ResponseEntity.ok(movieService.getFavoritesPaginated(playlistId, userId, validPage, validSize));
    }
    
    @GetMapping("/search")
    public ResponseEntity<PageResponse<Movie>> searchMovies(
            @PathVariable String playlistId,
            @RequestParam String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        int validPage = page == null || page < 0 ? 0 : page;
        int validSize = paginationConfig.validatePageSize(size, paginationConfig.getMoviesPageSize());
        
        return ResponseEntity.ok(movieService.searchMoviesPaginated(query, playlistId, userId, validPage, validSize));
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

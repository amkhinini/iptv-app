package com.ahmed.iptvapp.service;

import com.ahmed.iptvapp.model.Movie;
import com.ahmed.iptvapp.repository.MovieRepository;
import com.ahmed.iptvapp.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {
    
    private final MovieRepository movieRepository;
    private final PlaylistRepository playlistRepository;
    
    /**
     * Get all movies for a playlist
     */
    public List<Movie> getMoviesByPlaylist(String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return movieRepository.findByPlaylistId(playlistId);
    }
    
    /**
     * Get movies by genre
     */
    public List<Movie> getMoviesByGenre(String playlistId, String genre, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return movieRepository.findByGenre(genre);
    }
    
    /**
     * Get a specific movie
     */
    public Optional<Movie> getMovie(String movieId, String userId) {
        Optional<Movie> movieOpt = movieRepository.findById(movieId);
        
        return movieOpt.flatMap(movie -> {
            // Verify the user owns the playlist the movie belongs to
            return playlistRepository.findById(movie.getPlaylistId())
                    .filter(playlist -> playlist.getUserId().equals(userId))
                    .map(playlist -> movie);
        });
    }
    
    /**
     * Toggle favorite status
     */
    public Movie toggleFavorite(String movieId, String userId) {
        Movie movie = getMovie(movieId, userId)
                .orElseThrow(() -> new RuntimeException("Movie not found or access denied"));
        
        movie.setFavorite(!movie.getFavorite());
        return movieRepository.save(movie);
    }
    
    /**
     * Get all favorite movies
     */
    public List<Movie> getFavorites(String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return movieRepository.findByPlaylistIdAndFavorite(playlistId, true);
    }
    
    /**
     * Search for movies by title
     */
    public List<Movie> searchMovies(String query, String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return movieRepository.findByTitleContainingIgnoreCase(query);
    }
    
    /**
     * Get all available movie genres
     */
    public List<String> getMovieGenres(String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return movieRepository.findByPlaylistId(playlistId).stream()
                .map(Movie::getGenre)
                .distinct()
                .sorted()
                .toList();
    }
}

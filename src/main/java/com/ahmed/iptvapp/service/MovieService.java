package com.ahmed.iptvapp.service;

import com.ahmed.iptvapp.dto.PageResponse;
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
     * Get all movies for a playlist with pagination
     */
    public PageResponse<Movie> getMoviesByPlaylistPaginated(String playlistId, String userId, int page, int size) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        List<Movie> allMovies = movieRepository.findByPlaylistId(playlistId);
        return paginateMovieList(allMovies, page, size);
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
     * Get movies by genre with pagination
     */
    public PageResponse<Movie> getMoviesByGenrePaginated(String playlistId, String genre, String userId, int page, int size) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        List<Movie> allMoviesByGenre = movieRepository.findByGenre(genre);
        return paginateMovieList(allMoviesByGenre, page, size);
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
     * Get all favorite movies with pagination
     */
    public PageResponse<Movie> getFavoritesPaginated(String playlistId, String userId, int page, int size) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        List<Movie> favorites = movieRepository.findByPlaylistIdAndFavorite(playlistId, true);
        return paginateMovieList(favorites, page, size);
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
     * Search for movies by title with pagination
     */
    public PageResponse<Movie> searchMoviesPaginated(String query, String playlistId, String userId, int page, int size) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        List<Movie> searchResults = movieRepository.findByTitleContainingIgnoreCase(query);
        return paginateMovieList(searchResults, page, size);
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
    
    /**
     * Helper method to paginate a list of movies
     */
    private PageResponse<Movie> paginateMovieList(List<Movie> movieList, int page, int size) {
        int totalElements = movieList.size();
        
        // Avoid overflow
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        
        List<Movie> paginatedContent = movieList.subList(fromIndex, toIndex);
        
        return PageResponse.of(paginatedContent, page, size, totalElements);
    }
}

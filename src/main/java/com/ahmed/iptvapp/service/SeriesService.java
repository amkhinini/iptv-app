package com.ahmed.iptvapp.service;

import com.ahmed.iptvapp.model.Episode;
import com.ahmed.iptvapp.model.Series;
import com.ahmed.iptvapp.repository.PlaylistRepository;
import com.ahmed.iptvapp.repository.SeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeriesService {
    
    private final SeriesRepository seriesRepository;
    private final PlaylistRepository playlistRepository;
    
    /**
     * Get all series for a playlist
     */
    public List<Series> getSeriesByPlaylist(String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return seriesRepository.findByPlaylistId(playlistId);
    }
    
    /**
     * Get series by genre
     */
    public List<Series> getSeriesByGenre(String playlistId, String genre, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return seriesRepository.findByGenre(genre);
    }
    
    /**
     * Get a specific series
     */
    public Optional<Series> getSeries(String seriesId, String userId) {
        Optional<Series> seriesOpt = seriesRepository.findById(seriesId);
        
        return seriesOpt.flatMap(series -> {
            // Verify the user owns the playlist the series belongs to
            return playlistRepository.findById(series.getPlaylistId())
                    .filter(playlist -> playlist.getUserId().equals(userId))
                    .map(playlist -> series);
        });
    }
    
    /**
     * Toggle favorite status
     */
    public Series toggleFavorite(String seriesId, String userId) {
        Series series = getSeries(seriesId, userId)
                .orElseThrow(() -> new RuntimeException("Series not found or access denied"));
        
        series.setFavorite(!series.getFavorite());
        return seriesRepository.save(series);
    }
    
    /**
     * Get all favorite series
     */
    public List<Series> getFavorites(String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return seriesRepository.findByPlaylistIdAndFavorite(playlistId, true);
    }
    
    /**
     * Search for series by title
     */
    public List<Series> searchSeries(String query, String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return seriesRepository.findByTitleContainingIgnoreCase(query);
    }
    
    /**
     * Get all available series genres
     */
    public List<String> getSeriesGenres(String playlistId, String userId) {
        // Verify the user owns the playlist
        boolean hasAccess = playlistRepository.findById(playlistId)
                .map(playlist -> playlist.getUserId().equals(userId))
                .orElse(false);
        
        if (!hasAccess) {
            throw new RuntimeException("Access denied to playlist");
        }
        
        return seriesRepository.findByPlaylistId(playlistId).stream()
                .map(Series::getGenre)
                .distinct()
                .sorted()
                .toList();
    }
    
    /**
     * Get episodes for a series
     */
    public List<Episode> getEpisodes(String seriesId, String userId) {
        Series series = getSeries(seriesId, userId)
                .orElseThrow(() -> new RuntimeException("Series not found or access denied"));
        
        return series.getEpisodes();
    }
    
    /**
     * Get specific episode by season and episode number
     */
    public Optional<Episode> getEpisode(String seriesId, int seasonNumber, int episodeNumber, String userId) {
        Series series = getSeries(seriesId, userId)
                .orElseThrow(() -> new RuntimeException("Series not found or access denied"));
        
        return series.getEpisodes().stream()
                .filter(e -> e.getSeasonNumber() == seasonNumber && e.getEpisodeNumber() == episodeNumber)
                .findFirst();
    }
    
    /**
     * Get all episodes for a specific season
     */
    public List<Episode> getSeasonEpisodes(String seriesId, int seasonNumber, String userId) {
        Series series = getSeries(seriesId, userId)
                .orElseThrow(() -> new RuntimeException("Series not found or access denied"));
        
        return series.getEpisodes().stream()
                .filter(e -> e.getSeasonNumber() == seasonNumber)
                .sorted((e1, e2) -> Integer.compare(e1.getEpisodeNumber(), e2.getEpisodeNumber()))
                .toList();
    }
    
    /**
     * Get all available seasons for a series
     */
    public List<Integer> getAvailableSeasons(String seriesId, String userId) {
        Series series = getSeries(seriesId, userId)
                .orElseThrow(() -> new RuntimeException("Series not found or access denied"));
        
        return series.getEpisodes().stream()
                .map(Episode::getSeasonNumber)
                .distinct()
                .sorted()
                .toList();
    }
}
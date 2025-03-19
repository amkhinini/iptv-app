package com.ahmed.iptvapp.repository;

import com.ahmed.iptvapp.model.Series;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeriesRepository extends MongoRepository<Series, String> {
    
    List<Series> findByPlaylistId(String playlistId);
    
    List<Series> findByGenre(String genre);
    
    List<Series> findByPlaylistIdAndFavorite(String playlistId, Boolean favorite);
    
    List<Series> findByTitleContainingIgnoreCase(String title);
}

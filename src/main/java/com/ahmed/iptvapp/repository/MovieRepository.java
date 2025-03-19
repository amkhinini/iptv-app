package com.ahmed.iptvapp.repository;

import com.ahmed.iptvapp.model.Movie;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends MongoRepository<Movie, String> {
    
    List<Movie> findByPlaylistId(String playlistId);
    
    List<Movie> findByGenre(String genre);
    
    List<Movie> findByPlaylistIdAndFavorite(String playlistId, Boolean favorite);
    
    List<Movie> findByTitleContainingIgnoreCase(String title);
}

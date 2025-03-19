package com.ahmed.iptvapp.repository;

import com.ahmed.iptvapp.model.Playlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistRepository extends MongoRepository<Playlist, String> {
    
    List<Playlist> findByUserId(String userId);
    
    List<Playlist> findByUserIdAndActive(String userId, Boolean active);
}

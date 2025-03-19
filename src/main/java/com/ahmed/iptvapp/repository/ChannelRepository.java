package com.ahmed.iptvapp.repository;

import com.ahmed.iptvapp.model.Channel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelRepository extends MongoRepository<Channel, String> {
    
    List<Channel> findByPlaylistId(String playlistId);
    
    List<Channel> findByPlaylistIdAndGroup(String playlistId, String group);
    
    List<Channel> findByPlaylistIdAndFavorite(String playlistId, Boolean favorite);
}

package com.ahmed.iptvapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "playlists")
public class Playlist {
    
    @Id
    private String id;
    
    private String name;
    
    private String userId;
    
    private String url;
    
    private String content;
    
    @Builder.Default
    private List<Channel> channels = new ArrayList<>();
    
    @Builder.Default
    private List<Movie> movies = new ArrayList<>();
    
    @Builder.Default
    private List<Series> series = new ArrayList<>();
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    private LocalDateTime lastRefreshed;
    
    private Boolean active;
}

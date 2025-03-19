package com.ahmed.iptvapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "movies")
public class Movie {
    
    @Id
    private String id;
    
    private String title;
    
    private String genre;
    
    private String streamUrl;
    
    private String thumbnailUrl;
    
    private String playlistId;
    
    private String description;
    
    private String releaseYear;
    
    private Integer duration; // in minutes
    
    @Builder.Default
    private Boolean favorite = false;
    
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();
}

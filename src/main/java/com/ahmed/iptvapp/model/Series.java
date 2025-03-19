package com.ahmed.iptvapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "series")
public class Series {
    
    @Id
    private String id;
    
    private String title;
    
    private String genre;
    
    private String thumbnailUrl;
    
    private String playlistId;
    
    private String description;
    
    @Builder.Default
    private List<Episode> episodes = new ArrayList<>();
    
    private Boolean favorite;
    
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();
}

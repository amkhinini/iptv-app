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
@Document(collection = "channels")
public class Channel {
    
    @Id
    private String id;
    
    private String name;
    
    private String group;
    
    private String streamUrl;
    
    private String logoUrl;
    
    private String playlistId;
    
    private Boolean favorite;
    
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();
}

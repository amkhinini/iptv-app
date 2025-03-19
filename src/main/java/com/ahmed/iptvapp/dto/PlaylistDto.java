package com.ahmed.iptvapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDto implements Serializable {
    
    private String id;
    
    private String userId;
    
    @NotBlank(message = "Playlist name is required")
    private String name;
    
    @Pattern(regexp = "^(http|https)://.*", message = "URL must start with http:// or https://")
    private String url;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRefreshed;
    private Boolean active;
    private Integer channelsCount;
    private Integer moviesCount;
    private Integer seriesCount;
}

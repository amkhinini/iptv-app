package com.ahmed.iptvapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a TV series episode.
 * Episodes are embedded within Series documents in MongoDB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Episode {

    /**
     * Unique identifier for the episode
     */
    private String id;

    /**
     * Episode title
     */
    private String title;

    /**
     * Season number
     */
    private Integer seasonNumber;

    /**
     * Episode number within the season
     */
    private Integer episodeNumber;

    /**
     * URL to the episode's video stream
     */
    private String streamUrl;

    /**
     * URL to the episode's thumbnail image
     */
    private String thumbnailUrl;

    /**
     * Length of the episode in minutes
     */
    @Builder.Default
    private Integer duration = 0;

    /**
     * Brief description of the episode
     */
    private String description;

    /**
     * Additional attributes from the M3U playlist
     */
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();
}
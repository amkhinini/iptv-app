package com.ahmed.iptvapp.service;

import com.ahmed.iptvapp.model.Channel;
import com.ahmed.iptvapp.model.Movie;
import com.ahmed.iptvapp.model.Playlist;
import com.ahmed.iptvapp.model.Series;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class M3UParserService {

    private static final String EXTINF_PATTERN = "#EXTINF:([^,]*),(.*)";
    private static final String EXTGRP_PATTERN = "group-title=\"([^\"]*)\"";
    private static final String EXTLOGO_PATTERN = "tvg-logo=\"([^\"]*)\"";
    private static final String MOVIE_CATEGORIES = "movie|cinema|film";
    private static final String SERIES_CATEGORIES = "series|tv shows|episodes";

    /**
     * Parse M3U content from a URL
     */
    public Playlist parseFromUrl(String url, String userId) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        Playlist playlist = Playlist.builder()
                .name("Playlist from " + url)
                .url(url)
                .userId(userId)
                .content(content.toString())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastRefreshed(LocalDateTime.now())
                .active(true)
                .build();
        
        parseContent(playlist, content.toString());
        return playlist;
    }
    
    /**
     * Parse M3U content from an uploaded file
     */
    public Playlist parseFromFile(MultipartFile file, String userId) throws IOException {
        String content = new String(file.getBytes());
        
        Playlist playlist = Playlist.builder()
                .name("Playlist from uploaded file")
                .userId(userId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastRefreshed(LocalDateTime.now())
                .active(true)
                .build();
        
        parseContent(playlist, content);
        return playlist;
    }

    /**
     * Parse the actual M3U content and populate the playlist with channels, movies, and series
     */
    private void parseContent(Playlist playlist, String content) {
        List<Channel> channels = new ArrayList<>();
        List<Movie> movies = new ArrayList<>();
        List<Series> series = new ArrayList<>();
        Map<String, Series> seriesMap = new HashMap<>();
        
        BufferedReader reader = new BufferedReader(new java.io.StringReader(content));
        String line;
        String currentExtInf = null;
        
        try {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#EXTINF")) {
                    currentExtInf = line;
                } else if (line.startsWith("http") && currentExtInf != null) {
                    // This is a stream URL following an EXTINF line
                    processStreamEntry(playlist.getId(), currentExtInf, line, channels, movies, series, seriesMap);
                    currentExtInf = null;
                }
            }
            
            playlist.setChannels(channels);
            playlist.setMovies(movies);
            playlist.setSeries(new ArrayList<>(seriesMap.values()));
            
        } catch (IOException e) {
            log.error("Error parsing M3U content", e);
        }
    }
    
    /**
     * Process a single stream entry (EXTINF + URL)
     */
    private void processStreamEntry(String playlistId, String extInf, String streamUrl, 
                                    List<Channel> channels, List<Movie> movies, 
                                    List<Series> series, Map<String, Series> seriesMap) {
        
        Map<String, String> attributes = parseAttributes(extInf);
        String title = parseTitle(extInf);
        String group = attributes.getOrDefault("group-title", "No Category");
        String logo = attributes.getOrDefault("tvg-logo", "");
        
        // Determine the content type based on group
        if (isMovieGroup(group)) {
            movies.add(createMovie(playlistId, title, group, streamUrl, logo, attributes));
        } else if (isSeriesGroup(group)) {
            addToSeries(playlistId, title, group, streamUrl, logo, attributes, seriesMap);
        } else {
            channels.add(createChannel(playlistId, title, group, streamUrl, logo, attributes));
        }
    }
    
    /**
     * Create a Channel object from parsed attributes
     */
    private Channel createChannel(String playlistId, String name, String group, 
                                 String streamUrl, String logoUrl, Map<String, String> attributes) {
        return Channel.builder()
                .name(name)
                .group(group)
                .streamUrl(streamUrl)
                .logoUrl(logoUrl)
                .playlistId(playlistId)
                .favorite(false)
                .attributes(attributes)
                .build();
    }
    
    /**
     * Create a Movie object from parsed attributes
     */
    private Movie createMovie(String playlistId, String title, String genre, 
                             String streamUrl, String thumbnailUrl, Map<String, String> attributes) {
        return Movie.builder()
                .title(title)
                .genre(genre)
                .streamUrl(streamUrl)
                .thumbnailUrl(thumbnailUrl)
                .playlistId(playlistId)
                .favorite(false)
                .attributes(attributes)
                .build();
    }
    
    /**
     * Add a stream to a series
     */
    private void addToSeries(String playlistId, String title, String genre, 
                            String streamUrl, String thumbnailUrl, 
                            Map<String, String> attributes, Map<String, Series> seriesMap) {
        
        // Try to extract series name and episode info from title
        String seriesName = extractSeriesName(title);
        int[] seasonEpisode = extractSeasonEpisode(title);
        
        Series series = seriesMap.getOrDefault(seriesName, Series.builder()
                .title(seriesName)
                .genre(genre)
                .thumbnailUrl(thumbnailUrl)
                .playlistId(playlistId)
                .favorite(false)
                .episodes(new ArrayList<>())
                .attributes(new HashMap<>())
                .build());
        
        // Add episode
        series.getEpisodes().add(com.ahmed.iptvapp.model.Episode.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .seasonNumber(seasonEpisode[0])
                .episodeNumber(seasonEpisode[1])
                .streamUrl(streamUrl)
                .thumbnailUrl(thumbnailUrl)
                .attributes(attributes)
                .build());
        
        seriesMap.put(seriesName, series);
    }
    
    /**
     * Check if a group is a movie category
     */
    private boolean isMovieGroup(String group) {
        return group.toLowerCase().matches(".*(" + MOVIE_CATEGORIES + ").*");
    }
    
    /**
     * Check if a group is a series category
     */
    private boolean isSeriesGroup(String group) {
        return group.toLowerCase().matches(".*(" + SERIES_CATEGORIES + ").*");
    }
    
    /**
     * Extract attributes from EXTINF line
     */
    private Map<String, String> parseAttributes(String extInf) {
        Map<String, String> attributes = new HashMap<>();
        
        // Extract group-title
        Pattern groupPattern = Pattern.compile(EXTGRP_PATTERN);
        Matcher groupMatcher = groupPattern.matcher(extInf);
        if (groupMatcher.find()) {
            attributes.put("group-title", groupMatcher.group(1));
        }
        
        // Extract tvg-logo
        Pattern logoPattern = Pattern.compile(EXTLOGO_PATTERN);
        Matcher logoMatcher = logoPattern.matcher(extInf);
        if (logoMatcher.find()) {
            attributes.put("tvg-logo", logoMatcher.group(1));
        }
        
        // Extract other tvg-* attributes
        Pattern tvgPattern = Pattern.compile("tvg-([^=]*)=\"([^\"]*)\"");
        Matcher tvgMatcher = tvgPattern.matcher(extInf);
        while (tvgMatcher.find()) {
            attributes.put("tvg-" + tvgMatcher.group(1), tvgMatcher.group(2));
        }
        
        return attributes;
    }
    
    /**
     * Extract title from EXTINF line
     */
    private String parseTitle(String extInf) {
        Pattern pattern = Pattern.compile(EXTINF_PATTERN);
        Matcher matcher = pattern.matcher(extInf);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "Unknown";
    }
    
    /**
     * Extract series name from title
     */
    private String extractSeriesName(String title) {
        // Try to extract series name using common patterns
        // E.g., "ShowName S01E01", "ShowName - Season 1 Episode 1"
        String[] patterns = {
            "(.+?)\\s+[Ss]\\d+[Ee]\\d+.*",  // ShowName S01E01
            "(.+?)\\s+-\\s+Season\\s+\\d+\\s+Episode\\s+\\d+.*", // ShowName - Season 1 Episode 1
            "(.+?)\\s+\\d+x\\d+.*" // ShowName 1x01
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(title);
            if (m.matches()) {
                return m.group(1).trim();
            }
        }
        
        // If no pattern matches, just return the title
        return title;
    }
    
    /**
     * Extract season and episode numbers from title
     */
    private int[] extractSeasonEpisode(String title) {
        int[] result = {0, 0}; // [season, episode]
        
        // Try S01E01 pattern
        Pattern seasonEpisodePattern = Pattern.compile("[Ss](\\d+)[Ee](\\d+)");
        Matcher seMatch = seasonEpisodePattern.matcher(title);
        if (seMatch.find()) {
            result[0] = Integer.parseInt(seMatch.group(1));
            result[1] = Integer.parseInt(seMatch.group(2));
            return result;
        }
        
        // Try "Season X Episode Y" pattern
        Pattern textPattern = Pattern.compile("Season\\s+(\\d+)\\s+Episode\\s+(\\d+)");
        Matcher textMatch = textPattern.matcher(title);
        if (textMatch.find()) {
            result[0] = Integer.parseInt(textMatch.group(1));
            result[1] = Integer.parseInt(textMatch.group(2));
            return result;
        }
        
        // Try 1x01 pattern
        Pattern alternatePattern = Pattern.compile("(\\d+)x(\\d+)");
        Matcher altMatch = alternatePattern.matcher(title);
        if (altMatch.find()) {
            result[0] = Integer.parseInt(altMatch.group(1));
            result[1] = Integer.parseInt(altMatch.group(2));
            return result;
        }
        
        return result;
    }
}

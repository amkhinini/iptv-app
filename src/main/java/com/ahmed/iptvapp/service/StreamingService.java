package com.ahmed.iptvapp.service;

import com.ahmed.iptvapp.model.Channel;
import com.ahmed.iptvapp.model.Episode;
import com.ahmed.iptvapp.model.Movie;
import com.ahmed.iptvapp.model.Series;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingService {

    private final ChannelService channelService;
    private final MovieService movieService;
    private final SeriesService seriesService;
    
    @Value("${server.host:localhost}")
    private String serverHost;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    /**
     * Get direct stream URL for a channel
     */
    public String getChannelStreamUrl(String channelId, String userId) {
        Channel channel = channelService.getChannel(channelId, userId)
                .orElseThrow(() -> new RuntimeException("Channel not found or access denied"));
        
        return channel.getStreamUrl();
    }
    
    /**
     * Get direct stream URL for a movie
     */
    public String getMovieStreamUrl(String movieId, String userId) {
        Movie movie = movieService.getMovie(movieId, userId)
                .orElseThrow(() -> new RuntimeException("Movie not found or access denied"));
        
        return movie.getStreamUrl();
    }
    
    /**
     * Get direct stream URL for a series episode
     */
    public String getEpisodeStreamUrl(String seriesId, int seasonNumber, int episodeNumber, String userId) {
        Episode episode = seriesService.getEpisode(seriesId, seasonNumber, episodeNumber, userId)
                .orElseThrow(() -> new RuntimeException("Episode not found or access denied"));
        
        return episode.getStreamUrl();
    }
    
    /**
     * Create a proxied stream URL for a channel
     */
    public String getProxiedChannelStreamUrl(String channelId, String userId) {
        return String.format("http://%s:%s/api/stream/channel/%s", 
                serverHost, serverPort, channelId);
    }
    
    /**
     * Create a proxied stream URL for a movie
     */
    public String getProxiedMovieStreamUrl(String movieId, String userId) {
        return String.format("http://%s:%s/api/stream/movie/%s", 
                serverHost, serverPort, movieId);
    }
    
    /**
     * Create a proxied stream URL for a series episode
     */
    public String getProxiedEpisodeStreamUrl(String seriesId, int seasonNumber, int episodeNumber, String userId) {
        return String.format("http://%s:%s/api/stream/series/%s/season/%d/episode/%d", 
                serverHost, serverPort, seriesId, seasonNumber, episodeNumber);
    }
    
    /**
     * Proxy stream content from the original source
     */
    public ResponseEntity<StreamingResponseBody> proxyStream(String streamUrl) {
        try {
            URL url = new URL(streamUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            // Forward common streaming headers
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            // Check response
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpStatus.OK.value()) {
                log.error("Failed to proxy stream, response code: {}", responseCode);
                return ResponseEntity.status(responseCode).build();
            }
            
            // Get content type
            String contentType = connection.getContentType();
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            
            // Set up streaming response
            StreamingResponseBody responseBody = outputStream -> {
                try (InputStream inputStream = connection.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                    }
                }
            };
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(responseBody);
            
        } catch (IOException e) {
            log.error("Error while proxying stream", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Handle HLS (m3u8) playlist proxying
     */
    public ResponseEntity<Resource> proxyHlsPlaylist(String playlistUrl) {
        try {
            URL url = new URL(playlistUrl);
            Resource resource = new UrlResource(url.toURI());
            
            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/vnd.apple.mpegurl";
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException | IOException e) {
            log.error("Error while proxying HLS playlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error while proxying HLS playlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Detect stream type (direct, HLS, etc.)
     */
    public String detectStreamType(String url) {
        String lowerCaseUrl = url.toLowerCase();
        
        if (lowerCaseUrl.endsWith(".m3u8")) {
            return "hls";
        } else if (lowerCaseUrl.endsWith(".mpd")) {
            return "dash";
        } else if (lowerCaseUrl.contains("rtmp://")) {
            return "rtmp";
        } else if (lowerCaseUrl.endsWith(".mp4") || lowerCaseUrl.endsWith(".mkv") || 
                lowerCaseUrl.endsWith(".avi") || lowerCaseUrl.endsWith(".mov")) {
            return "direct";
        } else {
            return "unknown";
        }
    }
}
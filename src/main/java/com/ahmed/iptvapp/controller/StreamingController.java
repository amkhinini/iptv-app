package com.ahmed.iptvapp.controller;

import com.ahmed.iptvapp.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@Slf4j
public class StreamingController {

    private final StreamingService streamingService;

    /**
     * Stream a channel
     */
    @GetMapping("/channel/{channelId}")
    public ResponseEntity<?> streamChannel(@PathVariable String channelId,
                                         Authentication authentication) {
        try {
            String userId = authentication.getName();
            String streamUrl = streamingService.getChannelStreamUrl(channelId, userId);
            String streamType = streamingService.detectStreamType(streamUrl);
            
            if ("hls".equals(streamType)) {
                return streamingService.proxyHlsPlaylist(streamUrl);
            } else {
                return streamingService.proxyStream(streamUrl);
            }
        } catch (Exception e) {
            log.error("Error streaming channel", e);
            return ResponseEntity.internalServerError().body("Error streaming content: " + e.getMessage());
        }
    }

    /**
     * Stream a movie
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<?> streamMovie(@PathVariable String movieId,
                                       Authentication authentication) {
        try {
            String userId = authentication.getName();
            String streamUrl = streamingService.getMovieStreamUrl(movieId, userId);
            String streamType = streamingService.detectStreamType(streamUrl);
            
            if ("hls".equals(streamType)) {
                return streamingService.proxyHlsPlaylist(streamUrl);
            } else {
                return streamingService.proxyStream(streamUrl);
            }
        } catch (Exception e) {
            log.error("Error streaming movie", e);
            return ResponseEntity.internalServerError().body("Error streaming content: " + e.getMessage());
        }
    }

    /**
     * Stream a series episode
     */
    @GetMapping("/series/{seriesId}/season/{seasonNumber}/episode/{episodeNumber}")
    public ResponseEntity<?> streamEpisode(@PathVariable String seriesId,
                                         @PathVariable int seasonNumber,
                                         @PathVariable int episodeNumber,
                                         Authentication authentication) {
        try {
            String userId = authentication.getName();
            String streamUrl = streamingService.getEpisodeStreamUrl(seriesId, seasonNumber, episodeNumber, userId);
            String streamType = streamingService.detectStreamType(streamUrl);
            
            if ("hls".equals(streamType)) {
                return streamingService.proxyHlsPlaylist(streamUrl);
            } else {
                return streamingService.proxyStream(streamUrl);
            }
        } catch (Exception e) {
            log.error("Error streaming episode", e);
            return ResponseEntity.internalServerError().body("Error streaming content: " + e.getMessage());
        }
    }

    /**
     * Get channel stream info (URL, type, etc.)
     */
    @GetMapping("/info/channel/{channelId}")
    public ResponseEntity<StreamInfo> getChannelStreamInfo(@PathVariable String channelId,
                                                        Authentication authentication) {
        try {
            String userId = authentication.getName();
            String streamUrl = streamingService.getChannelStreamUrl(channelId, userId);
            String streamType = streamingService.detectStreamType(streamUrl);
            String proxyUrl = streamingService.getProxiedChannelStreamUrl(channelId, userId);
            
            return ResponseEntity.ok(new StreamInfo(streamUrl, streamType, proxyUrl));
        } catch (Exception e) {
            log.error("Error getting channel stream info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get movie stream info
     */
    @GetMapping("/info/movie/{movieId}")
    public ResponseEntity<StreamInfo> getMovieStreamInfo(@PathVariable String movieId,
                                                      Authentication authentication) {
        try {
            String userId = authentication.getName();
            String streamUrl = streamingService.getMovieStreamUrl(movieId, userId);
            String streamType = streamingService.detectStreamType(streamUrl);
            String proxyUrl = streamingService.getProxiedMovieStreamUrl(movieId, userId);
            
            return ResponseEntity.ok(new StreamInfo(streamUrl, streamType, proxyUrl));
        } catch (Exception e) {
            log.error("Error getting movie stream info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get episode stream info
     */
    @GetMapping("/info/series/{seriesId}/season/{seasonNumber}/episode/{episodeNumber}")
    public ResponseEntity<StreamInfo> getEpisodeStreamInfo(@PathVariable String seriesId,
                                                        @PathVariable int seasonNumber,
                                                        @PathVariable int episodeNumber,
                                                        Authentication authentication) {
        try {
            String userId = authentication.getName();
            String streamUrl = streamingService.getEpisodeStreamUrl(seriesId, seasonNumber, episodeNumber, userId);
            String streamType = streamingService.detectStreamType(streamUrl);
            String proxyUrl = streamingService.getProxiedEpisodeStreamUrl(seriesId, seasonNumber, episodeNumber, userId);
            
            return ResponseEntity.ok(new StreamInfo(streamUrl, streamType, proxyUrl));
        } catch (Exception e) {
            log.error("Error getting episode stream info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Stream info class for returning stream details
     */
    private static class StreamInfo {
        private final String directUrl;
        private final String streamType;
        private final String proxyUrl;

        public StreamInfo(String directUrl, String streamType, String proxyUrl) {
            this.directUrl = directUrl;
            this.streamType = streamType;
            this.proxyUrl = proxyUrl;
        }

        public String getDirectUrl() {
            return directUrl;
        }

        public String getStreamType() {
            return streamType;
        }

        public String getProxyUrl() {
            return proxyUrl;
        }
    }
}
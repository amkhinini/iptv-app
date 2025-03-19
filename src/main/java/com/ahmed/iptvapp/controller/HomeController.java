package com.ahmed.iptvapp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HomeController {

    @Value("${spring.application.name}")
    private String appName;
    
    @Value("${spring.application.version:1.0.0}")
    private String appVersion;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", appName);
        info.put("version", appVersion);
        info.put("status", "running");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("authentication", "/api/auth");
        endpoints.put("playlists", "/api/playlists");
        endpoints.put("channels", "/api/playlists/{playlistId}/channels");
        endpoints.put("movies", "/api/playlists/{playlistId}/movies");
        endpoints.put("series", "/api/playlists/{playlistId}/series");
        endpoints.put("streaming", "/api/stream");
        
        info.put("endpoints", endpoints);
        
        return ResponseEntity.ok(info);
    }
    
    @GetMapping("/public/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        return ResponseEntity.ok(status);
    }
}

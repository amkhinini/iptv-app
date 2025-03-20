package com.ahmed.iptvapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistImportResponse {
    private String taskId;
    private String playlistId;
    private String status;  // PENDING, PROCESSING, COMPLETED, FAILED
    private Integer progress;  // 0-100 percent
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
    private PlaylistDto playlist;
    
    public static PlaylistImportResponse pending(String taskId) {
        return PlaylistImportResponse.builder()
                .taskId(taskId)
                .status("PENDING")
                .progress(0)
                .startTime(LocalDateTime.now())
                .build();
    }
    
    public static PlaylistImportResponse completed(String taskId, String playlistId, PlaylistDto playlist) {
        return PlaylistImportResponse.builder()
                .taskId(taskId)
                .playlistId(playlistId)
                .status("COMPLETED")
                .progress(100)
                .startTime(playlist.getCreatedAt())
                .endTime(LocalDateTime.now())
                .playlist(playlist)
                .build();
    }
    
    public static PlaylistImportResponse failed(String taskId, String error) {
        return PlaylistImportResponse.builder()
                .taskId(taskId)
                .status("FAILED")
                .progress(0)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .errorMessage(error)
                .build();
    }
    
    public static PlaylistImportResponse processing(String taskId, int progress) {
        return PlaylistImportResponse.builder()
                .taskId(taskId)
                .status("PROCESSING")
                .progress(progress)
                .startTime(LocalDateTime.now())
                .build();
    }
}

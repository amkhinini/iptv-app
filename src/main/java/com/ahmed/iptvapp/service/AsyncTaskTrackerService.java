package com.ahmed.iptvapp.service;

import com.ahmed.iptvapp.dto.PlaylistImportResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track the status of asynchronous tasks
 */
@Service
public class AsyncTaskTrackerService {
    
    private final Map<String, PlaylistImportResponse> taskStatusMap = new ConcurrentHashMap<>();
    
    /**
     * Create a new task and return the task ID
     */
    public String createTask() {
        String taskId = generateTaskId();
        PlaylistImportResponse response = PlaylistImportResponse.pending(taskId);
        taskStatusMap.put(taskId, response);
        return taskId;
    }
    
    /**
     * Update the status of a task
     */
    public void updateTaskStatus(String taskId, PlaylistImportResponse status) {
        taskStatusMap.put(taskId, status);
    }
    
    /**
     * Get the status of a task
     */
    public Optional<PlaylistImportResponse> getTaskStatus(String taskId) {
        return Optional.ofNullable(taskStatusMap.get(taskId));
    }
    
    /**
     * Generate a unique task ID
     */
    private String generateTaskId() {
        return java.util.UUID.randomUUID().toString();
    }
    
    /**
     * Clean up completed tasks older than 24 hours
     * Note: This would typically be scheduled to run periodically
     */
    public void cleanupOldTasks() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(1);
        
        taskStatusMap.entrySet().removeIf(entry -> {
            PlaylistImportResponse status = entry.getValue();
            return (status.getStatus().equals("COMPLETED") || status.getStatus().equals("FAILED")) 
                   && status.getEndTime() != null 
                   && status.getEndTime().isBefore(cutoff);
        });
    }
}

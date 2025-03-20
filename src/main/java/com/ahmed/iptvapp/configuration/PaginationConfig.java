package com.ahmed.iptvapp.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class PaginationConfig {
    
    @Value("${app.pagination.default-page-size:20}")
    private int defaultPageSize;
    
    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;
    
    @Value("${app.pagination.channels-page-size:50}")
    private int channelsPageSize;
    
    @Value("${app.pagination.movies-page-size:24}")
    private int moviesPageSize;
    
    @Value("${app.pagination.series-page-size:24}")
    private int seriesPageSize;
    
    public int validatePageSize(Integer requestedSize, int defaultSize) {
        if (requestedSize == null) {
            return defaultSize;
        }
        
        return Math.min(Math.max(1, requestedSize), maxPageSize);
    }
}

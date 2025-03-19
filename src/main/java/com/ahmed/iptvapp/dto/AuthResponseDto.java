package com.ahmed.iptvapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    
    private String token;
    private String userId;
    private String username;
    
    @Builder.Default
    private String tokenType = "Bearer";
}

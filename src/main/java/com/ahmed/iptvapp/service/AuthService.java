package com.ahmed.iptvapp.service;

import com.ahmed.iptvapp.dto.AuthResponseDto;
import com.ahmed.iptvapp.dto.LoginRequestDto;
import com.ahmed.iptvapp.dto.RegisterRequestDto;
import com.ahmed.iptvapp.model.User;
import com.ahmed.iptvapp.service.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponseDto register(RegisterRequestDto request) {
        // Check if username or email already exists
        if (userService.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        if (userService.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
        
        User savedUser = userService.createUser(user);
        
        // Generate JWT token
        String token = jwtService.generateToken(savedUser);
        
        return AuthResponseDto.builder()
                .token(token)
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .build();
    }

    public AuthResponseDto login(LoginRequestDto request) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        
        // Generate JWT token
        User user = (User) userService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(user);
        
        return AuthResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }
}

package com.bis.intelliguide.service.auth;

import com.bis.intelliguide.dto.request.LoginRequest;
import com.bis.intelliguide.dto.request.RegisterRequest;
import com.bis.intelliguide.dto.response.AuthResponse;
import com.bis.intelliguide.model.User;
import com.bis.intelliguide.repository.UserRepository;
import com.bis.intelliguide.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // request.getRole() is already constrained to CONSUMER|MSME|LABORATORY by
        // @Pattern validation on the DTO — ADMIN can never reach this line via this endpoint.
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .phone(request.getPhone())
                .preferredLanguage(request.getPreferredLanguage())
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return AuthResponse.builder()
                .token(token)
                .user(new AuthResponse.UserSummary(user.getId(), user.getName(), user.getRole()))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("This account has been disabled. Contact support.");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return AuthResponse.builder()
                .token(token)
                .user(new AuthResponse.UserSummary(user.getId(), user.getName(), user.getRole()))
                .build();
    }
}

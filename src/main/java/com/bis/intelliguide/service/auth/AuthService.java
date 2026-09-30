package com.bis.intelliguide.service.auth;

import com.bis.intelliguide.dto.request.CompleteProfileRequest;
import com.bis.intelliguide.dto.request.LoginRequest;
import com.bis.intelliguide.dto.request.RegisterRequest;
import com.bis.intelliguide.dto.response.AuthResponse;
import com.bis.intelliguide.model.User;
import com.bis.intelliguide.repository.UserRepository;
import com.bis.intelliguide.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.google.client-id}")
    private String googleClientId;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .phone(request.getPhone())
                .preferredLanguage(request.getPreferredLanguage())
                .status("ACTIVE")
                .profileCompleted(true)
                .createdAt(Instant.now())
                .build();

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .isNewUser(true)
                        .profileCompleted(true)
                        .build())
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
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .isNewUser(false)
                        .profileCompleted(user.getProfileCompleted() != null && user.getProfileCompleted())
                        .build())
                .build();
    }

    public AuthResponse authenticateWithGoogle(String idToken) {
        GoogleIdToken.Payload payload = verifyGoogleToken(idToken);
        if (payload == null) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        Optional<User> existingUser = userRepository.findByGoogleId(googleId);
        if (existingUser.isEmpty()) {
            existingUser = userRepository.findByEmail(email);
        }

        boolean isNew = existingUser.isEmpty();
        User user;

        if (isNew) {
            user = User.builder()
                    .googleId(googleId)
                    .name(name != null ? name : email)
                    .email(email)
                    .role("CONSUMER")
                    .status("ACTIVE")
                    .profileCompleted(false)
                    .createdAt(Instant.now())
                    .build();
            user = userRepository.save(user);
        } else {
            user = existingUser.get();
            boolean needsSave = false;

            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                needsSave = true;
            }

            // Invited admin's first login: activate the account + pick up their real name.
            if ("INVITED".equals(user.getStatus())) {
                user.setStatus("ACTIVE");
                needsSave = true;
            }
            if ("Invited Admin".equals(user.getName()) && name != null) {
                user.setName(name);
                needsSave = true;
            }

            if (needsSave) {
                user = userRepository.save(user);
            }
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .isNewUser(isNew)
                        .profileCompleted(user.getProfileCompleted() != null && user.getProfileCompleted())
                        .build())
                .build();
    }

    public AuthResponse completeProfile(String userId, CompleteProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setPreferredLanguage(request.getPreferredLanguage());
        user.setUdyamRegistered(request.getUdyamRegistered());
        user.setLabAffiliation(request.getLabAffiliation());
        user.setProfileCompleted(true);
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .isNewUser(false)
                        .profileCompleted(true)
                        .build())
                .build();
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken token = verifier.verify(idToken);
            return token != null ? token.getPayload() : null;
        } catch (Exception e) {
            return null;
        }
    }
}

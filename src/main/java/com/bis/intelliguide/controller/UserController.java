package com.bis.intelliguide.controller;

import com.bis.intelliguide.dto.request.CompleteProfileRequest;
import com.bis.intelliguide.dto.response.AuthResponse;
import com.bis.intelliguide.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PostMapping("/complete-profile")
    public ResponseEntity<AuthResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request,
                                                          Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(authService.completeProfile(userId, request));
    }
}

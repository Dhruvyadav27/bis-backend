package com.bis.intelliguide.controller;

import com.bis.intelliguide.dto.request.CertificationRecommendRequest;
import com.bis.intelliguide.dto.response.CertificationRecommendResponse;
import com.bis.intelliguide.service.agents.CertificationGuideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certification")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationGuideService certificationGuideService;

    @PostMapping("/recommend")
    public ResponseEntity<CertificationRecommendResponse> recommend(@Valid @RequestBody CertificationRecommendRequest request,
                                                                      Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : null;
        return ResponseEntity.ok(certificationGuideService.recommend(request, userId));
    }
}

package com.bis.intelliguide.controller;

import com.bis.intelliguide.dto.request.StandardSearchRequest;
import com.bis.intelliguide.dto.response.StandardSearchResponse;
import com.bis.intelliguide.service.agents.StandardFinderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/standards")
@RequiredArgsConstructor
public class StandardFinderController {

    private final StandardFinderService standardFinderService;

    @PostMapping("/search")
    public ResponseEntity<StandardSearchResponse> search(@Valid @RequestBody StandardSearchRequest request,
                                                           Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : null;
        return ResponseEntity.ok(standardFinderService.search(request.getProductDescription(), userId));
    }
}

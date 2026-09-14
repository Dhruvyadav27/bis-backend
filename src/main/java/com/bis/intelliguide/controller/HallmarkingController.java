package com.bis.intelliguide.controller;

import com.bis.intelliguide.dto.request.HuidVerifyRequest;
import com.bis.intelliguide.dto.response.HuidVerifyResponse;
import com.bis.intelliguide.service.agents.HallmarkingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hallmarking")
@RequiredArgsConstructor
public class HallmarkingController {

    private final HallmarkingService hallmarkingService;

    @PostMapping("/verify-huid")
    public ResponseEntity<HuidVerifyResponse> verifyHuid(@Valid @RequestBody HuidVerifyRequest request) {
        return ResponseEntity.ok(hallmarkingService.verifyHuid(request.getHuid()));
    }
}

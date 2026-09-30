package com.bis.intelliguide.controller;

import com.bis.intelliguide.dto.request.HallmarkAskRequest;
import com.bis.intelliguide.dto.request.HallmarkComplaintRequest;
import com.bis.intelliguide.dto.request.HuidVerifyRequest;
import com.bis.intelliguide.dto.response.AssistantResponse;
import com.bis.intelliguide.dto.response.ComplaintGuidanceResponse;
import com.bis.intelliguide.dto.response.HuidVerifyResponse;
import com.bis.intelliguide.dto.response.NearestCentreResponse;
import com.bis.intelliguide.dto.response.PurityInfoResponse;
import com.bis.intelliguide.service.agents.HallmarkingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hallmarking")
@RequiredArgsConstructor
public class HallmarkingController {

    private final HallmarkingService hallmarkingService;

    @PostMapping("/verify-huid")
    public ResponseEntity<HuidVerifyResponse> verifyHuid(@Valid @RequestBody HuidVerifyRequest request) {
        return ResponseEntity.ok(hallmarkingService.verifyHuid(request.getHuid()));
    }

    @GetMapping("/nearest-centres")
    public ResponseEntity<NearestCentreResponse> nearestCentres(
            @RequestParam double lat, @RequestParam double lng) {
        return ResponseEntity.ok(hallmarkingService.findNearestCentres(lat, lng));
    }

    @GetMapping("/jeweller-registration-info")
    public ResponseEntity<Map<String, Object>> jewellerRegistrationInfo() {
        return ResponseEntity.ok(hallmarkingService.getJewellerRegistrationInfo());
    }

    @GetMapping("/purity-info")
    public ResponseEntity<PurityInfoResponse> purityInfo(@RequestParam String code) {
        return ResponseEntity.ok(hallmarkingService.getPurityInfo(code));
    }

    @PostMapping("/complaint-guidance")
    public ResponseEntity<ComplaintGuidanceResponse> complaintGuidance(
            @Valid @RequestBody HallmarkComplaintRequest request) {
        return ResponseEntity.ok(hallmarkingService.getComplaintGuidance(request.getQuery()));
    }

    @PostMapping("/ask")
    public ResponseEntity<AssistantResponse> ask(@Valid @RequestBody HallmarkAskRequest request) {
        return ResponseEntity.ok(hallmarkingService.askFollowUp(request));
    }
}

package com.bis.intelliguide.controller;

import com.bis.intelliguide.dto.request.ComplaintRequest;
import com.bis.intelliguide.dto.request.ConsumerQueryRequest;
import com.bis.intelliguide.dto.response.ComplaintResponse;
import com.bis.intelliguide.dto.response.ConsumerAskResponse;
import com.bis.intelliguide.service.agents.ConsumerAffairsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consumer")
@RequiredArgsConstructor
public class ConsumerAffairsController {

    private final ConsumerAffairsService consumerAffairsService;

    /** General query mode — free-form consumer questions, not limited to complaints. */
    @PostMapping("/ask")
    public ResponseEntity<ConsumerAskResponse> ask(@Valid @RequestBody ConsumerQueryRequest request) {
        return ResponseEntity.ok(consumerAffairsService.ask(request.getQuery()));
    }

    /** Complaint filing mode — one use case within this agent. */
    @PostMapping("/complaint")
    public ResponseEntity<ComplaintResponse> fileComplaint(@Valid @RequestBody ComplaintRequest request,
                                                             Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : null;
        return ResponseEntity.ok(consumerAffairsService.fileComplaint(request, userId));
    }
}

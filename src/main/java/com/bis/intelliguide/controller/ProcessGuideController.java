package com.bis.intelliguide.controller;

import com.bis.intelliguide.dto.request.JourneyAdvanceRequest;
import com.bis.intelliguide.dto.request.JourneyStartRequest;
import com.bis.intelliguide.dto.response.JourneyAdvanceResponse;
import com.bis.intelliguide.dto.response.JourneyResponse;
import com.bis.intelliguide.service.orchestrator.ProcessGuideOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/journey")
@RequiredArgsConstructor
public class ProcessGuideController {

    private final ProcessGuideOrchestratorService orchestratorService;

    @GetMapping("/current")
    public ResponseEntity<JourneyResponse> current(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        JourneyResponse response = orchestratorService.getCurrent(userId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    @PostMapping("/start")
    public ResponseEntity<JourneyResponse> start(@Valid @RequestBody JourneyStartRequest request,
                                                  Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(orchestratorService.start(request, userId));
    }

    @PostMapping("/advance")
    public ResponseEntity<JourneyAdvanceResponse> advance(@Valid @RequestBody JourneyAdvanceRequest request,
                                                            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        // NOTE: manufacturerType/state/district would ideally be carried from the original
        // JourneyStartRequest via the stored journey document; passed as null here in this
        // scaffold's advance() call for brevity — wire these through when persisting the
        // original journey context in a full build.
        return ResponseEntity.ok(orchestratorService.advance(
                request.getJourneyId(), request.getAction(), userId, null, null, null));
    }
}

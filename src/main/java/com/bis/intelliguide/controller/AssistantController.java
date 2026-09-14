package com.bis.intelliguide.controller;

import com.bis.intelliguide.dto.request.AssistantQueryRequest;
import com.bis.intelliguide.dto.response.AssistantResponse;
import com.bis.intelliguide.service.agents.AssistantRouterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantRouterService assistantRouterService;

    @PostMapping("/query")
    public ResponseEntity<AssistantResponse> query(@Valid @RequestBody AssistantQueryRequest request) {
        return ResponseEntity.ok(assistantRouterService.handle(request));
    }
}

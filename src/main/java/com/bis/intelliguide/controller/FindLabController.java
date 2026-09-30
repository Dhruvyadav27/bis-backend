package com.bis.intelliguide.controller;

import com.bis.intelliguide.dto.response.LabSearchResponse;
import com.bis.intelliguide.service.agents.FindLabService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/labs")
@RequiredArgsConstructor
public class FindLabController {

    private final FindLabService findLabService;

    @GetMapping("/nearest")
    public ResponseEntity<LabSearchResponse> nearest(
            @RequestParam(required = false) String productDescription,
            @RequestParam double lat,
            @RequestParam double lng) {
        return ResponseEntity.ok(findLabService.findNearest(productDescription, lat, lng));
    }

    /** Legacy search endpoint kept for backward compatibility. */
    @GetMapping("/search")
    public ResponseEntity<LabSearchResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district) {
        return ResponseEntity.ok(findLabService.search(query, state, district));
    }
}

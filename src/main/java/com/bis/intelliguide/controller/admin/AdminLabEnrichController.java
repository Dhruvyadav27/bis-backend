package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.service.agents.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/labs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLabEnrichController {

    private final GeocodingService geocodingService;

    @PostMapping("/enrich-geolocation")
    public ResponseEntity<Map<String, Object>> enrichGeolocation() {
        return ResponseEntity.ok(geocodingService.enrichLabGeolocation());
    }
}

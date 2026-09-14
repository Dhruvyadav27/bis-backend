package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.RejectRequest;
import com.bis.intelliguide.model.CertificationScheme;
import com.bis.intelliguide.repository.CertificationSchemeRepository;
import com.bis.intelliguide.service.rag.IngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/schemes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSchemesController {

    private final CertificationSchemeRepository schemeRepository;
    private final IngestionService ingestionService;

    @GetMapping
    public Page<CertificationScheme> list(@RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size);
        return status != null ? schemeRepository.findByStatus(status, pageable) : schemeRepository.findAll(pageable);
    }

    @PostMapping
    public CertificationScheme create(@Valid @RequestBody CertificationScheme scheme) {
        scheme.setStatus("DRAFT");
        scheme.setVersion(1);
        return schemeRepository.save(scheme);
    }

    @PutMapping("/{id}")
    public CertificationScheme update(@PathVariable String id, @Valid @RequestBody CertificationScheme updated) {
        CertificationScheme existing = get(id);
        existing.setSchemeName(updated.getSchemeName());
        existing.setDescription(updated.getDescription());
        existing.setEligibility(updated.getEligibility());
        existing.setDocumentsRequired(updated.getDocumentsRequired());
        existing.setProcessSteps(updated.getProcessSteps());
        existing.setEstimatedTimeline(updated.getEstimatedTimeline());
        existing.setVersion(existing.getVersion() + 1);
        return schemeRepository.save(existing);
    }

    @PostMapping("/{id}/submit-for-review")
    public CertificationScheme submitForReview(@PathVariable String id) {
        CertificationScheme s = get(id);
        s.setStatus("PENDING_REVIEW");
        return schemeRepository.save(s);
    }

    @PostMapping("/{id}/publish")
    public CertificationScheme publish(@PathVariable String id) {
        CertificationScheme s = get(id);
        s.setChunks(ingestionService.ingestShared(
                s.getDescription(), s.getSchemeName(), "certification_schemes", s.getId()));
        s.setStatus("PUBLISHED");
        s.setPublishedAt(Instant.now());
        return schemeRepository.save(s);
    }

    @PostMapping("/{id}/reject")
    public CertificationScheme reject(@PathVariable String id, @Valid @RequestBody RejectRequest request) {
        CertificationScheme s = get(id);
        s.setStatus("DRAFT");
        return schemeRepository.save(s);
    }

    @PostMapping("/bulk-import-json")
    public ResponseEntity<?> bulkImportJson(@RequestBody List<CertificationScheme> schemes, Authentication auth) {
        String adminId = auth != null ? (String) auth.getPrincipal() : null;
        int count = 0;

        for (CertificationScheme scheme : schemes) {

            if (scheme.getSchemeName() == null || scheme.getSchemeName().isBlank()) {
                continue;
            }

            String status = (scheme.getStatus() == null || scheme.getStatus().isBlank())
                    ? "DRAFT"
                    : scheme.getStatus().trim().toUpperCase();

            scheme.setStatus(status);
            scheme.setVersion(1);
            scheme.setCreatedBy(adminId);
            scheme.setLastEditedBy(adminId);

            CertificationScheme saved = schemeRepository.save(scheme); // _id assign karne ke liye pehle save

            if (status.equals("PUBLISHED")) {
                saved.setChunks(ingestionService.ingestShared(
                        saved.getDescription(), saved.getSchemeName(), "certification_schemes", saved.getId()));
                saved.setPublishedAt(Instant.now());
                schemeRepository.save(saved);
            }

            count++;
        }

        return ResponseEntity.ok(Map.of("importedCount", count));
    }

    private CertificationScheme get(String id) {
        return schemeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + id));
    }
}
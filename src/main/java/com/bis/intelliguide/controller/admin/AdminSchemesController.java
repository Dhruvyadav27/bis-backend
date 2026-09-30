package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.RejectRequest;
import com.bis.intelliguide.model.CertificationScheme;
import com.bis.intelliguide.model.EntityVersion;
import com.bis.intelliguide.repository.CertificationSchemeRepository;
import com.bis.intelliguide.service.admin.VersionHistoryService;
import com.bis.intelliguide.service.rag.IngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.bis.intelliguide.service.admin.BulkImportService;
import com.bis.intelliguide.service.admin.VersionHistoryService;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/schemes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSchemesController {

    private static final String ENTITY_TYPE = "CERTIFICATION_SCHEME";

    private final CertificationSchemeRepository schemeRepository;
    private final IngestionService ingestionService;
    private final VersionHistoryService versionHistoryService;
    private final BulkImportService bulkImportService;

    @GetMapping
    public Page<CertificationScheme> list(@RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size);
        return status != null ? schemeRepository.findByStatus(status, pageable) : schemeRepository.findAll(pageable);
    }

    @PostMapping
    public CertificationScheme create(@Valid @RequestBody CertificationScheme scheme, Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        scheme.setStatus("DRAFT");
        scheme.setVersion(1);
        scheme.setCreatedBy(adminId);
        scheme.setLastEditedBy(adminId);
        CertificationScheme saved = schemeRepository.save(scheme);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "CREATE", null);
        return saved;
    }

    @PutMapping("/{id}")
    public CertificationScheme update(@PathVariable String id, @Valid @RequestBody CertificationScheme updated, Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        CertificationScheme existing = get(id);
        existing.setSchemeName(updated.getSchemeName());
        existing.setDescription(updated.getDescription());
        existing.setEligibility(updated.getEligibility());
        existing.setDocumentsRequired(updated.getDocumentsRequired());
        existing.setProcessSteps(updated.getProcessSteps());
        existing.setEstimatedTimeline(updated.getEstimatedTimeline());
        existing.setVersion(existing.getVersion() + 1);
        existing.setLastEditedBy(adminId);
        CertificationScheme saved = schemeRepository.save(existing);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "UPDATE", null);
        return saved;
    }

    @PostMapping("/{id}/submit-for-review")
    public CertificationScheme submitForReview(@PathVariable String id) {
        CertificationScheme s = get(id);
        if (!"DRAFT".equals(s.getStatus())) {
            throw new IllegalStateException("Only DRAFT schemes can be submitted for review");
        }
        s.setStatus("PENDING_REVIEW");
        return schemeRepository.save(s);
    }

    @PostMapping("/{id}/publish")
    public CertificationScheme publish(@PathVariable String id, Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        CertificationScheme s = get(id);

        if (!"PENDING_REVIEW".equals(s.getStatus())) {
            throw new IllegalStateException("Only PENDING_REVIEW schemes can be published");
        }
        if (adminId != null && adminId.equals(s.getLastEditedBy())) {
            throw new IllegalStateException("Four-eyes principle: The same admin cannot edit and publish a scheme.");
        }

        s.setChunks(ingestionService.ingestShared(
                s.getDescription(), s.getSchemeName(), "certification_schemes", s.getId()));
        s.setStatus("PUBLISHED");
        s.setPublishedAt(Instant.now());
        CertificationScheme saved = schemeRepository.save(s);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "PUBLISH", null);
        return saved;
    }

    @PostMapping("/{id}/reject")
    public CertificationScheme reject(@PathVariable String id, @Valid @RequestBody RejectRequest request, Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        CertificationScheme s = get(id);
        if (!"PENDING_REVIEW".equals(s.getStatus())) {
            throw new IllegalStateException("Only PENDING_REVIEW schemes can be rejected");
        }
        s.setStatus("DRAFT");
        CertificationScheme saved = schemeRepository.save(s);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "REJECT", request.getReason());
        return saved;
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

            CertificationScheme saved = schemeRepository.save(scheme);
            versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "CREATE", null);

            if (status.equals("PUBLISHED")) {
                saved.setChunks(ingestionService.ingestShared(
                        saved.getDescription(), saved.getSchemeName(), "certification_schemes", saved.getId()));
                saved.setPublishedAt(Instant.now());
                schemeRepository.save(saved);
                versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "PUBLISH", null);
            }

            count++;
        }

        return ResponseEntity.ok(Map.of("importedCount", count));
    }

    @GetMapping("/{id}/history")
    public List<EntityVersion> history(@PathVariable String id) {
        return versionHistoryService.history(ENTITY_TYPE, id);
    }

    @PostMapping("/{id}/restore/{versionNumber}")
    public CertificationScheme restore(@PathVariable String id, @PathVariable int versionNumber, Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        CertificationScheme current = get(id);
        EntityVersion version = versionHistoryService.getVersion(ENTITY_TYPE, id, versionNumber);
        CertificationScheme restored = versionHistoryService.applySnapshot(current, version.getSnapshot(), CertificationScheme.class);

        restored.setId(current.getId());
        restored.setStatus("DRAFT");
        restored.setVersion(current.getVersion() + 1);
        restored.setLastEditedBy(adminId);
        restored.setCreatedBy(current.getCreatedBy());
        restored.setChunks(current.getChunks());

        CertificationScheme saved = schemeRepository.save(restored);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "RESTORE", "Restored from version " + versionNumber);
        return saved;
    }

    @PostMapping("/import")

    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        int count = bulkImportService.importSchemesCsv(file, adminId);
        return ResponseEntity.ok(Map.of("importedCount", count, "message", count + " schemes imported as DRAFT"));
    }

    private CertificationScheme get(String id) {
        return schemeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + id));
    }
}
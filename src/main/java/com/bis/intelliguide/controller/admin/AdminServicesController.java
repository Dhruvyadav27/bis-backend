package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.RejectRequest;
import com.bis.intelliguide.model.BisService;
import com.bis.intelliguide.model.EntityVersion;
import com.bis.intelliguide.repository.BisServiceRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;


@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminServicesController {

    private static final String ENTITY_TYPE = "BIS_SERVICE";

    private final BisServiceRepository bisServiceRepository;
    private final IngestionService ingestionService;
    private final VersionHistoryService versionHistoryService;
    private final BulkImportService bulkImportService;

    @GetMapping
    public Page<BisService> list(@RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size);
        return status != null ? bisServiceRepository.findByStatus(status, pageable) : bisServiceRepository.findAll(pageable);
    }

    @PostMapping
    public BisService create(@Valid @RequestBody BisService service, Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        service.setStatus("DRAFT");
        service.setVersion(1);
        service.setCreatedBy(adminId);
        service.setLastEditedBy(adminId);
        BisService saved = bisServiceRepository.save(service);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "CREATE", null);
        return saved;
    }

    @PutMapping("/{id}")
    public BisService update(@PathVariable String id, @Valid @RequestBody BisService updated, Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        BisService existing = get(id);
        existing.setServiceName(updated.getServiceName());
        existing.setCategory(updated.getCategory());
        existing.setDescription(updated.getDescription());
        existing.setProcedure(updated.getProcedure());
        existing.setVersion(existing.getVersion() + 1);
        existing.setLastEditedBy(adminId);
        BisService saved = bisServiceRepository.save(existing);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "UPDATE", null);
        return saved;
    }

    @PostMapping("/{id}/submit-for-review")
    public BisService submitForReview(@PathVariable String id) {
        BisService s = get(id);
        if (!"DRAFT".equals(s.getStatus())) {
            throw new IllegalStateException("Only DRAFT services can be submitted for review");
        }
        s.setStatus("PENDING_REVIEW");
        return bisServiceRepository.save(s);
    }

    @PostMapping("/{id}/publish")
    public BisService publish(@PathVariable String id, Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        BisService s = get(id);

        if (!"PENDING_REVIEW".equals(s.getStatus())) {
            throw new IllegalStateException("Only PENDING_REVIEW services can be published");
        }
        if (adminId != null && adminId.equals(s.getLastEditedBy())) {
            throw new IllegalStateException("Four-eyes principle: The same admin cannot edit and publish a service.");
        }

        s.setChunks(ingestionService.ingestShared(
                s.getDescription(), s.getServiceName(), "bis_services", s.getId()));
        s.setStatus("PUBLISHED");
        s.setPublishedAt(Instant.now());
        BisService saved = bisServiceRepository.save(s);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "PUBLISH", null);
        return saved;
    }

    @PostMapping("/{id}/reject")
    public BisService reject(@PathVariable String id, @Valid @RequestBody RejectRequest request, Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        BisService s = get(id);
        if (!"PENDING_REVIEW".equals(s.getStatus())) {
            throw new IllegalStateException("Only PENDING_REVIEW services can be rejected");
        }
        s.setStatus("DRAFT");
        BisService saved = bisServiceRepository.save(s);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "REJECT", request.getReason());
        return saved;
    }

    @PostMapping("/bulk-import-json")
    public ResponseEntity<?> bulkImportJson(@RequestBody List<BisService> services, Authentication auth) {
        String adminId = auth != null ? (String) auth.getPrincipal() : null;
        int count = 0;

        for (BisService service : services) {
            if (service.getServiceName() == null || service.getServiceName().isBlank()) {
                continue;
            }

            String status = (service.getStatus() == null || service.getStatus().isBlank())
                    ? "DRAFT"
                    : service.getStatus().trim().toUpperCase();

            service.setStatus(status);
            service.setVersion(1);
            service.setCreatedBy(adminId);
            service.setLastEditedBy(adminId);

            BisService saved = bisServiceRepository.save(service);
            versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "CREATE", null);

            if (status.equals("PUBLISHED")) {
                saved.setChunks(ingestionService.ingestShared(
                        saved.getDescription(), saved.getServiceName(), "bis_services", saved.getId()));
                saved.setPublishedAt(Instant.now());
                bisServiceRepository.save(saved);
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
    public BisService restore(@PathVariable String id, @PathVariable int versionNumber, Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        BisService current = get(id);
        EntityVersion version = versionHistoryService.getVersion(ENTITY_TYPE, id, versionNumber);
        BisService restored = versionHistoryService.applySnapshot(current, version.getSnapshot(), BisService.class);

        restored.setId(current.getId());
        restored.setStatus("DRAFT");
        restored.setVersion(current.getVersion() + 1);
        restored.setLastEditedBy(adminId);
        restored.setCreatedBy(current.getCreatedBy());
        restored.setChunks(current.getChunks());

        BisService saved = bisServiceRepository.save(restored);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "RESTORE", "Restored from version " + versionNumber);
        return saved;
    }

    @PostMapping("/import")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        int count = bulkImportService.importServicesCsv(file, adminId);
        return ResponseEntity.ok(Map.of("importedCount", count, "message", count + " services imported as DRAFT"));
    }

    private BisService get(String id) {
        return bisServiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    }
}
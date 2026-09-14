package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.RejectRequest;
import com.bis.intelliguide.model.BisService;
import com.bis.intelliguide.repository.BisServiceRepository;
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
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminServicesController {

    private final BisServiceRepository bisServiceRepository;
    private final IngestionService ingestionService;

    @GetMapping
    public Page<BisService> list(@RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size);
        return status != null ? bisServiceRepository.findByStatus(status, pageable) : bisServiceRepository.findAll(pageable);
    }

    @PostMapping
    public BisService create(@Valid @RequestBody BisService service) {
        service.setStatus("DRAFT");
        service.setVersion(1);
        return bisServiceRepository.save(service);
    }

    @PutMapping("/{id}")
    public BisService update(@PathVariable String id, @Valid @RequestBody BisService updated) {
        BisService existing = get(id);
        existing.setServiceName(updated.getServiceName());
        existing.setCategory(updated.getCategory());
        existing.setDescription(updated.getDescription());
        existing.setProcedure(updated.getProcedure());
        existing.setVersion(existing.getVersion() + 1);
        return bisServiceRepository.save(existing);
    }

    @PostMapping("/{id}/submit-for-review")
    public BisService submitForReview(@PathVariable String id) {
        BisService s = get(id);
        s.setStatus("PENDING_REVIEW");
        return bisServiceRepository.save(s);
    }

    @PostMapping("/{id}/publish")
    public BisService publish(@PathVariable String id) {
        BisService s = get(id);
        s.setChunks(ingestionService.ingestShared(
                s.getDescription(), s.getServiceName(), "bis_services", s.getId()));
        s.setStatus("PUBLISHED");
        s.setPublishedAt(Instant.now());
        return bisServiceRepository.save(s);
    }

    @PostMapping("/{id}/reject")
    public BisService reject(@PathVariable String id, @Valid @RequestBody RejectRequest request) {
        BisService s = get(id);
        s.setStatus("DRAFT");
        return bisServiceRepository.save(s);
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

            if (status.equals("PUBLISHED")) {
                saved.setChunks(ingestionService.ingestShared(
                        saved.getDescription(), saved.getServiceName(), "bis_services", saved.getId()));
                saved.setPublishedAt(Instant.now());
                bisServiceRepository.save(saved);
            }

            count++;
        }

        return ResponseEntity.ok(Map.of("importedCount", count));
    }

    private BisService get(String id) {
        return bisServiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    }
}
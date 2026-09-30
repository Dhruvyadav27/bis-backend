package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.RejectRequest;
import com.bis.intelliguide.model.Standard;
import com.bis.intelliguide.service.admin.BulkImportService;
import com.bis.intelliguide.service.admin.ContentModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.bis.intelliguide.dto.request.StandardBulkImportRequest;
import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/standards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStandardsController {

    private final ContentModerationService contentModerationService;
    private final BulkImportService bulkImportService;

    @GetMapping
    public Page<Standard> list(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String search,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return contentModerationService.list(status, category, pageable);
    }

    @PostMapping
    public Standard create(@Valid @RequestBody Standard standard, Authentication authentication) {
        return contentModerationService.createDraft(standard, (String) authentication.getPrincipal());
    }
    @PostMapping("/bulk-import-json")
    public ResponseEntity<?> bulkImportJson(@RequestBody List<StandardBulkImportRequest> items,
                                            Authentication authentication) {
        String adminId = authentication != null ? (String) authentication.getPrincipal() : null;
        int count = bulkImportService.importStandardsJson(items, adminId);
        return ResponseEntity.ok(java.util.Map.of("importedCount", count));
    }

    @PutMapping("/{id}")
    public Standard update(@PathVariable String id, @Valid @RequestBody Standard standard, Authentication authentication) {
        return contentModerationService.update(id, standard, (String) authentication.getPrincipal());
    }

    @PostMapping("/{id}/submit-for-review")
    public Standard submitForReview(@PathVariable String id) {
        return contentModerationService.submitForReview(id);
    }

    /** Triggers RAG ingestion. Must be a different admin than the last editor (four-eyes rule). */
    @PostMapping("/{id}/publish")
    public Standard publish(@PathVariable String id, Authentication authentication) {
        return contentModerationService.publish(id, (String) authentication.getPrincipal());
    }

    @PostMapping("/{id}/reject")
    public Standard reject(@PathVariable String id, @Valid @RequestBody RejectRequest request, Authentication authentication) {
        return contentModerationService.reject(id, request.getReason(), (String) authentication.getPrincipal());
    }

    @GetMapping("/{id}/history")
    public List<String> history(@PathVariable String id) {
        // Full build: maintain a dedicated version-history collection keyed by standardId.
        return List.of("Version history not yet persisted separately in this scaffold — "
                + "current `version` field on Standard increments on every update().");
    }

    @PostMapping("/{id}/restore/{versionId}")
    public Standard restore(@PathVariable String id, @PathVariable String versionId) {
        throw new UnsupportedOperationException(
                "Version restore requires the dedicated version-history collection noted in /history — "
                        + "not yet implemented in this scaffold.");
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        int count = bulkImportService.importStandardsCsv(file, (String) authentication.getPrincipal());
        return count + " standards imported as DRAFT";
    }
    @PostMapping("/dedupe")
    public ResponseEntity<?> dedupeByIsNumber() {
        List<Standard> all = contentModerationService.list(null, null, org.springframework.data.domain.Pageable.unpaged()).getContent();

        java.util.Map<String, List<Standard>> grouped = all.stream()
                .filter(s -> s.getIsNumber() != null)
                .collect(java.util.stream.Collectors.groupingBy(Standard::getIsNumber));

        int deletedCount = 0;
        for (List<Standard> group : grouped.values()) {
            if (group.size() > 1) {
                for (int i = 1; i < group.size(); i++) {
                    bulkImportService.deleteStandardById(group.get(i).getId());
                    deletedCount++;
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "deletedDuplicates", deletedCount,
                "remaining", all.size() - deletedCount
        ));
    }
}

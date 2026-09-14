package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.LabBulkImportRequest;
import com.bis.intelliguide.service.admin.BulkImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bulk-import")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBulkImportController {

    private final BulkImportService bulkImportService;

    @PostMapping(
            value = "/pdf",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public BulkImportService.BulkImportResult uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            Authentication authentication
    ) throws IOException {

        String adminId =
                (String) authentication.getPrincipal();

        return bulkImportService.importPdf(
                file,
                type,
                adminId
        );
    }

    @PostMapping("/labs-json")
    public ResponseEntity<?> bulkImportLabsJson(@RequestBody List<LabBulkImportRequest> items) {
        int count = bulkImportService.importLabsJson(items);
        return ResponseEntity.ok(Map.of("importedCount", count));
    }
}

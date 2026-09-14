package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.HuidRecordBulkImportRequest;
import com.bis.intelliguide.model.HuidRecord;
import com.bis.intelliguide.repository.HuidRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/huid-records")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminHuidController {

    private final HuidRecordRepository huidRecordRepository;

    @GetMapping
    public Page<HuidRecord> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return huidRecordRepository.findAll(PageRequest.of(page, size));
    }

    @PostMapping
    public HuidRecord create(@RequestBody HuidRecordBulkImportRequest item) {
        return huidRecordRepository.save(toRecord(item));
    }

    // ===== bulk import =====
    @PostMapping("/bulk-import-json")
    public ResponseEntity<?> bulkImportJson(@RequestBody List<HuidRecordBulkImportRequest> items) {

        List<HuidRecord> toSave = new ArrayList<>();

        for (HuidRecordBulkImportRequest item : items) {

            if (item.getHuid() == null || item.getHuid().isBlank()) {
                continue; // huid is the minimum required field
            }

            toSave.add(toRecord(item));
        }

        huidRecordRepository.saveAll(toSave);

        return ResponseEntity.ok(Map.of("importedCount", toSave.size()));
    }

    private HuidRecord toRecord(HuidRecordBulkImportRequest item) {
        return HuidRecord.builder()
                .huid(item.getHuid())
                .purity(item.getPurity())
                .ahcCentre(item.getAhcCentre())
                .hallmarkedOn(parseDate(item.getHallmarkedOn()))
                .verified(item.isVerified())
                .build();
    }

    private java.time.Instant parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            return null; // invalid date format — leave null instead of failing the whole import
        }
    }
}

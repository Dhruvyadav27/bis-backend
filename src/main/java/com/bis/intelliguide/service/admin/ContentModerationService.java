package com.bis.intelliguide.service.admin;

import com.bis.intelliguide.model.EntityVersion;
import com.bis.intelliguide.model.Standard;
import com.bis.intelliguide.repository.StandardRepository;
import com.bis.intelliguide.service.rag.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private static final String ENTITY_TYPE = "STANDARD";

    private final StandardRepository standardRepository;
    private final IngestionService ingestionService;
    private final VersionHistoryService versionHistoryService;

    public Standard createDraft(Standard standard, String adminId) {
        standard.setStatus("DRAFT");
        standard.setVersion(1);
        standard.setCreatedBy(adminId);
        standard.setLastEditedBy(adminId);
        Standard saved = standardRepository.save(standard);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "CREATE", null);
        return saved;
    }

    public Standard update(String id, Standard updated, String adminId) {
        Standard existing = getOrThrow(id);

        existing.setTitle(updated.getTitle());
        existing.setScope(updated.getScope());
        existing.setRevision(updated.getRevision());
        existing.setCategory(updated.getCategory());
        existing.setDocumentUrl(updated.getDocumentUrl());
        existing.setLastEditedBy(adminId);
        existing.setVersion(existing.getVersion() + 1);
        // status intentionally untouched here — editing does not change lifecycle state
        Standard saved = standardRepository.save(existing);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "UPDATE", null);
        return saved;
    }

    public Standard submitForReview(String id) {
        Standard s = getOrThrow(id);
        if (!"DRAFT".equals(s.getStatus())) {
            throw new IllegalStateException("Only DRAFT standards can be submitted for review");
        }
        s.setStatus("PENDING_REVIEW");
        return standardRepository.save(s);
    }

    public Standard publish(String id, String publishingAdminId) {
        Standard s = getOrThrow(id);
        if (!"PENDING_REVIEW".equals(s.getStatus())) {
            throw new IllegalStateException("Only PENDING_REVIEW standards can be published");
        }
        if (publishingAdminId.equals(s.getLastEditedBy())) {
            throw new IllegalStateException("The admin who last edited this record cannot also publish it (four-eyes rule).");
        }

        // RAG ingestion happens HERE, on publish — never on draft save.
        String rawText = StringUtils.hasText(s.getScope()) ? s.getScope() : s.getTitle();
        s.setChunks(ingestionService.ingest(rawText, s.getIsNumber()));

        s.setStatus("PUBLISHED");
        s.setPublishedAt(Instant.now());
        Standard saved = standardRepository.save(s);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, publishingAdminId, "PUBLISH", null);
        return saved;
    }

    public Standard reject(String id, String reason, String adminId) {
        Standard s = getOrThrow(id);
        if (!"PENDING_REVIEW".equals(s.getStatus())) {
            throw new IllegalStateException("Only PENDING_REVIEW standards can be rejected");
        }
        s.setStatus("DRAFT");
        Standard saved = standardRepository.save(s);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "REJECT", reason);
        return saved;
    }

    public Standard restore(String id, int versionNumber, String adminId) {
        Standard current = getOrThrow(id);
        EntityVersion version = versionHistoryService.getVersion(ENTITY_TYPE, id, versionNumber);
        Standard restored = versionHistoryService.applySnapshot(current, version.getSnapshot(), Standard.class);

        restored.setId(current.getId());
        restored.setStatus("DRAFT"); // restoring content re-enters the draft->review->publish gate
        restored.setVersion(current.getVersion() + 1);
        restored.setLastEditedBy(adminId);
        restored.setCreatedBy(current.getCreatedBy());
        restored.setChunks(current.getChunks()); // untouched until next real publish

        Standard saved = standardRepository.save(restored);
        versionHistoryService.snapshot(ENTITY_TYPE, saved.getId(), saved.getVersion(), saved, adminId, "RESTORE", "Restored from version " + versionNumber);
        return saved;
    }

    public List<EntityVersion> history(String id) {
        return versionHistoryService.history(ENTITY_TYPE, id);
    }

    public Page<Standard> list(String status, String category, Pageable pageable) {
        if (StringUtils.hasText(status) && StringUtils.hasText(category)) {
            return standardRepository.findByStatusAndCategory(status, category, pageable);
        }
        if (StringUtils.hasText(status)) {
            return standardRepository.findByStatus(status, pageable);
        }
        return standardRepository.findAll(pageable);
    }

    private Standard getOrThrow(String id) {
        return standardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Standard not found: " + id));
    }
}
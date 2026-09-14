package com.bis.intelliguide.service.admin;

import com.bis.intelliguide.model.Standard;
import com.bis.intelliguide.repository.StandardRepository;
import com.bis.intelliguide.service.rag.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Enforces the draft -> pending_review -> published lifecycle at the SERVICE layer
 * (not just hidden in the UI), and triggers RAG ingestion only on publish. Also
 * enforces the four-eyes rule: the admin who publishes must differ from the admin
 * who last edited the record.
 *
 * Shown here fully wired for Standards; CertificationScheme and BisService follow
 * the identical pattern (same status field + same publish-triggers-ingestion rule).
 */
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private final StandardRepository standardRepository;
    private final IngestionService ingestionService;

    public Standard createDraft(Standard standard, String adminId) {
        standard.setStatus("DRAFT");
        standard.setVersion(1);
        standard.setCreatedBy(adminId);
        standard.setLastEditedBy(adminId);
        return standardRepository.save(standard);
    }

    public Standard update(String id, Standard updated, String adminId) {
        Standard existing = standardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Standard not found: " + id));

        existing.setTitle(updated.getTitle());
        existing.setScope(updated.getScope());
        existing.setRevision(updated.getRevision());
        existing.setCategory(updated.getCategory());
        existing.setDocumentUrl(updated.getDocumentUrl());
        existing.setLastEditedBy(adminId);
        existing.setVersion(existing.getVersion() + 1);
        // status intentionally untouched here — editing does not change lifecycle state
        return standardRepository.save(existing);
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
        return standardRepository.save(s);
    }

    public Standard reject(String id, String reason) {
        Standard s = getOrThrow(id);
        if (!"PENDING_REVIEW".equals(s.getStatus())) {
            throw new IllegalStateException("Only PENDING_REVIEW standards can be rejected");
        }
        s.setStatus("DRAFT");
        // In a full build: store `reason` in a review-history sub-list on the document.
        return standardRepository.save(s);
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

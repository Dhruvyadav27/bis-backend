package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.RejectRequest;
import com.bis.intelliguide.model.ConsumerRule;
import com.bis.intelliguide.repository.ConsumerRuleRepository;
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
@RequestMapping("/api/admin/consumer-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConsumerRulesController {

    private final ConsumerRuleRepository consumerRuleRepository;
    private final IngestionService ingestionService;

    @GetMapping
    public Page<ConsumerRule> list(@RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size);
        return status != null ? consumerRuleRepository.findByStatus(status, pageable) : consumerRuleRepository.findAll(pageable);
    }

    @PostMapping
    public ConsumerRule create(@Valid @RequestBody ConsumerRule rule, Authentication auth) {
        rule.setStatus("DRAFT");
        rule.setVersion(1);
        rule.setCreatedBy((String) auth.getPrincipal());
        rule.setLastEditedBy((String) auth.getPrincipal());
        return consumerRuleRepository.save(rule);
    }

    @PutMapping("/{id}")
    public ConsumerRule update(@PathVariable String id, @Valid @RequestBody ConsumerRule updated) {
        ConsumerRule existing = get(id);
        existing.setActName(updated.getActName());
        existing.setClauseRef(updated.getClauseRef());
        existing.setTopic(updated.getTopic());
        existing.setDescription(updated.getDescription());
        existing.setApplicableAction(updated.getApplicableAction());
        existing.setVersion(existing.getVersion() + 1);
        return consumerRuleRepository.save(existing);
    }

    @PostMapping("/{id}/submit-for-review")
    public ConsumerRule submitForReview(@PathVariable String id) {
        ConsumerRule r = get(id);
        r.setStatus("PENDING_REVIEW");
        return consumerRuleRepository.save(r);
    }

    @PostMapping("/{id}/publish")
    public ConsumerRule publish(@PathVariable String id) {
        ConsumerRule r = get(id);
        String text = r.getActName() + " " + r.getClauseRef() + " — " + r.getDescription()
                + (r.getApplicableAction() != null ? " Action: " + r.getApplicableAction() : "");
        r.setChunks(ingestionService.ingestShared(
                text, r.getActName() + " " + r.getClauseRef(), "consumer_protection", r.getId()));
        r.setStatus("PUBLISHED");
        r.setPublishedAt(Instant.now());
        return consumerRuleRepository.save(r);
    }

    @PostMapping("/{id}/reject")
    public ConsumerRule reject(@PathVariable String id, @Valid @RequestBody RejectRequest request) {
        ConsumerRule r = get(id);
        r.setStatus("DRAFT");
        return consumerRuleRepository.save(r);
    }

    @PostMapping("/bulk-import-json")
    public ResponseEntity<?> bulkImportJson(@RequestBody List<ConsumerRule> rules, Authentication auth) {
        String adminId = auth != null ? (String) auth.getPrincipal() : null;
        int count = 0;

        for (ConsumerRule rule : rules) {

            if (rule.getActName() == null || rule.getActName().isBlank()) {
                continue;
            }

            String status = (rule.getStatus() == null || rule.getStatus().isBlank())
                    ? "DRAFT"
                    : rule.getStatus().trim().toUpperCase();

            rule.setStatus(status);
            rule.setVersion(1);
            rule.setCreatedBy(adminId);
            rule.setLastEditedBy(adminId);

            ConsumerRule saved = consumerRuleRepository.save(rule);

            if (status.equals("PUBLISHED")) {
                String text = saved.getActName() + " " + saved.getClauseRef() + " — " + saved.getDescription()
                        + (saved.getApplicableAction() != null ? " Action: " + saved.getApplicableAction() : "");
                saved.setChunks(ingestionService.ingestShared(
                        text, saved.getActName() + " " + saved.getClauseRef(), "consumer_protection", saved.getId()));
                saved.setPublishedAt(Instant.now());
                consumerRuleRepository.save(saved);
            }

            count++;
        }

        return ResponseEntity.ok(Map.of("importedCount", count));
    }

    private ConsumerRule get(String id) {
        return consumerRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));
    }
}

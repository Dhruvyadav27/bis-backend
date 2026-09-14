package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.FlaggedAnswerResolveRequest;
import com.bis.intelliguide.dto.response.FlaggedAnswerDto;
import com.bis.intelliguide.model.AiAnswerLog;
import com.bis.intelliguide.repository.AiAnswerLogRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consolidated low-confidence review queue — across ALL agents (Standard Finder,
 * Certification Guide, Consumer Affairs general-query mode, Assistant, etc.), since
 * they all log to the same ai_answer_logs collection via GenerationService/agent services.
 */
@RestController
@RequestMapping("/api/admin/flagged-answers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFlaggedAnswersController {

    private final AiAnswerLogRepository aiAnswerLogRepository;

    @GetMapping
    public List<FlaggedAnswerDto> list(@RequestParam(defaultValue = "0.0") double minConfidence,
                                        @RequestParam(defaultValue = "1.0") double maxConfidence) {
        return aiAnswerLogRepository.findByConfidenceScoreBetween(minConfidence, maxConfidence, PageRequest.of(0, 100))
                .stream()
                .map(l -> FlaggedAnswerDto.builder()
                        .id(l.getId())
                        .question(l.getQuestion())
                        .agent(l.getAgent())
                        .confidence(l.getConfidenceScore())
                        .answer(l.getAnswer())
                        .source(l.getSourceRef())
                        .build())
                .toList();
    }

    @PostMapping("/{id}/resolve")
    public AiAnswerLog resolve(@PathVariable String id, @Valid @RequestBody FlaggedAnswerResolveRequest request) {
        AiAnswerLog log = aiAnswerLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Answer log not found: " + id));

        switch (request.getAction()) {
            case "mark_correct" -> { log.setReviewStatus("CORRECT"); log.setFlagged(false); }
            case "needs_update" -> log.setReviewStatus("NEEDS_UPDATE");
            case "edit" -> { log.setReviewStatus("EDITED"); log.setAnswer(request.getNote()); }
            default -> throw new IllegalArgumentException("Unknown action: " + request.getAction());
        }
        log.setReviewNote(request.getNote());
        return aiAnswerLogRepository.save(log);
    }
}

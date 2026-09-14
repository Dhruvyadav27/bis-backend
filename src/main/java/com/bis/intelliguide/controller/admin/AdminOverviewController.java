package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.response.AdminOverviewResponse;
import com.bis.intelliguide.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/admin/overview")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOverviewController {

    private final StandardRepository standardRepository;
    private final BisDocumentRepository bisDocumentRepository;
    private final CertificationSchemeRepository certificationSchemeRepository;
    private final BisServiceRepository bisServiceRepository;
    private final AiAnswerLogRepository aiAnswerLogRepository;

    @GetMapping
    public AdminOverviewResponse overview() {
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        long flaggedThisWeek = aiAnswerLogRepository.findAll().stream()
                .filter(l -> l.isFlagged() && l.getCreatedAt() != null && l.getCreatedAt().isAfter(weekAgo))
                .count();

        return AdminOverviewResponse.builder()
                .totalStandards(standardRepository.count())
                .totalDocuments(bisDocumentRepository.count())
                .totalSchemes(certificationSchemeRepository.count())
                .totalServices(bisServiceRepository.count())
                .totalQueries(aiAnswerLogRepository.count())
                .flaggedThisWeek(flaggedThisWeek)
                .build();
    }
}

package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.dto.request.CertificationRecommendRequest;
import com.bis.intelliguide.dto.response.CertificationRecommendResponse;
import com.bis.intelliguide.dto.response.CitationRef;
import com.bis.intelliguide.model.AiAnswerLog;
import com.bis.intelliguide.repository.AiAnswerLogRepository;
import com.bis.intelliguide.service.rag.ConfidenceScorer;
import com.bis.intelliguide.service.rag.RetrievalService;
import com.bis.intelliguide.service.rag.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificationGuideService {

    private final RetrievalService retrievalService;
    private final ConfidenceScorer confidenceScorer;
    private final AiAnswerLogRepository aiAnswerLogRepository;

    public CertificationRecommendResponse recommend(CertificationRecommendRequest request, String userId) {
        String query = request.getProductType() + " certification for " +
                (request.getManufacturerType() != null ? request.getManufacturerType() : "manufacturer");

        List<RetrievedChunk> chunks = retrievalService.retrieve(query, "certification_schemes");
        double bestScore = confidenceScorer.bestScore(chunks);
        boolean insufficient = chunks.isEmpty() || !confidenceScorer.isAboveRetrievalFloor(bestScore);

        aiAnswerLogRepository.save(AiAnswerLog.builder()
                .userId(userId).question(query).agent("CERTIFICATION_GUIDE")
                .confidenceScore(bestScore)
                .sourceRef(chunks.isEmpty() ? null : chunks.get(0).getClauseRef())
                .flagged(insufficient || bestScore < confidenceScorer.getModerateThreshold())
                .reviewStatus("UNREVIEWED").createdAt(Instant.now()).build());

        if (insufficient) {
            return CertificationRecommendResponse.builder()
                    .insufficientEvidence(true)
                    .reason("No verified certification scheme could be matched confidently for this product/manufacturer combination.")
                    .build();
        }

        boolean isMsme = "MSME".equalsIgnoreCase(request.getManufacturerType());
        String scheme = isMsme ? "Scheme-I (Simplified Procedure for MSME)" : "Scheme-I (Normal Procedure)";

        return CertificationRecommendResponse.builder()
                .recommendedScheme(scheme)
                .reason("Based on the product category and manufacturer type, " + scheme
                        + " is the applicable BIS certification route.")
                .processSteps(List.of(
                        new CertificationRecommendResponse.StepDto(1, "Apply online via BIS CARE / Manak Online portal"),
                        new CertificationRecommendResponse.StepDto(2, "Sample testing at a BIS-recognized lab"),
                        new CertificationRecommendResponse.StepDto(3, "Factory inspection (if applicable)"),
                        new CertificationRecommendResponse.StepDto(4, "Grant of license and ISI mark usage")
                ))
                .documentsRequired(List.of(
                        "Udyam Registration Certificate (if MSME)",
                        "Test report from recognized lab",
                        "Factory layout and manufacturing process details",
                        "Proof of ownership/authorization"
                ))
                .estimatedTimeline("4-6 weeks (subject to test/inspection scheduling)")
                .references(chunks.stream()
                        .map(c -> CitationRef.builder().doc(c.getSourceId()).clause(c.getClauseRef()).build())
                        .toList())
                .insufficientEvidence(false)
                .build();
    }
}

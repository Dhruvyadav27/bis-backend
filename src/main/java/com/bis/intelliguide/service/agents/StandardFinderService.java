package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.dto.response.StandardMatch;
import com.bis.intelliguide.dto.response.StandardSearchResponse;
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
public class StandardFinderService {

    private final RetrievalService retrievalService;
    private final ConfidenceScorer confidenceScorer;
    private final AiAnswerLogRepository aiAnswerLogRepository;

    public StandardSearchResponse search(String productDescription, String userId) {
        List<RetrievedChunk> chunks = retrievalService.retrieve(productDescription, "standards");
        double bestScore = confidenceScorer.bestScore(chunks);

        boolean insufficient = chunks.isEmpty() || !confidenceScorer.isAboveRetrievalFloor(bestScore);

        logAnswer(userId, productDescription, "STANDARD_FINDER", bestScore,
                chunks.isEmpty() ? null : chunks.get(0).getClauseRef(), insufficient);

        if (insufficient) {
            return StandardSearchResponse.builder()
                    .results(List.of())
                    .insufficientEvidence(true)
                    .message("No verified standard could be matched confidently for this description. "
                            + "This has been flagged for admin review — try a more specific product description.")
                    .build();
        }

        List<StandardMatch> matches = chunks.stream()
                .map(c -> StandardMatch.builder()
                        .isNumber(extractIsNumber(c.getClauseRef()))
                        .title(c.getText())
                        .matchScore(c.getScore())
                        .confidenceLabel(confidenceScorer.label(c.getScore()))
                        .clause(c.getClauseRef())
                        .build())
                .toList();

        return StandardSearchResponse.builder()
                .results(matches)
                .insufficientEvidence(false)
                .message(null)
                .build();
    }

    private String extractIsNumber(String clauseRef) {
        if (clauseRef == null) return "N/A";
        return clauseRef.split(" ")[0] + " " + (clauseRef.split(" ").length > 1 ? clauseRef.split(" ")[1] : "");
    }

    private void logAnswer(String userId, String question, String agent, double score, String sourceRef, boolean flagged) {
        aiAnswerLogRepository.save(AiAnswerLog.builder()
                .userId(userId)
                .question(question)
                .agent(agent)
                .confidenceScore(score)
                .sourceRef(sourceRef)
                .answer(null)
                .flagged(flagged || score < confidenceScorer.getModerateThreshold())
                .reviewStatus("UNREVIEWED")
                .createdAt(Instant.now())
                .build());
    }
}

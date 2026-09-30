package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.dto.response.StandardMatch;
import com.bis.intelliguide.dto.response.StandardSearchResponse;
import com.bis.intelliguide.model.AiAnswerLog;
import com.bis.intelliguide.model.Standard;
import com.bis.intelliguide.repository.AiAnswerLogRepository;
import com.bis.intelliguide.repository.StandardRepository;
import com.bis.intelliguide.service.rag.ConfidenceScorer;
import com.bis.intelliguide.service.rag.GenerationResult;
import com.bis.intelliguide.service.rag.GenerationService;
import com.bis.intelliguide.service.rag.RetrievalService;
import com.bis.intelliguide.service.rag.RetrievedChunk;
import com.bis.intelliguide.service.translate.SarvamTranslateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StandardFinderService {

    private final RetrievalService retrievalService;
    private final ConfidenceScorer confidenceScorer;
    private final AiAnswerLogRepository aiAnswerLogRepository;
    private final StandardRepository standardRepository;
    private final GenerationService generationService;
    private final SarvamTranslateService translator;

    public StandardSearchResponse search(String productDescription, String userId) {
        // Retrieval runs in English; the person's own-language query is translated once here.
        String englishDescription = translator.toEnglish(productDescription);

        List<RetrievedChunk> chunks = retrievalService.retrieve(englishDescription, "standards");
        double bestScore = confidenceScorer.bestScore(chunks);

        boolean insufficient = chunks.isEmpty() || !confidenceScorer.isAboveRetrievalFloor(bestScore);

        logAnswer(userId, productDescription, bestScore,
                chunks.isEmpty() ? null : chunks.get(0).getClauseRef(), insufficient);

        if (insufficient) {
            return StandardSearchResponse.builder()
                    .results(List.of())
                    .insufficientEvidence(true)
                    .message(translator.fromEnglish(
                            "No verified standard could be matched confidently for this description. "
                                    + "This has been flagged for admin review — try a more specific product description."))
                    .build();
        }

        // Dedupe: multiple chunks can point at the same Standard (same sourceId).
        // Keep the highest-scoring chunk per standard, preserve score-descending order.
        Map<String, RetrievedChunk> bestChunkPerStandard = new LinkedHashMap<>();
        for (RetrievedChunk c : chunks) {
            RetrievedChunk existing = bestChunkPerStandard.get(c.getSourceId());
            if (existing == null || c.getScore() > existing.getScore()) {
                bestChunkPerStandard.put(c.getSourceId(), c);
            }
        }

        List<StandardMatch> matches = bestChunkPerStandard.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .map(this::toStandardMatch)
                .filter(m -> m != null)
                .toList();

        if (matches.isEmpty()) {
            // Chunks existed but their parent Standard docs couldn't be resolved
            // (shouldn't normally happen, but fail safe rather than return empty results silently)
            return StandardSearchResponse.builder()
                    .results(List.of())
                    .insufficientEvidence(true)
                    .message(translator.fromEnglish(
                            "Matched content could not be linked back to a published standard. Flagged for review."))
                    .build();
        }

        // Spec step 6: Gemini generates a plain-language explanation of the top match.
        // Built from the English description so it lines up with what was actually retrieved;
        // generateGroundedAnswer translates the answer back to the person's language itself.
        StandardMatch top = matches.get(0);
        String explanationQuery = "Explain in simple, plain language why standard " + top.getIsNumber()
                + " (" + top.getTitle() + ") applies to this product: \"" + englishDescription + "\". "
                + "Mention whether certification against it is compulsory.";
        GenerationResult genResult = generationService.generateGroundedAnswer(explanationQuery, "standards");

        return StandardSearchResponse.builder()
                .results(matches)
                .insufficientEvidence(false)
                .message(null)
                .explanation(genResult.getAnswer())
                .build();
    }

    private StandardMatch toStandardMatch(RetrievedChunk chunk) {
        Optional<Standard> standardOpt = standardRepository.findById(chunk.getSourceId());
        if (standardOpt.isEmpty()) {
            return null;
        }
        Standard s = standardOpt.get();
        return StandardMatch.builder()
                .isNumber(s.getIsNumber())
                .title(s.getTitle())
                .matchScore(chunk.getScore())
                .confidenceLabel(confidenceScorer.label(chunk.getScore()))
                .clause(chunk.getClauseRef())
                .isCompulsory(s.isCompulsory())
                .regulatoryType(s.getRegulatoryType())
                .build();
    }

    private void logAnswer(String userId, String question, double score, String sourceRef, boolean flagged) {
        aiAnswerLogRepository.save(AiAnswerLog.builder()
                .userId(userId)
                .question(question)
                .agent("STANDARD_FINDER")
                .confidenceScore(score)
                .sourceRef(sourceRef)
                .answer(null)
                .flagged(flagged || score < confidenceScorer.getModerateThreshold())
                .reviewStatus("UNREVIEWED")
                .createdAt(Instant.now())
                .build());
    }
}
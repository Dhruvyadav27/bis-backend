package com.bis.intelliguide.service.rag;

import com.bis.intelliguide.dto.response.CitationRef;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The single choke point through which every agent must pass before returning a
 * natural-language answer to the user. Enforces:
 *   1. No ungrounded answers — refuses to call the LLM at all if retrieval is weak.
 *   2. Confidence is computed from retrieval, not the LLM.
 *   3. Citations always attached from the retrieved chunks, not invented.
 */
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final RetrievalService retrievalService;
    private final ConfidenceScorer confidenceScorer;
    private final ChatLanguageModel chatLanguageModel;

    public GenerationResult generateGroundedAnswer(String query, String collection) {
        List<RetrievedChunk> chunks = retrievalService.retrieve(query, collection);

        double bestScore = confidenceScorer.bestScore(chunks);

        if (chunks.isEmpty() || !confidenceScorer.isAboveRetrievalFloor(bestScore)) {
            return GenerationResult.builder()
                    .answer("I could not find verified information in the BIS knowledge base to answer this "
                            + "confidently. Please rephrase your question, or this has been flagged for admin review.")
                    .citations(List.of())
                    .confidenceLabel("needs-verification")
                    .confidenceScore(bestScore)
                    .insufficientEvidence(true)
                    .build();
        }

        String context = chunks.stream()
                .map(c -> "- (" + c.getClauseRef() + ") " + c.getText())
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are a BIS (Bureau of Indian Standards) assistant. Answer the user's question
                using ONLY the context below. Do not use any outside knowledge. If the context is
                insufficient to fully answer, say so explicitly rather than guessing. Keep the
                answer simple and in plain language for a non-expert (MSME/consumer) audience.

                Context:
                %s

                Question: %s

                Answer:
                """.formatted(context, query);

        String llmAnswer;
        try {
            llmAnswer = chatLanguageModel.generate(prompt);
        } catch (Exception e) {
        e.printStackTrace(); // TEMPORARY — asli error dekhne ke liye, baad mein hata dena
        llmAnswer = "[LLM not configured] Based on retrieved context: " + chunks.get(0).getText();
        }

        List<CitationRef> citations = chunks.stream()
                .map(c -> CitationRef.builder().doc(c.getSourceId()).clause(c.getClauseRef()).build())
                .distinct()
                .toList();

        return GenerationResult.builder()
                .answer(llmAnswer)
                .citations(citations)
                .confidenceLabel(confidenceScorer.label(bestScore))
                .confidenceScore(bestScore)
                .insufficientEvidence(false)
                .build();
    }
}

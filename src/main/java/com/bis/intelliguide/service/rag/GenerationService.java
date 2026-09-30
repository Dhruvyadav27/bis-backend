package com.bis.intelliguide.service.rag;

import com.bis.intelliguide.dto.response.CitationRef;
import com.bis.intelliguide.model.BisService;
import com.bis.intelliguide.model.CertificationScheme;
import com.bis.intelliguide.model.ConsumerRule;
import com.bis.intelliguide.model.Standard;
import com.bis.intelliguide.repository.BisServiceRepository;
import com.bis.intelliguide.repository.CertificationSchemeRepository;
import com.bis.intelliguide.repository.ConsumerRuleRepository;
import com.bis.intelliguide.repository.StandardRepository;
import com.bis.intelliguide.service.translate.SarvamTranslateService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Every LLM answer passes through here. The LLM and retrieval always work in English;
 * Sarvam translates the user's question in and the final answer out.
 */
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final RetrievalService retrievalService;
    private final ConfidenceScorer confidenceScorer;
    private final ChatLanguageModel chatLanguageModel;
    private final StandardRepository standardRepository;
    private final CertificationSchemeRepository schemeRepository;
    private final BisServiceRepository bisServiceRepository;
    private final ConsumerRuleRepository consumerRuleRepository;
    private final SarvamTranslateService translator;

    private static final String INSUFFICIENT_EN =
            "I could not find verified information in the BIS knowledge base to answer this "
                    + "confidently. Please rephrase your question, or this has been flagged for admin review.";

    private static final String GENERAL_SYSTEM_PROMPT = """
            You are a helpful assistant for the Bureau of Indian Standards (BIS) intelligent agent platform.
            Answer general, conceptual, or how-to questions about BIS processes, Indian Standards (IS),
            hallmarking, certification schemes, and consumer rights using your own general knowledge.

            Rules:
            - You may explain concepts, definitions, and general "how does X work" / "how do I do Y" questions.
            - NEVER invent a specific IS number, clause number, fee amount, or deadline — if the user needs
              an exact figure, tell them to use the relevant agent (Standard Finder, Certification Guide,
              Hallmarking, or Consumer Affairs) to get a verified answer instead.
            - Keep answers simple, plain-language, and a few sentences long.
            - If you are not confident about something BIS-specific, say so honestly.
            """;

    // ===== Gemini call with retry on transient errors =====

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 1200;

    private String callLlm(String prompt) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return chatLanguageModel.generate(prompt);
            } catch (Exception e) {
                lastError = (e instanceof RuntimeException re) ? re : new RuntimeException(e);
                if (!isTransientError(e) || attempt == MAX_ATTEMPTS) {
                    e.printStackTrace(); // keep for now — remove once things are stable
                    throw lastError;
                }
                System.out.println("[Gemini] Transient error on attempt " + attempt + "/" + MAX_ATTEMPTS
                        + ", retrying: " + e.getMessage());
                try {
                    Thread.sleep(BASE_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw lastError;
    }

    private boolean isTransientError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        return msg.contains("UNAVAILABLE") || msg.contains("503")
                || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("429")
                || msg.contains("DEADLINE_EXCEEDED");
    }

    /** English on purpose: it goes through translator.fromEnglish() like every other answer. */
    private String llmFailureMessage(Exception e) {
        return isTransientError(e)
                ? "Gemini is temporarily overloaded with high demand right now. Please try again in a few seconds."
                : "The AI explanation could not be generated right now due to a technical issue. The verified data on this page is still accurate.";
    }

    private String resolveDisplayName(String collection, String sourceId) {
        try {
            return switch (collection) {
                case "standards" -> standardRepository.findById(sourceId)
                        .map(Standard::getIsNumber).orElse(sourceId);
                case "certification_schemes" -> schemeRepository.findById(sourceId)
                        .map(CertificationScheme::getSchemeName).orElse(sourceId);
                case "bis_services" -> bisServiceRepository.findById(sourceId)
                        .map(BisService::getServiceName).orElse(sourceId);
                case "consumer_protection" -> consumerRuleRepository.findById(sourceId)
                        .map(ConsumerRule::getActName).orElse(sourceId);
                default -> sourceId;
            };
        } catch (Exception e) {
            return sourceId;
        }
    }

    // ===== Public methods =====

    public GenerationResult generateGroundedAnswer(String query, String collection) {
        String englishQuery = translator.toEnglish(query);
        List<RetrievedChunk> chunks = retrievalService.retrieve(englishQuery, collection);

        double bestScore = confidenceScorer.bestScore(chunks);

        if (chunks.isEmpty() || !confidenceScorer.isAboveRetrievalFloor(bestScore)) {
            return GenerationResult.builder()
                    .answer(translator.fromEnglish(INSUFFICIENT_EN))
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
                """.formatted(context, englishQuery);

        String englishAnswer;
        try {
            englishAnswer = callLlm(prompt);
        } catch (Exception e) {
            englishAnswer = llmFailureMessage(e) + "\n\n" + chunks.get(0).getText();
        }

        List<CitationRef> citations = chunks.stream()
                .map(c -> CitationRef.builder()
                        .doc(resolveDisplayName(collection, c.getSourceId()))
                        .clause(c.getClauseRef())
                        .build())
                .distinct()
                .toList();

        return GenerationResult.builder()
                .answer(translator.fromEnglish(englishAnswer))
                .citations(citations)
                .confidenceLabel(confidenceScorer.label(bestScore))
                .confidenceScore(bestScore)
                .insufficientEvidence(false)
                .build();
    }

    /**
     * For callers that already hold a verified, PUBLISHED record (e.g. a matched scheme) — skips
     * retrieval and confidence-gating. Returns the answer in the user's language.
     */
    public String generateFromKnownData(String systemInstruction, String structuredData, String userQuestion) {
        String englishQuestion = userQuestion == null ? null : translator.toEnglish(userQuestion);

        String prompt = """
                %s

                Verified data:
                %s

                %s

                Answer:
                """.formatted(systemInstruction, structuredData,
                englishQuestion != null ? "Question: " + englishQuestion : "");

        try {
            return translator.fromEnglish(callLlm(prompt));
        } catch (Exception e) {
            // Only the short notice is translated; the raw verified data stays as stored.
            return translator.fromEnglish(llmFailureMessage(e)) + "\n\n" + structuredData;
        }
    }

    /** General / conceptual questions that need no citation ("what is hallmarking?"). */
    public GenerationResult generateGeneralKnowledgeAnswer(String query) {
        String englishQuery = translator.toEnglish(query);
        String prompt = GENERAL_SYSTEM_PROMPT + "\n\nQuestion: " + englishQuery + "\n\nAnswer:";

        String englishAnswer;
        try {
            englishAnswer = callLlm(prompt);
        } catch (Exception e) {
            englishAnswer = llmFailureMessage(e);
        }
        return GenerationResult.builder()
                .answer(translator.fromEnglish(englishAnswer))
                .citations(List.of())
                .confidenceLabel("general-knowledge")
                .confidenceScore(0)
                .insufficientEvidence(false)
                .build();
    }
}
package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.dto.request.AssistantQueryRequest;
import com.bis.intelliguide.dto.response.AssistantResponse;
import com.bis.intelliguide.service.rag.GenerationResult;
import com.bis.intelliguide.service.rag.GenerationService;
import com.bis.intelliguide.service.translate.SarvamTranslateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssistantRouterService {

    private final GenerationService generationService;
    private final SarvamTranslateService translator;

    private static final List<String> GENERAL_TRIGGERS = List.of(
            "what is", "what's", "what are", "how do i", "how can i", "how does",
            "how to", "explain", "define", "why do", "why does", "tell me about"
    );

    public AssistantResponse handle(AssistantQueryRequest request) {
        String english = translator.toEnglish(request.getQuery());   // routing works on English
        String q = english.toLowerCase();
        String currentAgent = request.getContext() != null ? request.getContext().getCurrentAgent() : null;

        String targetCollection = "standards";
        String suggestedRoute = null;
        String suggestedLabel = null;

        if (q.contains("complaint") || q.contains("fake") || q.contains("counterfeit") || q.contains("consumer")) {
            targetCollection = "consumer_protection";
            suggestedRoute = "/consumer-affairs";
            suggestedLabel = "Open Consumer Affairs";
        } else if (q.contains("certif") || q.contains("license") || q.contains("scheme")) {
            targetCollection = "certification_schemes";
            suggestedRoute = "/certification-guide";
            suggestedLabel = "Open Certification Guide";
        } else if (q.contains("hallmark") || q.contains("huid") || q.contains("gold") || q.contains("jewel")) {
            targetCollection = "bis_services";
            suggestedRoute = "/hallmarking";
            suggestedLabel = "Open Hall Marking";
        } else if (q.contains("lab") || q.contains("test")) {
            suggestedRoute = "/find-lab";
            suggestedLabel = "Open Find Lab";
        }

        boolean shouldRedirect = suggestedRoute != null && !suggestedRoute.equals(currentAgent);
        boolean looksGeneral = GENERAL_TRIGGERS.stream().anyMatch(q::contains);

        GenerationResult result;
        if (looksGeneral) {
            result = generationService.generateGeneralKnowledgeAnswer(english);
        } else {
            result = generationService.generateGroundedAnswer(english, targetCollection);
            if (result.isInsufficientEvidence()) {
                result = generationService.generateGeneralKnowledgeAnswer(english);
            }
        }

        return AssistantResponse.builder()
                .answer(result.getAnswer())
                .suggestedAction(shouldRedirect
                        ? new AssistantResponse.SuggestedAction(translator.fromEnglish(suggestedLabel), suggestedRoute)
                        : null)
                .shouldRedirect(shouldRedirect)
                .build();
    }
}
package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.dto.request.AssistantQueryRequest;
import com.bis.intelliguide.dto.response.AssistantResponse;
import com.bis.intelliguide.service.rag.GenerationResult;
import com.bis.intelliguide.service.rag.GenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Reactive router + context-aware floating widget backend. Classifies the free-form
 * query to the right worker-agent domain (very simple keyword routing here — swap for
 * an LLM-based classifier prompt later if needed) and answers in-context, or suggests
 * navigating to a different agent if it's out of scope for the current page.
 */
@Service
@RequiredArgsConstructor
public class AssistantRouterService {

    private final GenerationService generationService;

    public AssistantResponse handle(AssistantQueryRequest request) {
        String q = request.getQuery().toLowerCase();
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

        GenerationResult result = generationService.generateGroundedAnswer(request.getQuery(), targetCollection);

        return AssistantResponse.builder()
                .answer(result.getAnswer())
                .suggestedAction(shouldRedirect
                        ? new AssistantResponse.SuggestedAction(suggestedLabel, suggestedRoute)
                        : null)
                .shouldRedirect(shouldRedirect)
                .build();
    }
}

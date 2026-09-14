package com.bis.intelliguide.service.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Confidence is ALWAYS derived from retrieval similarity scores, never invented
 * by the LLM. This enforces core principle #2 from the project brief.
 */
@Component
public class ConfidenceScorer {

    @Value("${app.rag.confidence.high-threshold}")
    private double highThreshold;

    @Value("${app.rag.confidence.moderate-threshold}")
    private double moderateThreshold;

    public double bestScore(List<RetrievedChunk> chunks) {
        return chunks.stream().mapToDouble(RetrievedChunk::getScore).max().orElse(0.0);
    }

    public String label(double score) {
        if (score >= highThreshold) return "high";
        if (score >= moderateThreshold) return "moderate";
        return "needs-verification";
    }

    public boolean isAboveRetrievalFloor(double score) {
        // Below the moderate threshold we don't consider retrieval "successful" enough
        // to even attempt an LLM-generated answer — see RetrievalService.
        return score >= moderateThreshold;
    }

    public double getModerateThreshold() {
        return moderateThreshold;
    }

    public double getHighThreshold() {
        return highThreshold;
    }
}

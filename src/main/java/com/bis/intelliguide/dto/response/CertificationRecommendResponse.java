package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CertificationRecommendResponse {
    private String recommendedScheme;
    private String reason;
    private List<StepDto> processSteps;
    private List<String> documentsRequired;
    private String estimatedTimeline;
    private List<CitationRef> references;
    private boolean insufficientEvidence;

    private MatchedScheme matchedScheme;
    private String friendlyExplanation;
    private String matchReason;
    private String confidence;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StepDto {
        private int step;
        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedScheme {
        private String schemeName;
        private String description;
        private String eligibility;
        private List<String> documentsRequired;
        private List<StepDto> processSteps;
        private String estimatedTimeline;
        private int version;
        private java.time.Instant publishedAt;
    }
}

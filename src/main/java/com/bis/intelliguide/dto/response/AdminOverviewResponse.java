package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminOverviewResponse {
    private long totalStandards;
    private long totalDocuments;
    private long totalSchemes;
    private long totalServices;
    private long totalQueries;
    private long flaggedThisWeek;
    private java.util.List<FlaggedAnswerDto> recentFlaggedQueries;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FlaggedAnswerDto {
        private String id;
        private String question;
        private String agent;
        private double confidenceScore;
        private java.time.Instant createdAt;
    }
}

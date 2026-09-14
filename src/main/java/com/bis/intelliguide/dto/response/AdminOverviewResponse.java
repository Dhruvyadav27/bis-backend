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
}

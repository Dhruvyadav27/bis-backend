package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StandardMatch {
    private String isNumber;
    private String title;
    /** 0.0 - 1.0 similarity-derived score */
    private double matchScore;
    /** high | moderate | needs-verification, derived from matchScore thresholds */
    private String confidenceLabel;
    private String clause;
}

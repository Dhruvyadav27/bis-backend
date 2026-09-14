package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConsumerAskResponse {
    private String answer;
    private List<CitationRef> references;
    /** high | moderate | needs-verification */
    private String confidence;
    private boolean insufficientEvidence;
}

package com.bis.intelliguide.service.rag;

import com.bis.intelliguide.dto.response.CitationRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GenerationResult {
    private String answer;
    private List<CitationRef> citations;
    private String confidenceLabel;
    private double confidenceScore;
    private boolean insufficientEvidence;
}

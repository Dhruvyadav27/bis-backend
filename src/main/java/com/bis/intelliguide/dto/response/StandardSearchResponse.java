package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StandardSearchResponse {
    private List<StandardMatch> results;
    /** Set to true (and results empty) when nothing cleared the similarity threshold. */
    private boolean insufficientEvidence;
    private String message;
}

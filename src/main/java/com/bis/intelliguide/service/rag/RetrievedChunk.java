package com.bis.intelliguide.service.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One chunk returned by Atlas Vector Search, with its similarity score. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RetrievedChunk {
    private String text;
    private String clauseRef;
    private String sourceId;
    /** cosine similarity, 0.0 - 1.0 */
    private double score;
}

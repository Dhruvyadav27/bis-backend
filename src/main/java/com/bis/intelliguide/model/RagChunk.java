package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

// Shared collection for certification_schemes / bis_services / consumer_protection —
// combined into ONE collection + ONE vector index to stay within Atlas M0's free-tier
// 3-index limit. "standards" keeps its own separate collection + index.
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "rag_content")
public class RagChunk {
    @Id
    private String id;

    /** "certification_schemes" | "bis_services" | "consumer_protection" */
    private String domain;

    /** id of the original CertificationScheme / BisService / ConsumerRule document */
    private String sourceId;

    private String clauseRef;
    private String text;
    private List<Double> embedding;
}
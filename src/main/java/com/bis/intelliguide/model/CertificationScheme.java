package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "certification_schemes")
public class CertificationScheme {
    @Id
    private String id;
    private String schemeName;
    private String description;
    private String eligibility;
    private List<String> documentsRequired;
    private List<ProcessStep> processSteps;
    private String estimatedTimeline;
    /** DRAFT | PENDING_REVIEW | PUBLISHED | WITHDRAWN */
    private String status;
    private int version;
    private Instant publishedAt;
    private String createdBy;
    private String lastEditedBy;
    private List<Chunk> chunks;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProcessStep {
        private int step;
        private String title;
        private String description;
    }
}

package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "bis_services")
public class BisService {
    @Id
    private String id;
    private String serviceName;
    private String category;
    private String description;

    // pehle ye "String procedure" tha — ab list of steps
    private List<ProcedureStep> procedure;

    /** DRAFT | PENDING_REVIEW | PUBLISHED | WITHDRAWN */
    private String status;
    private int version;
    private Instant publishedAt;
    private String createdBy;
    private String lastEditedBy;
    private List<Chunk> chunks;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProcedureStep {
        private int step;
        private String description;
    }
}

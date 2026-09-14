package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

// Collection name MUST be exactly "consumer_protection" — RetrievalService looks
// up the Mongo collection by this exact string.
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "consumer_protection")
public class ConsumerRule {
    @Id
    private String id;
    private String actName;
    private String clauseRef;
    private String topic;
    private String description;
    private String applicableAction;

    /** DRAFT | PENDING_REVIEW | PUBLISHED | WITHDRAWN */
    private String status;
    private int version;
    private Instant publishedAt;
    private String createdBy;
    private String lastEditedBy;
    private List<Chunk> chunks;
}

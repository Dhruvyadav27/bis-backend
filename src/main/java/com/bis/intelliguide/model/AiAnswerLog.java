package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "ai_answer_logs")
public class AiAnswerLog {
    @Id
    private String id;
    private String userId;
    private String question;
    /** which agent handled this: STANDARD_FINDER | CERTIFICATION_GUIDE | CONSUMER_AFFAIRS | ... */
    private String agent;
    private double confidenceScore;
    private String sourceRef;
    private String answer;
    private boolean flagged;
    /** UNREVIEWED | CORRECT | NEEDS_UPDATE | EDITED */
    private String reviewStatus;
    private String reviewNote;
    private Instant createdAt;
}

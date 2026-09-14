package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Consumer Affairs — complaint-filing mode. Separate from general Q&A (logged in ai_answer_logs). */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "complaints")
public class Complaint {
    @Id
    private String id;
    private String referenceId;
    private String userId;
    /** fake_isi | counterfeit | purity_mismatch | refund | service */
    private String complaintType;
    private String productDetails;
    private String licenseOrHuidNumber;
    private String evidenceUrl;
    private String applicableClause;
    private double confidenceScore;
    /** FILED | UNDER_REVIEW | VERIFIED | FORWARDED | RESOLVED | REJECTED */
    private String status;
    private String adminNote;
    private boolean redirectedToBis;
    private Instant createdAt;
    private Instant updatedAt;
}

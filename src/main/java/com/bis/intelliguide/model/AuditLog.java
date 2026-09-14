package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "audit_log")
public class AuditLog {
    @Id
    private String id;
    private String adminUserId;
    private String entityType;
    private String entityId;
    private String action;
    private Instant timestamp;
}

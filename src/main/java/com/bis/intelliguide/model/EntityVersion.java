package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * One snapshot of an admin-editable entity (Standard / CertificationScheme / BisService)
 * at a point in time. Written on every create/update/publish/reject/restore so any
 * previous state can be inspected or restored.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "entity_versions")
public class EntityVersion {
    @Id
    private String id;

    /** "STANDARD" | "CERTIFICATION_SCHEME" | "BIS_SERVICE" */
    private String entityType;
    private String entityId;
    private int versionNumber;

    /** "CREATE" | "UPDATE" | "PUBLISH" | "REJECT" | "RESTORE" */
    private String action;

    /** Optional — only populated for REJECT actions. */
    private String reason;

    private String editedBy;
    private Instant timestamp;

    /** Field snapshot at this version (chunks/embeddings deliberately excluded — see VersionHistoryService). */
    private Map<String, Object> snapshot;
}
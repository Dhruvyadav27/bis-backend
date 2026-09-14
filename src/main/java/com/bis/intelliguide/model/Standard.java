

package com.bis.intelliguide.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "standards")
public class Standard {

    @Id
    private String id;

    private String isNumber;
    private String title;
    private String scope;
    private String revision;
    private String category;

    /** DRAFT | PENDING_REVIEW | PUBLISHED | WITHDRAWN */
    private String status;

    private String documentUrl;
    private int version;
    private Instant publishedAt;
    private String createdBy;
    private String lastEditedBy;

    private List<Chunk> chunks;

    // ===== naye fields =====

    /** CRS | QCO | VOLUNTARY */
    private String regulatoryType;

    private boolean isCompulsory;

    /** e.g. "CRS", or a specific certification scheme id/name this standard is tied to */
    private String certificationSchemeRef;
}

package com.bis.intelliguide.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class StandardBulkImportRequest {

    private String isNumber;
    private String title;
    private String scope;
    private String category;
    private String revision;

    /** DRAFT | PENDING_REVIEW | PUBLISHED | WITHDRAWN */
    private String status;

    private String documentUrl;

    /** CRS | QCO | VOLUNTARY */
    private String regulatoryType;

    private boolean isCompulsory;

    private String certificationSchemeRef;

    private List<ChunkInput> chunks;

    @Data
    public static class ChunkInput {
        private String text;
        private String clauseRef;
    }
}

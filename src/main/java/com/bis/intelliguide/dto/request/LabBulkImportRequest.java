package com.bis.intelliguide.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class LabBulkImportRequest {
    // Purana format (agar kabhi mile)
    private String district;
    private String scope;
    private List<String> standardsCovered;
    private String workingHours;
    private String address;

    // Common fields
    private String name;
    private String state;
    private String recognitionStatus;

    // Naye fields — asli BIS data column-structure
    private Integer slNo;
    private String ownershipType;
    private String oslCode;
    private String recognitionValidUpTo;
    private String remarks;
}

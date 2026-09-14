package com.bis.intelliguide.dto.request;

import lombok.Data;

@Data
public class HuidRecordBulkImportRequest {
    private String huid;
    private String purity;
    private String ahcCentre;

    /** Format: "yyyy-MM-dd" */
    private String hallmarkedOn;

    private boolean verified;
}

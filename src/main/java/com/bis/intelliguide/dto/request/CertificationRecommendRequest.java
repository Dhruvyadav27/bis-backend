package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CertificationRecommendRequest {
    @NotBlank
    private String productType;

    /** MSME | LARGE | FOREIGN */
    private String manufacturerType;

    private String productCategory;
    private Boolean udyamRegistered;

    /** naya field: spec ke rule-chain ka 5th branch — user explicitly management-system cert maangta hai */
    private Boolean managementSystemCertificationRequested;
}

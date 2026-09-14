package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CertificationRecommendRequest {
    @NotBlank
    private String productType;

    /** MSME | LARGE | FOREIGN */
    private String manufacturerType;

    private Boolean udyamRegistered;
}

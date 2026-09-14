package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplaintRequest {
    @NotBlank
    private String complaintType;

    @NotBlank
    private String productDetails;

    private String licenseOrHuidNumber;
    private String evidenceUrl;
}

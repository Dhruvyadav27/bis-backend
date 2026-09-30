package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CompleteProfileRequest {
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "CONSUMER|MSME|LABORATORY", message = "Role must be one of CONSUMER, MSME, LABORATORY")
    private String role;

    private String phone;
    private String preferredLanguage;
    private Boolean udyamRegistered;
    private String labAffiliation;
}

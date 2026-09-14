package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JourneyStartRequest {
    @NotBlank
    private String productTitle;

    @NotBlank
    private String productDescription;

    @NotBlank
    private String state;

    @NotBlank
    private String district;

    private String manufacturerType;
}

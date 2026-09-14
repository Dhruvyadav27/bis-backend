package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StandardSearchRequest {
    @NotBlank
    private String productDescription;
}

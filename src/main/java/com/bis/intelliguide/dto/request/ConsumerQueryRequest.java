package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConsumerQueryRequest {
    @NotBlank
    private String query;
}

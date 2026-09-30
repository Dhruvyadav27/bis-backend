package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JourneyAskRequest {
    @NotBlank(message = "Query is required")
    private String query;
}

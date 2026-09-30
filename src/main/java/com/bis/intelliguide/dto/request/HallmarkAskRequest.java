package com.bis.intelliguide.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Data
public class HallmarkAskRequest {
    @NotBlank(message = "Query is required")
    private String query;
    private Map<String, String> context;
}

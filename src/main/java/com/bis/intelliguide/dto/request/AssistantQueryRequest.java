package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssistantQueryRequest {
    @NotBlank
    private String query;
    private AssistantContext context;

    @Data
    public static class AssistantContext {
        private String currentAgent;
        private String currentStep;
    }
}

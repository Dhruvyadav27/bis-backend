package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JourneyResponse {
    private String journeyId;
    private int currentStep;
    private String status;
    private String productTitle;
    private String productDescription;
    private List<StageDto> stages;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StageDto {
        private int step;
        private String title;
        private String status;
        private Object result;
        private String agentUsed;
    }
}

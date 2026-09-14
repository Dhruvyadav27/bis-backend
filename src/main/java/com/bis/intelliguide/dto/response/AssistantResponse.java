package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssistantResponse {
    private String answer;
    private SuggestedAction suggestedAction;
    private boolean shouldRedirect;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SuggestedAction {
        private String label;
        private String route;
    }
}

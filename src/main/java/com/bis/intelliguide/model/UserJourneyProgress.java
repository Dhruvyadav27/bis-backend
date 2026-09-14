package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "user_journey_progress")
public class UserJourneyProgress {
    @Id
    private String id;
    private String userId;
    /** e.g. GET_PRODUCT_CERTIFIED */
    private String journeyType;
    private int currentStep;
    private List<Stage> stages;
    private Instant updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Stage {
        private int step;
        private String title;
        /** PENDING | IN_PROGRESS | DONE */
        private String status;
        private Object result;
        private String agentUsed;
    }
}

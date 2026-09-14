package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "testing_requirements")
public class TestingRequirement {
    @Id
    private String id;
    private String standardId;
    private String testName;
    private String method;
    private String acceptanceCriteria;
}

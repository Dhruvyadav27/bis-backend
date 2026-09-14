package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "standard_relations")
public class StandardRelation {
    @Id
    private String id;
    private String standardId;
    private String relatedStandardId;
    /** e.g. REFERENCES | SUPERSEDES | RELATED */
    private String relationType;
}

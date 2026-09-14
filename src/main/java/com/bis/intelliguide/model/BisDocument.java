package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Named BisDocument (not "Document") to avoid clashing with the Spring Data @Document annotation. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "documents")
public class BisDocument {
    @Id
    private String id;
    private String documentType;
    private String title;
    private String version;
    private String source;
    private Instant uploadedAt;
}

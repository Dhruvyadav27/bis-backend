package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "huid_records")
public class HuidRecord {
    @Id
    private String id;
    private String huid;
    private String purity;
    private String ahcCentre;
    private Instant hallmarkedOn;
    private boolean verified;
}

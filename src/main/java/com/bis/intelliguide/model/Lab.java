package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "labs")
public class Lab {
    @Id
    private String id;
    private String name;
    private String state;
    private String district;
    private double distanceMeta;
    private String scope;
    private String workingHours;
    private String recognitionStatus;
    private List<String> standardsCovered;
    private String address;

    // ===== naye fields (asli BIS official data ke columns) =====
    private Integer slNo;
    private String ownershipType;          // "Govt." | "Private"
    private String oslCode;
    private String recognitionValidUpTo;   // raw string, format DD.MM.YYYY (kabhi galat bhi ho sakta hai source mein)
    private String remarks;                // suspension history — free text, lamba ho sakta hai
}

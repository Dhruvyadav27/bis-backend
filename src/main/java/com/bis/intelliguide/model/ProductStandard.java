package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "product_standards")
public class ProductStandard {
    @Id
    private String id;
    private String productId;
    private String standardId;
}

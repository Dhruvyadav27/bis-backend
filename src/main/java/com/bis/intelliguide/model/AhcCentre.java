package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** BIS Assaying & Hallmarking Centre — geolocation for nearest-centre search (Agent 3b). */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "ahc_centres")
public class AhcCentre {
    @Id
    private String id;
    private String name;
    private String city;
    private String state;
    private String address;
    private GeoPoint location;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GeoPoint {
        private String type;
        private double[] coordinates; // [longitude, latitude]
    }
}
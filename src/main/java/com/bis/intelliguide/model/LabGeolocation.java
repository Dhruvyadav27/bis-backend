package com.bis.intelliguide.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lab_geolocation")
public class LabGeolocation {
    @Id
    private String id;
    private String labName;
    private String state;
    private String district;
    private String city;
    private String address;
    private GeoPoint location;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoPoint {
        private String type;
        private double[] coordinates; // [longitude, latitude]
    }
}

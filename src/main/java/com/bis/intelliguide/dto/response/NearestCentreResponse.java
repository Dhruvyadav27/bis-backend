package com.bis.intelliguide.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearestCentreResponse {
    private List<CentreDto> centres;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CentreDto {
        private String name;
        private String city;
        private String state;
        private String address;
        private double distanceKm;
    }
}

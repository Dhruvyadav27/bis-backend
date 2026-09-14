package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LabSearchResponse {
    private List<LabDto> labs;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LabDto {
        private String name;
        private double distanceKm;
        private String scope;
    }
}

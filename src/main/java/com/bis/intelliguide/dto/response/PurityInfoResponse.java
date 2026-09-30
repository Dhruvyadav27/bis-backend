package com.bis.intelliguide.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurityInfoResponse {
    private String karat;
    private double purityPercent;
    private String description;
}

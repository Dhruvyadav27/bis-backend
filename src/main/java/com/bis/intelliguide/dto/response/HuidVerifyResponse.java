package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HuidVerifyResponse {
    private boolean verified;
    private String purity;
    private String ahcCentre;
    private Instant hallmarkedOn;
    private String message;
}

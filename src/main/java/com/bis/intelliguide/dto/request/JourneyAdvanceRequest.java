package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JourneyAdvanceRequest {
    @NotBlank
    private String journeyId;

    private int step;

    /** mark_done */
    @NotBlank
    private String action;

    private Double lat;
    private Double lng;
}

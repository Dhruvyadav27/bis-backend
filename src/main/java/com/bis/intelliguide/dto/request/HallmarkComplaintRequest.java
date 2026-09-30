package com.bis.intelliguide.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class HallmarkComplaintRequest {
    @NotBlank(message = "Query is required")
    private String query;
}

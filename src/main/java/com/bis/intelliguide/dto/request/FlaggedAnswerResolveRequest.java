package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlaggedAnswerResolveRequest {
    /** mark_correct | needs_update | edit */
    @NotBlank
    private String action;
    private String note;
}

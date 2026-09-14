package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminInviteRequest {
    @NotBlank @Email
    private String email;

    /** Always ADMIN for this endpoint; kept explicit for clarity/future roles. */
    @NotBlank
    private String role;
}

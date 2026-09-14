package com.bis.intelliguide.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    /**
     * Public registration only ever accepts these three. ADMIN is intentionally
     * excluded from the allowed pattern below — never trust a client-supplied
     * ADMIN role, even if someone crafts a raw request with role=ADMIN.
     */
    @NotBlank
    @Pattern(regexp = "CONSUMER|MSME|LABORATORY", message = "role must be one of CONSUMER, MSME, LABORATORY")
    private String role;

    private String phone;
    private String preferredLanguage;
}

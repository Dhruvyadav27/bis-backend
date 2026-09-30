package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String token;
    private UserSummary user;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserSummary {
        private String id;
        private String name;
        private String email;
        private String role;
        private boolean isNewUser;
        private Boolean profileCompleted;
    }
}

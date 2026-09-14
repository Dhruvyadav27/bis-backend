package com.bis.intelliguide.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;
    private String email;
    private String passwordHash;

    /** CONSUMER | MSME | LABORATORY | ADMIN. Never set to ADMIN from a public endpoint. */
    private String role;

    private String phone;
    private String preferredLanguage;

    /** ACTIVE | DISABLED */
    private String status;

    private Instant createdAt;
}

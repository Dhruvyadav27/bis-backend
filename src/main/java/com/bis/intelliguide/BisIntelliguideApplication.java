package com.bis.intelliguide;

import com.bis.intelliguide.model.User;
import com.bis.intelliguide.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@SpringBootApplication
public class BisIntelliguideApplication {

    public static void main(String[] args) {
        SpringApplication.run(BisIntelliguideApplication.class, args);
    }

    /**
     * Seeds exactly one ADMIN account on startup, ONLY if no admin exists yet.
     * This is the one and only way an ADMIN account can be created outside of
     * an existing admin inviting/promoting someone (see AdminUsersController).
     * Public registration (AuthController) never accepts role=ADMIN.
     */
    @Bean
    CommandLineRunner seedAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.seed.enabled:true}") boolean seedEnabled,
            @Value("${app.admin.seed.email}") String seedEmail,
            @Value("${app.admin.seed.password}") String seedPassword,
            @Value("${app.admin.seed.name}") String seedName
    ) {
        return args -> {
            if (!seedEnabled) return;
            boolean adminExists = userRepository.existsByRole("ADMIN");
            if (adminExists) return;

            User admin = User.builder()
                    .name(seedName)
                    .email(seedEmail)
                    .passwordHash(passwordEncoder.encode(seedPassword))
                    .role("ADMIN")
                    .status("ACTIVE")
                    .createdAt(Instant.now())
                    .build();
            userRepository.save(admin);
            System.out.println("[SEED] First ADMIN account created for: " + seedEmail
                    + " (change the password immediately after first login)");
        };
    }
}

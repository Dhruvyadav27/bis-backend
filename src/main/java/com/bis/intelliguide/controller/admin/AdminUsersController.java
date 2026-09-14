package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.AdminInviteRequest;
import com.bis.intelliguide.dto.request.RoleChangeRequest;
import com.bis.intelliguide.model.User;
import com.bis.intelliguide.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The ONLY two ways (besides the CommandLineRunner seed bean on first boot) that an
 * ADMIN account can ever be created or granted: an existing admin promoting a user's
 * role, or an existing admin inviting a new admin by email. Public /api/auth/register
 * can never produce an ADMIN account (enforced by @Pattern on RegisterRequest).
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public List<User> list() {
        return userRepository.findAll();
    }

    @PostMapping("/{id}/role")
    public User changeRole(@PathVariable String id, @Valid @RequestBody RoleChangeRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setRole(request.getRole());
        return userRepository.save(user);
    }

    /** Creates a new ADMIN account with a temporary password (in a full build: emailed as a reset link). */
    @PostMapping("/invite")
    public User invite(@Valid @RequestBody AdminInviteRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        String tempPassword = UUID.randomUUID().toString().substring(0, 12);
        User admin = User.builder()
                .name("Invited Admin")
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role("ADMIN")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();
        return userRepository.save(admin);
        // NOTE: in a full build, email `tempPassword` to request.getEmail() via a mail
        // service instead of ever returning/logging it in plaintext.
    }
}

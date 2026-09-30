package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.request.AdminInviteRequest;
import com.bis.intelliguide.dto.request.RoleChangeRequest;
import com.bis.intelliguide.model.User;
import com.bis.intelliguide.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;


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
    /**
     * Creates a pending ADMIN account. No password is set — this system is Google
     * Sign-In only, so the invited person simply signs in with Google using this
     * exact email address and is automatically recognized as ADMIN.
     * Status becomes ACTIVE on their first successful Google login (see AuthService).
     *
     * NOTE: no email is sent automatically (no mail service configured in this build).
     * The inviting admin must communicate the invited email + instructions manually.
     */
    @PostMapping("/invite")
    public User invite(@Valid @RequestBody AdminInviteRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User admin = User.builder()
                .name("Invited Admin")
                .email(request.getEmail())
                .role("ADMIN")
                .status("INVITED")
                .profileCompleted(true)
                .createdAt(Instant.now())
                .build();
        return userRepository.save(admin);
    }
}

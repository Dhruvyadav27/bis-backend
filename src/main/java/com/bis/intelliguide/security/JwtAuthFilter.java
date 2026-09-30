package com.bis.intelliguide.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the Authorization: Bearer <token> header, validates it, and populates
 * the SecurityContext with the user's id (as principal) and ROLE_<role> authority
 * so that @PreAuthorize("hasRole('ADMIN')") works on admin/** controllers.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // =========================================================
        // CORS PREFLIGHT REQUEST
        // OPTIONS requests do not contain the user's JWT.
        // Let Spring Security's CORS configuration handle them.
        // =========================================================
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // =========================================================
        // JWT AUTHENTICATION
        // =========================================================
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtService.isTokenValid(token)) {
            String userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();

            if (role != null && !role.isEmpty() && !"null".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }

            var authToken = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}

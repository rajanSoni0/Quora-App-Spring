package org.example.quoraproject.util;

import org.example.quoraproject.exception.UnauthorizedException;
import org.example.quoraproject.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Single access point for the authenticated principal. Services use this
 * instead of trusting a client-supplied userId.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static CustomUserDetails getCurrentUser() {
        return findCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("No authenticated user in security context"));
    }

    public static Optional<CustomUserDetails> findCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return Optional.of(details);
        }
        return Optional.empty();
    }

    public static Optional<String> getCurrentUsername() {
        return findCurrentUser().map(CustomUserDetails::getUsername);
    }
}

package org.example.quoraproject.dto.response;

import java.time.Instant;

/**
 * Public view of a user. Password (even hashed) is never exposed.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        Instant createdAt
) {}

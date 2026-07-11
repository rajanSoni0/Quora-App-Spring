package org.example.quoraproject.dto.response;

/**
 * Minimal author projection embedded in questions, answers, and comments.
 */
public record AuthorResponse(
        Long id,
        String username
) {}

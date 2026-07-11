package org.example.quoraproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Note: no userId field. The author is always the authenticated principal,
 * taken from the SecurityContext - the client is never trusted to declare
 * who owns a resource.
 */
public record CreateQuestionRequest(

        @NotBlank(message = "Title is required")
        @Size(min = 10, max = 255, message = "Title must be between 10 and 255 characters")
        String title,

        @NotBlank(message = "Content is required")
        @Size(min = 20, max = 10000, message = "Content must be between 20 and 10000 characters")
        String content,

        @NotEmpty(message = "At least one tag is required")
        Set<Long> tagIds
) {}

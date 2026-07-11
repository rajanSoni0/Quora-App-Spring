package org.example.quoraproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(

        @NotBlank(message = "Content is required")
        @Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters")
        String content,

        @NotNull(message = "answerId is required")
        Long answerId,

        Long parentCommentId
) {}

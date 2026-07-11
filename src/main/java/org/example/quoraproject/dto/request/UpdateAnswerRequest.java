package org.example.quoraproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAnswerRequest(

        @NotBlank(message = "Content is required")
        @Size(min = 10, max = 20000, message = "Content must be between 10 and 20000 characters")
        String content
) {}

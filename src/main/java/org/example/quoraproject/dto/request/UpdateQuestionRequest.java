package org.example.quoraproject.dto.request;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateQuestionRequest(

        @Size(min = 10, max = 255, message = "Title must be between 10 and 255 characters")
        String title,

        @Size(min = 20, max = 10000, message = "Content must be between 20 and 10000 characters")
        String content,

        Set<Long> tagIds
) {}

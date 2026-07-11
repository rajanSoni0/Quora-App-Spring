package org.example.quoraproject.dto.response;

import java.time.Instant;
import java.util.Set;

public record QuestionResponse(
        Long id,
        String title,
        String content,
        AuthorResponse author,
        Set<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt
) {}

package org.example.quoraproject.dto.response;

import java.time.Instant;

public record AnswerResponse(
        Long id,
        String content,
        Long questionId,
        AuthorResponse author,
        int likeCount,
        Instant createdAt,
        Instant updatedAt
) {}

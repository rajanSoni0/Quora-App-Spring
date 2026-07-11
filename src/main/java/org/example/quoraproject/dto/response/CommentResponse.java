package org.example.quoraproject.dto.response;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String content,
        Long answerId,
        Long parentCommentId,
        AuthorResponse author,
        int likeCount,
        Instant createdAt
) {}

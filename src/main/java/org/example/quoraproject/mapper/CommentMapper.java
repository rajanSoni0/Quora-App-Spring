package org.example.quoraproject.mapper;

import org.example.quoraproject.dto.response.CommentResponse;
import org.example.quoraproject.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface CommentMapper {

    @Mapping(target = "author", source = "user")
    @Mapping(target = "answerId", source = "answer.id")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "likeCount", expression = "java(comment.getLikedBy().size())")
    CommentResponse toResponse(Comment comment);
}

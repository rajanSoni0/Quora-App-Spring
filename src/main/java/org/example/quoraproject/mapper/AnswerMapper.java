package org.example.quoraproject.mapper;

import org.example.quoraproject.dto.response.AnswerResponse;
import org.example.quoraproject.entity.Answer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface AnswerMapper {

    @Mapping(target = "author", source = "user")
    @Mapping(target = "questionId", source = "question.id")
    @Mapping(target = "likeCount", expression = "java(answer.getLikedBy().size())")
    AnswerResponse toResponse(Answer answer);
}

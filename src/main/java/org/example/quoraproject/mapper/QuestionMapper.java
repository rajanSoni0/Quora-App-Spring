package org.example.quoraproject.mapper;

import org.example.quoraproject.dto.response.QuestionResponse;
import org.example.quoraproject.entity.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, TagMapper.class})
public interface QuestionMapper {

    @Mapping(target = "author", source = "user")
    QuestionResponse toResponse(Question question);
}

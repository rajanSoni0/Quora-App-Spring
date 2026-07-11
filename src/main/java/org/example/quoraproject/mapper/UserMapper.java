package org.example.quoraproject.mapper;

import org.example.quoraproject.dto.response.AuthorResponse;
import org.example.quoraproject.dto.response.UserResponse;
import org.example.quoraproject.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserResponse toResponse(User user);

    AuthorResponse toAuthorResponse(User user);
}

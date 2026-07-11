package org.example.quoraproject.mapper;

import org.example.quoraproject.dto.response.TagResponse;
import org.example.quoraproject.entity.Tag;
import org.mapstruct.Mapper;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponse toResponse(Tag tag);

    Set<TagResponse> toResponseSet(Set<Tag> tags);
}

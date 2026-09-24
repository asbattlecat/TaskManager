package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.TagDto;
import org.example.taskmanager.taskmanager.domain.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
  TagDto toDto(Tag tag);
  Tag toEntity(TagDto tagDto);
}

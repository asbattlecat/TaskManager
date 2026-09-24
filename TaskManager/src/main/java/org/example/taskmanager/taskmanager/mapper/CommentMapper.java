package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.CommentDto;
import org.example.taskmanager.taskmanager.domain.entity.Comment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentMapper {
  CommentDto toDto(Comment comment);
  Comment  toEntity(CommentDto commentDto);
}

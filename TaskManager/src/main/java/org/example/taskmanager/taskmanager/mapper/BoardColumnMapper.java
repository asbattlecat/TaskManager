package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.BoardColumnDto;
import org.example.taskmanager.taskmanager.domain.entity.BoardColumn;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BoardColumnMapper {
  BoardColumnDto toDto(BoardColumn boardColumn);
  BoardColumn toEntity(BoardColumnDto boardColumnDto);
}

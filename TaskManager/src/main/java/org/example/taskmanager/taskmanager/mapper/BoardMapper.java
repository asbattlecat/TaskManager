package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.BoardDto;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BoardMapper {
  BoardDto toDto(Board board);
  Board toEntity(BoardDto boardDto);
}

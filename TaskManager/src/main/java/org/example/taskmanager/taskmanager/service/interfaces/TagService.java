package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.domain.entity.Tag;
import org.example.taskmanager.taskmanager.domain.enums.TagColor;

import java.util.UUID;

public interface TagService {
  Tag create(String name, TagColor color);
  Tag delete(UUID tagId);
  Tag changeName(UUID tagId);
}

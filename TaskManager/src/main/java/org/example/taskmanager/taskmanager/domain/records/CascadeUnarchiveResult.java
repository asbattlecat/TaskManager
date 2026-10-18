package org.example.taskmanager.taskmanager.domain.records;

import org.example.taskmanager.taskmanager.domain.enums.RootType;

import java.util.List;
import java.util.UUID;

public record CascadeUnarchiveResult(
        UUID rootId,
        RootType rootType,
        int projectsUnarchived,
        int boardsUnarchived,
        int tasksUnarchived,
        List<UUID> projectIds,
        List<UUID> boardIds,
        List<UUID> taskIds
) {
  public static CascadeUnarchiveResult empty(UUID rootId, RootType rootType) {
    return new CascadeUnarchiveResult(
            rootId,
            rootType,
            0,
            0,
            0,
            List.of(),
            List.of(),
            List.of()
    );
  }
}

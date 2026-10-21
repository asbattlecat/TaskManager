package org.example.taskmanager.taskmanager.domain.records;

import org.example.taskmanager.taskmanager.domain.enums.RootEntityType;

import java.util.List;
import java.util.UUID;

public record CascadeUnarchiveResult(
        UUID rootId,
        RootEntityType rootEntityType,
        int projectsUnarchived,
        int boardsUnarchived,
        int tasksUnarchived,
        List<UUID> projectIds,
        List<UUID> boardIds,
        List<UUID> taskIds
) {
  public static CascadeUnarchiveResult empty(UUID rootId, RootEntityType rootEntityType) {
    return new CascadeUnarchiveResult(
            rootId,
            rootEntityType,
            0,
            0,
            0,
            List.of(),
            List.of(),
            List.of()
    );
  }
}

package org.example.taskmanager.taskmanager.domain.records;

import org.example.taskmanager.taskmanager.domain.enums.RootEntityType;

import java.util.List;
import java.util.UUID;

public record CascadeArchiveResult(
        UUID rootId,
        RootEntityType rootEntityType,
        int projectsArchived,
        int boardsArchived,
        int tasksArchived,
        List<UUID> projectIds,
        List<UUID> boardIds,
        List<UUID> taskIds
) {
  public static CascadeArchiveResult empty(UUID rootId, RootEntityType rootEntityType) {
    return new CascadeArchiveResult(
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

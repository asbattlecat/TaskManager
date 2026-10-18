package org.example.taskmanager.taskmanager.domain.records;

import org.example.taskmanager.taskmanager.domain.enums.RootType;

import java.util.List;
import java.util.UUID;

public record CascadeArchiveResult(
        UUID rootId,
        RootType rootType,
        int projectsArchived,
        int boardsArchived,
        int tasksArchived,
        List<UUID> projectIds,
        List<UUID> boardIds,
        List<UUID> taskIds
) {
  public static CascadeArchiveResult empty(UUID rootId, RootType rootType) {
    return new CascadeArchiveResult(
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

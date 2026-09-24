package org.example.taskmanager.taskmanager.infrastructure.specification;

import jakarta.persistence.criteria.Root;
import org.example.taskmanager.taskmanager.domain.entity.Tag;
import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.example.taskmanager.taskmanager.domain.enums.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class TaskSpecification {
  public static Specification<Task> statusContains(TaskStatus status) {
    return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
  }

  public static Specification<Task> assigneeContains(UUID assigneeId) {
    return (root, query, cb) ->
            assigneeId == null ? null : cb.equal(root.get("assigneeId"), assigneeId);
  }

  public static Specification<Task> tagContains(Tag tag) {
    return (root, query, cb) -> {
      Root<Tag> tagRoot = query.from(Tag.class);

      return cb.and(
              cb.equal(tagRoot.get("taskId"), root.get("id")),
              cb.equal(tagRoot.get("name"), tag.getName()),
              cb.equal(tagRoot.get("color"), tag.getColor())
      );
    };
  }
}

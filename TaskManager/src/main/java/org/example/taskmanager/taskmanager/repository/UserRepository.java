package org.example.taskmanager.taskmanager.repository;

import org.example.taskmanager.taskmanager.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}

package com.taskflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.taskflow.entity.Task;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
public interface TaskRepository extends
        JpaRepository<Task, Long>,
        JpaSpecificationExecutor<Task> {
List<Task> findByStatus(TaskStatus status);
List<Task> findByPriority(TaskPriority priority);
List<Task> findByUserId(Long userId);
Page<Task> findByUserId(Long userId, Pageable pageable);
Optional<Task> findByIdAndUserId(Long taskId, Long userId);
List<Task> findByStatusAndPriority(
        TaskStatus status,
        TaskPriority priority
);
List<Task> findByTitleContainingIgnoreCase(String keyword);
}

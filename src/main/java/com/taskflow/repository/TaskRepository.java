package com.taskflow.repository;

import java.util.List;

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
List<Task> findByStatusAndPriority(
        TaskStatus status,
        TaskPriority priority
);
List<Task> findByTitleContainingIgnoreCase(String keyword);
}

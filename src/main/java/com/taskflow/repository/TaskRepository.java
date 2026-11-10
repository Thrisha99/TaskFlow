
package com.taskflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.taskflow.entity.Task;

public interface TaskRepository extends
        JpaRepository<Task, Long>,
        JpaSpecificationExecutor<Task> {

    // GET ALL TASKS BELONGING TO A USER
    List<Task> findByUserId(Long userId);

    // GET USER TASKS WITH PAGINATION
    Page<Task> findByUserId(Long userId, Pageable pageable);

    // FIND A TASK ONLY IF IT BELONGS TO THE USER
    Optional<Task> findByIdAndUserId(Long taskId, Long userId);
}

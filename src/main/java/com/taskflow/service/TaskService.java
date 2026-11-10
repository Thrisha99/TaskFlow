package com.taskflow.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.taskflow.dto.TaskRequestDTO;
import com.taskflow.dto.TaskResponseDTO;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.User;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.specification.TaskSpecification;
@Service
public class TaskService {

    private final TaskRepository taskRepository;
private final UserRepository userRepository;

public TaskService(
        TaskRepository taskRepository,
        UserRepository userRepository) {

    this.taskRepository = taskRepository;
    this.userRepository = userRepository;
}
    // FILTER TASKS BY STATUS
public List<TaskResponseDTO> getTasksByStatus(TaskStatus status) {

    return taskRepository.findByStatus(status)
            .stream()
            .map(TaskMapper::toResponseDTO)
            .toList();
}
// FILTER TASKS BY STATUS AND PRIORITY
public List<TaskResponseDTO> getTasksByStatusAndPriority(
        TaskStatus status,
        TaskPriority priority) {

    return taskRepository.findByStatusAndPriority(status, priority)
            .stream()
            .map(TaskMapper::toResponseDTO)
            .toList();
}
// SEARCH TASKS BY TITLE
public List<TaskResponseDTO> searchTasksByTitle(String keyword) {

    return taskRepository.findByTitleContainingIgnoreCase(keyword)
            .stream()
            .map(TaskMapper::toResponseDTO)
            .toList();
}
// UNIFIED TASK FILTERING, SEARCH AND PAGINATION
public Page<TaskResponseDTO> getFilteredTasks(
        Long userId,
        TaskStatus status,
        TaskPriority priority,
        String keyword,
        Pageable pageable) {

    if (keyword != null) {
        keyword = keyword.trim();
    }

    Specification<Task> spec =
            Specification.where(TaskSpecification.hasStatus(status))
                    .and(TaskSpecification.hasPriority(priority))
                    .and(TaskSpecification.titleContains(keyword))
                    .and((root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("user").get("id"),
                                    userId
                            )
                    );

    return taskRepository.findAll(spec, pageable)
            .map(TaskMapper::toResponseDTO);
}
    // CREATE TASK
   // CREATE TASK
public TaskResponseDTO createTask(
        TaskRequestDTO request,
        Long userId) {

    // 1. Find the authenticated user
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found"));

    // 2. Convert DTO into Task entity
    Task task = TaskMapper.toEntity(request);

    // 3. Assign the task owner
    task.setUser(user);

    // 4. Save task in database
    Task savedTask = taskRepository.save(task);

    // 5. Convert entity into response DTO
    return TaskMapper.toResponseDTO(savedTask);
}

    // GET ALL TASKS
// GET ALL TASKS BELONGING TO A USER
public Page<TaskResponseDTO> getAllTasks(
        Long userId,
        Pageable pageable) {

    return taskRepository.findByUserId(userId, pageable)
            .map(TaskMapper::toResponseDTO);
}

    // GET TASK BY ID
    public TaskResponseDTO getTaskById(Long id) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"));

        return TaskMapper.toResponseDTO(task);
    }

    // UPDATE TASK
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO request) {

        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"));

        existingTask.setTitle(request.getTitle());
        existingTask.setDescription(request.getDescription());
        existingTask.setStatus(request.getStatus());
        existingTask.setPriority(request.getPriority());

        Task updatedTask = taskRepository.save(existingTask);

        return TaskMapper.toResponseDTO(updatedTask);
    }

    // DELETE TASK
    public void deleteTask(Long id) {

        if (!taskRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Task not found");
        }

        taskRepository.deleteById(id);
    }
// FILTER TASKS BY PRIORITY
public List<TaskResponseDTO> getTasksByPriority(TaskPriority priority) {

    return taskRepository.findByPriority(priority)
            .stream()
            .map(TaskMapper::toResponseDTO)
            .toList();
}
}

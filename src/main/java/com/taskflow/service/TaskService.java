
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

    // BUILD A SPECIFICATION THAT ALWAYS RESTRICTS TASKS TO ONE USER
    private Specification<Task> buildTaskSpecification(
            Long userId,
            TaskStatus status,
            TaskPriority priority,
            String keyword) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (keyword != null) {
            keyword = keyword.trim();
        }

        return Specification
                .where(TaskSpecification.hasStatus(status))
                .and(TaskSpecification.hasPriority(priority))
                .and(TaskSpecification.titleContains(keyword))
                .and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(
                                root.get("user").get("id"),
                                userId
                        )
                );
    }

    // FILTER TASKS BY STATUS
    public List<TaskResponseDTO> getTasksByStatus(
            Long userId,
            TaskStatus status) {

        return taskRepository.findAll(
                        buildTaskSpecification(userId, status, null, null)
                )
                .stream()
                .map(TaskMapper::toResponseDTO)
                .toList();
    }

    // FILTER TASKS BY STATUS AND PRIORITY
    public List<TaskResponseDTO> getTasksByStatusAndPriority(
            Long userId,
            TaskStatus status,
            TaskPriority priority) {

        return taskRepository.findAll(
                        buildTaskSpecification(
                                userId, status, priority, null
                        )
                )
                .stream()
                .map(TaskMapper::toResponseDTO)
                .toList();
    }

    // SEARCH TASKS BY TITLE
    public List<TaskResponseDTO> searchTasksByTitle(
            Long userId,
            String keyword) {

        return taskRepository.findAll(
                        buildTaskSpecification(
                                userId, null, null, keyword
                        )
                )
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

        Specification<Task> spec =
                buildTaskSpecification(
                        userId, status, priority, keyword
                );

        return taskRepository.findAll(spec, pageable)
                .map(TaskMapper::toResponseDTO);
    }

    // CREATE TASK
    public TaskResponseDTO createTask(
            TaskRequestDTO request,
            Long userId) {

        // Find the authenticated user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Convert request DTO into Task entity
        Task task = TaskMapper.toEntity(request);

        // Assign the task owner
        task.setUser(user);

        // Save task
        Task savedTask = taskRepository.save(task);

        return TaskMapper.toResponseDTO(savedTask);
    }

    // GET ALL TASKS BELONGING TO A USER WITH PAGINATION
    public Page<TaskResponseDTO> getAllTasks(
            Long userId,
            Pageable pageable) {

        return taskRepository.findByUserId(userId, pageable)
                .map(TaskMapper::toResponseDTO);
    }

    // GET TASK BY ID - VERIFY OWNERSHIP
    public TaskResponseDTO getTaskById(
            Long id,
            Long userId) {

        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found"
                ));

        return TaskMapper.toResponseDTO(task);
    }

    // UPDATE TASK - VERIFY OWNERSHIP
    public TaskResponseDTO updateTask(
            Long id,
            TaskRequestDTO request,
            Long userId) {

        Task existingTask = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found"
                ));

        existingTask.setTitle(request.getTitle());
        existingTask.setDescription(request.getDescription());
        existingTask.setStatus(request.getStatus());
        existingTask.setPriority(request.getPriority());

        // Do not change the existing task owner
        Task updatedTask = taskRepository.save(existingTask);

        return TaskMapper.toResponseDTO(updatedTask);
    }

    // DELETE TASK - VERIFY OWNERSHIP
    public void deleteTask(
            Long id,
            Long userId) {

        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found"
                ));

        taskRepository.delete(task);
    }

    // FILTER TASKS BY PRIORITY
    public List<TaskResponseDTO> getTasksByPriority(
            Long userId,
            TaskPriority priority) {

        return taskRepository.findAll(
                        buildTaskSpecification(
                                userId, null, priority, null
                        )
                )
                .stream()
                .map(TaskMapper::toResponseDTO)
                .toList();
    }
}

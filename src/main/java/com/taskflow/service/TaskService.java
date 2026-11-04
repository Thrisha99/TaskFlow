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
import com.taskflow.mapper.TaskMapper;
import com.taskflow.repository.TaskRepository;
import com.taskflow.specification.TaskSpecification;
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
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
                    .and(TaskSpecification.titleContains(keyword));

    return taskRepository.findAll(spec, pageable)
            .map(TaskMapper::toResponseDTO);
}
    // CREATE TASK
    public TaskResponseDTO createTask(TaskRequestDTO request) {

        Task task = TaskMapper.toEntity(request);

        Task savedTask = taskRepository.save(task);

        return TaskMapper.toResponseDTO(savedTask);
    }

    // GET ALL TASKS
 public Page<TaskResponseDTO> getAllTasks(Pageable pageable) {

    return taskRepository.findAll(pageable)
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

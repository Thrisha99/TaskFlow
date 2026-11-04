package com.taskflow.controller;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.taskflow.dto.TaskRequestDTO;
import com.taskflow.dto.TaskResponseDTO;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
private final TaskService taskService;
    

    public TaskController( TaskService taskService) {
    this.taskService = taskService;
}
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public TaskResponseDTO createTask(
        @Valid @RequestBody TaskRequestDTO taskRequestDTO) {

    return taskService.createTask(taskRequestDTO);
}

@GetMapping
public Page<TaskResponseDTO> getAllTasks(

        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,

        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "asc") String direction,

        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) TaskPriority priority,
        @RequestParam(required = false) String keyword) {
if (page < 0) {
    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Page number cannot be negative"
    );
}

if (size <= 0 || size > 100) {
    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Page size must be between 1 and 100"
    );
}
List<String> allowedSortFields = List.of(
        "id",
        "title",
        "description",
        "status",
        "priority"
);

if (!allowedSortFields.contains(sortBy)) {
    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid sorting field: " + sortBy
    );
}
if (!direction.equalsIgnoreCase("asc")
        && !direction.equalsIgnoreCase("desc")) {

    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid sorting direction. Use 'asc' or 'desc'"
    );
}
    Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);

    return taskService.getFilteredTasks(
            status,
            priority,
            keyword,
            pageable
    );
}
    @GetMapping("/filter/status")
public List<TaskResponseDTO> getTasksByStatus(
        @RequestParam TaskStatus status) {

    return taskService.getTasksByStatus(status);
}
// FILTER TASKS BY PRIORITY
@GetMapping("/filter/priority")
public List<TaskResponseDTO> getTasksByPriority(
        @RequestParam TaskPriority priority) {

    return taskService.getTasksByPriority(priority);
}

// FILTER TASKS BY STATUS AND PRIORITY
@GetMapping("/filter")
public List<TaskResponseDTO> getTasksByStatusAndPriority(
        @RequestParam TaskStatus status,
        @RequestParam TaskPriority priority) {

    return taskService.getTasksByStatusAndPriority(status, priority);
}
@GetMapping("/{id}")
public TaskResponseDTO getTaskById(@PathVariable Long id) {
    return taskService.getTaskById(id);
}
// SEARCH TASKS BY TITLE
@GetMapping("/search")
public List<TaskResponseDTO> searchTasks(
        @RequestParam String keyword) {

    return taskService.searchTasksByTitle(keyword);
}

@PutMapping("/{id}")
public TaskResponseDTO updateTask(
        @PathVariable Long id,
        @Valid @RequestBody TaskRequestDTO updatedTask) {

    return taskService.updateTask(id, updatedTask);
}

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);
}



}

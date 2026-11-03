package com.taskflow.controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.taskflow.dto.TaskRequestDTO;
import com.taskflow.dto.TaskResponseDTO;
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
public java.util.List<TaskResponseDTO> getAllTasks() {
    return taskService.getAllTasks();
}
    

@GetMapping("/{id}")
public TaskResponseDTO getTaskById(@PathVariable Long id) {
    return taskService.getTaskById(id);
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

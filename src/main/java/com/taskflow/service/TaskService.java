


package com.taskflow.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.taskflow.dto.TaskRequestDTO;
import com.taskflow.dto.TaskResponseDTO;
import com.taskflow.entity.Task;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.repository.TaskRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // CREATE TASK
    public TaskResponseDTO createTask(TaskRequestDTO request) {

        Task task = TaskMapper.toEntity(request);

        Task savedTask = taskRepository.save(task);

        return TaskMapper.toResponseDTO(savedTask);
    }

    // GET ALL TASKS
    public List<TaskResponseDTO> getAllTasks() {

        return taskRepository.findAll()
                .stream()
                .map(TaskMapper::toResponseDTO)
                .toList();
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
}

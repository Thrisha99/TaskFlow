package com.taskflow.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskflow.repository.TaskRepository;
import com.taskflow.dto.TaskRequestDTO;
import com.taskflow.dto.TaskResponseDTO;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.TaskPriority;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;
    @Test
void createTask_shouldReturnSavedTask() {

    // Arrange
    TaskRequestDTO request = new TaskRequestDTO();
    request.setTitle("Learn Unit Testing");
    request.setDescription("Practice JUnit and Mockito");
    request.setStatus(TaskStatus.PENDING);
    request.setPriority(TaskPriority.HIGH);

    Task savedTask = new Task();
    savedTask.setId(1L);
    savedTask.setTitle("Learn Unit Testing");
    savedTask.setDescription("Practice JUnit and Mockito");
    savedTask.setStatus(TaskStatus.PENDING);
    savedTask.setPriority(TaskPriority.HIGH);

    when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

    // Act
    TaskResponseDTO response = taskService.createTask(request);

    // Assert
    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("Learn Unit Testing", response.getTitle());
    assertEquals(TaskStatus.PENDING, response.getStatus());
    assertEquals(TaskPriority.HIGH, response.getPriority());

    verify(taskRepository, times(1)).save(any(Task.class));
}
@Test
void getAllTasks_shouldReturnAllTasks() {

    // Arrange
    Task task1 = new Task();
    task1.setId(1L);
    task1.setTitle("Learn Java");
    task1.setDescription("Practice Core Java");
    task1.setStatus(TaskStatus.PENDING);
    task1.setPriority(TaskPriority.HIGH);

    Task task2 = new Task();
    task2.setId(2L);
    task2.setTitle("Practice DSA");
    task2.setDescription("Solve LeetCode problems");
    task2.setStatus(TaskStatus.IN_PROGRESS);
    task2.setPriority(TaskPriority.MEDIUM);

    when(taskRepository.findAll()).thenReturn(
            java.util.List.of(task1, task2)
    );

    // Act
    java.util.List<TaskResponseDTO> response = taskService.getAllTasks();

    // Assert
    assertEquals(2, response.size());
    assertEquals("Learn Java", response.get(0).getTitle());
    assertEquals("Practice DSA", response.get(1).getTitle());

    verify(taskRepository, times(1)).findAll();
}
@Test
void getTaskById_shouldReturnTaskWhenIdExists() {

    // Arrange
    Task task = new Task();
    task.setId(1L);
    task.setTitle("Learn Spring Boot");
    task.setDescription("Understand REST APIs");
    task.setStatus(TaskStatus.PENDING);
    task.setPriority(TaskPriority.HIGH);

    when(taskRepository.findById(1L))
            .thenReturn(java.util.Optional.of(task));

    // Act
    TaskResponseDTO response = taskService.getTaskById(1L);

    // Assert
    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("Learn Spring Boot", response.getTitle());
    assertEquals(TaskPriority.HIGH, response.getPriority());

    verify(taskRepository, times(1)).findById(1L);
}
@Test
void getTaskById_shouldThrowExceptionWhenTaskNotFound() {

    // Arrange
    when(taskRepository.findById(999L))
            .thenReturn(java.util.Optional.empty());

    // Act & Assert
    org.springframework.web.server.ResponseStatusException exception =
            assertThrows(
                    org.springframework.web.server.ResponseStatusException.class,
                    () -> taskService.getTaskById(999L)
            );

    assertEquals(
            org.springframework.http.HttpStatus.NOT_FOUND,
            exception.getStatusCode()
    );

    verify(taskRepository, times(1)).findById(999L);
}
@Test
void updateTask_shouldUpdateExistingTask() {

    // Arrange
    Task existingTask = new Task();
    existingTask.setId(1L);
    existingTask.setTitle("Learn Java");
    existingTask.setDescription("Core Java");
    existingTask.setStatus(TaskStatus.PENDING);
    existingTask.setPriority(TaskPriority.LOW);

    TaskRequestDTO request = new TaskRequestDTO();
    request.setTitle("Learn Spring Boot");
    request.setDescription("Build REST APIs");
    request.setStatus(TaskStatus.IN_PROGRESS);
    request.setPriority(TaskPriority.HIGH);

    when(taskRepository.findById(1L))
            .thenReturn(java.util.Optional.of(existingTask));

    when(taskRepository.save(any(Task.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    TaskResponseDTO response = taskService.updateTask(1L, request);

    // Assert
    assertEquals("Learn Spring Boot", response.getTitle());
    assertEquals("Build REST APIs", response.getDescription());
    assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
    assertEquals(TaskPriority.HIGH, response.getPriority());

    verify(taskRepository, times(1)).findById(1L);
    verify(taskRepository, times(1)).save(any(Task.class));
}
@Test
void updateTask_shouldThrowExceptionWhenTaskNotFound() {

    // Arrange
    TaskRequestDTO request = new TaskRequestDTO();
    request.setTitle("Learn Spring Boot");
    request.setDescription("Build REST APIs");
    request.setStatus(TaskStatus.IN_PROGRESS);
    request.setPriority(TaskPriority.HIGH);

    when(taskRepository.findById(999L))
            .thenReturn(java.util.Optional.empty());

    // Act & Assert
    org.springframework.web.server.ResponseStatusException exception =
            assertThrows(
                    org.springframework.web.server.ResponseStatusException.class,
                    () -> taskService.updateTask(999L, request)
            );

    assertEquals(
            org.springframework.http.HttpStatus.NOT_FOUND,
            exception.getStatusCode()
    );

    verify(taskRepository, times(1)).findById(999L);
    verify(taskRepository, never()).save(any(Task.class));
}
@Test
void deleteTask_shouldDeleteTaskWhenIdExists() {

    // Arrange
    when(taskRepository.existsById(1L)).thenReturn(true);

    // Act
    taskService.deleteTask(1L);

    // Assert
    verify(taskRepository, times(1)).existsById(1L);
    verify(taskRepository, times(1)).deleteById(1L);
}
@Test
void deleteTask_shouldThrowExceptionWhenTaskNotFound() {

    // Arrange
    when(taskRepository.existsById(999L)).thenReturn(false);

    // Act & Assert
    org.springframework.web.server.ResponseStatusException exception =
            assertThrows(
                    org.springframework.web.server.ResponseStatusException.class,
                    () -> taskService.deleteTask(999L)
            );

    assertEquals(
            org.springframework.http.HttpStatus.NOT_FOUND,
            exception.getStatusCode()
    );

    verify(taskRepository, times(1)).existsById(999L);
    verify(taskRepository, never()).deleteById(999L);
}
}
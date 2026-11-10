
package com.taskflow.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.taskflow.dto.TaskRequestDTO;
import com.taskflow.dto.TaskResponseDTO;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.User;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    // ==========================================
    // CREATE TASK
    // ==========================================

    @Test
    void createTask_shouldReturnSavedTask() {

        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");

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
        savedTask.setUser(user);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(taskRepository.save(any(Task.class)))
                .thenReturn(savedTask);

        TaskResponseDTO response =
                taskService.createTask(request, userId);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Learn Unit Testing", response.getTitle());
        assertEquals(TaskStatus.PENDING, response.getStatus());
        assertEquals(TaskPriority.HIGH, response.getPriority());

        verify(userRepository).findById(userId);

        verify(taskRepository).save(argThat(task ->
                task.getUser() != null &&
                task.getUser().getId().equals(userId)
        ));
    }

    // ==========================================
    // FILTERED TASKS - PAGINATION
    // ==========================================

    @Test
    void getFilteredTasks_shouldReturnPaginatedTasks() {

        Task task1 = createTask(
                1L, "Learn Java",
                TaskStatus.PENDING, TaskPriority.HIGH
        );

        Task task2 = createTask(
                2L, "Practice DSA",
                TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM
        );

        Pageable pageable = PageRequest.of(
                0, 2, Sort.by("id").ascending()
        );

        Page<Task> taskPage = new PageImpl<>(
                List.of(task1, task2), pageable, 2
        );

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(taskPage);

        Page<TaskResponseDTO> response =
                taskService.getFilteredTasks(
                        1L, null, null, null, pageable
                );

        assertEquals(2, response.getContent().size());
        assertEquals("Learn Java",
                response.getContent().get(0).getTitle());
        assertEquals("Practice DSA",
                response.getContent().get(1).getTitle());
        assertEquals(2, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(0, response.getNumber());

        verify(taskRepository).findAll(
                any(Specification.class), eq(pageable)
        );
    }

    @Test
    void getFilteredTasks_shouldReturnEmptyPageWhenNoTasksMatch() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> emptyPage = new PageImpl<>(
                List.of(), pageable, 0
        );

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(emptyPage);

        Page<TaskResponseDTO> response =
                taskService.getFilteredTasks(
                        1L,
                        TaskStatus.COMPLETED,
                        TaskPriority.HIGH,
                        "Unknown",
                        pageable
                );

        assertEquals(0, response.getContent().size());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());

        verify(taskRepository).findAll(
                any(Specification.class), eq(pageable)
        );
    }

    @Test
    void getFilteredTasks_shouldApplyStatusAndPriorityFilters() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> taskPage = new PageImpl<>(
                List.of(
                        createTask(
                                1L, "Java Practice",
                                TaskStatus.PENDING,
                                TaskPriority.HIGH
                        )
                ),
                pageable,
                1
        );

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(taskPage);

        Page<TaskResponseDTO> response =
                taskService.getFilteredTasks(
                        1L,
                        TaskStatus.PENDING,
                        TaskPriority.HIGH,
                        null,
                        pageable
                );

        assertEquals(1, response.getContent().size());
        assertEquals(TaskStatus.PENDING,
                response.getContent().get(0).getStatus());
        assertEquals(TaskPriority.HIGH,
                response.getContent().get(0).getPriority());

        verify(taskRepository).findAll(
                any(Specification.class), eq(pageable)
        );
    }

    @Test
    void getFilteredTasks_shouldApplyKeywordFilter() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> taskPage = new PageImpl<>(
                List.of(
                        createTask(
                                1L, "Learn Java",
                                TaskStatus.PENDING,
                                TaskPriority.HIGH
                        )
                ),
                pageable,
                1
        );

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(taskPage);

        Page<TaskResponseDTO> response =
                taskService.getFilteredTasks(
                        1L, null, null, "Java", pageable
                );

        assertEquals(1, response.getContent().size());
        assertEquals("Learn Java",
                response.getContent().get(0).getTitle());

        verify(taskRepository).findAll(
                any(Specification.class), eq(pageable)
        );
    }

    @Test
    void getFilteredTasks_shouldTrimKeywordWhitespace() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> taskPage = new PageImpl<>(
                List.of(), pageable, 0
        );

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(taskPage);

        assertDoesNotThrow(() ->
                taskService.getFilteredTasks(
                        1L, null, null, "  Java  ", pageable
                )
        );

        verify(taskRepository).findAll(
                any(Specification.class), eq(pageable)
        );
    }

    // ==========================================
    // GET TASK BY ID
    // ==========================================

    @Test
    void getTaskById_shouldReturnTaskWhenIdExists() {

        Task task = createTask(
                1L, "Learn Spring Boot",
                TaskStatus.PENDING, TaskPriority.HIGH
        );

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        TaskResponseDTO response = taskService.getTaskById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Learn Spring Boot", response.getTitle());
        assertEquals(TaskPriority.HIGH, response.getPriority());

        verify(taskRepository).findById(1L);
    }

    @Test
    void getTaskById_shouldThrowExceptionWhenTaskNotFound() {

        when(taskRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.getTaskById(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(taskRepository).findById(999L);
    }

    // ==========================================
    // UPDATE TASK
    // ==========================================

    @Test
    void updateTask_shouldUpdateExistingTask() {

        Task existingTask = createTask(
                1L, "Learn Java",
                TaskStatus.PENDING, TaskPriority.LOW
        );

        TaskRequestDTO request = new TaskRequestDTO();
        request.setTitle("Learn Spring Boot");
        request.setDescription("Build REST APIs");
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setPriority(TaskPriority.HIGH);

        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(existingTask));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponseDTO response =
                taskService.updateTask(1L, request);

        assertEquals("Learn Spring Boot", response.getTitle());
        assertEquals("Build REST APIs", response.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        assertEquals(TaskPriority.HIGH, response.getPriority());

        verify(taskRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void updateTask_shouldThrowExceptionWhenTaskNotFound() {

        TaskRequestDTO request = new TaskRequestDTO();
        request.setTitle("Learn Spring Boot");
        request.setDescription("Build REST APIs");
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setPriority(TaskPriority.HIGH);

        when(taskRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.updateTask(999L, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(taskRepository).findById(999L);
        verify(taskRepository, never()).save(any(Task.class));
    }

    // ==========================================
    // DELETE TASK
    // ==========================================

    @Test
    void deleteTask_shouldDeleteTaskWhenIdExists() {

        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskRepository).existsById(1L);
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deleteTask_shouldThrowExceptionWhenTaskNotFound() {

        when(taskRepository.existsById(999L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.deleteTask(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(taskRepository).existsById(999L);
        verify(taskRepository, never()).deleteById(999L);
    }

    // ==========================================
    // HELPER METHOD
    // ==========================================

    private Task createTask(
            Long id,
            String title,
            TaskStatus status,
            TaskPriority priority) {

        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDescription("Test description");
        task.setStatus(status);
        task.setPriority(priority);

        return task;
    }
}


package com.taskflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

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
    void createTask_shouldReturnSavedTaskAndAssignOwner() {

        User user = createUser(USER_ID);

        TaskRequestDTO request = createRequest(
                "Learn Unit Testing",
                TaskStatus.PENDING,
                TaskPriority.HIGH
        );

        Task savedTask = createTask(
                1L,
                "Learn Unit Testing",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(taskRepository.save(any(Task.class)))
                .thenReturn(savedTask);

        TaskResponseDTO response =
                taskService.createTask(request, USER_ID);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Learn Unit Testing", response.getTitle());
        assertEquals(TaskStatus.PENDING, response.getStatus());
        assertEquals(TaskPriority.HIGH, response.getPriority());

        verify(userRepository).findById(USER_ID);

        verify(taskRepository).save(argThat(task ->
                task.getUser() != null
                        && task.getUser().getId().equals(USER_ID)
        ));
    }

    @Test
    void createTask_shouldThrowExceptionWhenUserNotFound() {

        TaskRequestDTO request = createRequest(
                "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH
        );

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.createTask(request, USER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(userRepository).findById(USER_ID);
        verify(taskRepository, never()).save(any(Task.class));
    }

    // ==========================================
    // GET ALL TASKS WITH PAGINATION
    // ==========================================

    @Test
    void getAllTasks_shouldReturnOnlyUserTasks() {

        Pageable pageable = PageRequest.of(
                0, 10, Sort.by("id").ascending()
        );

        User user = createUser(USER_ID);

        Task task = createTask(
                1L,
                "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        Page<Task> taskPage = new PageImpl<>(
                List.of(task), pageable, 1
        );

        when(taskRepository.findByUserId(USER_ID, pageable))
                .thenReturn(taskPage);

        Page<TaskResponseDTO> response =
                taskService.getAllTasks(USER_ID, pageable);

        assertEquals(1, response.getContent().size());
        assertEquals("Learn Java",
                response.getContent().get(0).getTitle());

        verify(taskRepository).findByUserId(USER_ID, pageable);
    }

    // ==========================================
    // FILTERED TASKS - PAGINATION
    // ==========================================

    @Test
    void getFilteredTasks_shouldReturnPaginatedTasks() {

        User user = createUser(USER_ID);

        Task task1 = createTask(
                1L, "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        Task task2 = createTask(
                2L, "Practice DSA",
                TaskStatus.IN_PROGRESS,
                TaskPriority.MEDIUM,
                user
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
                        USER_ID, null, null, null, pageable
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
                        USER_ID,
                        TaskStatus.COMPLETED,
                        TaskPriority.HIGH,
                        "Unknown",
                        pageable
                );

        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());

        verify(taskRepository).findAll(
                any(Specification.class), eq(pageable)
        );
    }

    @Test
    void getFilteredTasks_shouldApplyStatusAndPriorityFilters() {

        Pageable pageable = PageRequest.of(0, 10);

        User user = createUser(USER_ID);

        Task task = createTask(
                1L, "Java Practice",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        Page<Task> taskPage = new PageImpl<>(
                List.of(task), pageable, 1
        );

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(taskPage);

        Page<TaskResponseDTO> response =
                taskService.getFilteredTasks(
                        USER_ID,
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

        User user = createUser(USER_ID);

        Task task = createTask(
                1L, "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        Page<Task> taskPage = new PageImpl<>(
                List.of(task), pageable, 1
        );

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(taskPage);

        Page<TaskResponseDTO> response =
                taskService.getFilteredTasks(
                        USER_ID, null, null, "Java", pageable
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

        when(taskRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertDoesNotThrow(() ->
                taskService.getFilteredTasks(
                        USER_ID, null, null, "  Java  ", pageable
                )
        );

        verify(taskRepository).findAll(
                any(Specification.class), eq(pageable)
        );
    }

    @Test
    void getFilteredTasks_shouldRejectNullUserId() {

        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(
                IllegalArgumentException.class,
                () -> taskService.getFilteredTasks(
                        null, null, null, null, pageable
                )
        );

        verify(taskRepository, never()).findAll(
                any(Specification.class), any(Pageable.class)
        );
    }

    // ==========================================
    // FILTER TASKS BY STATUS
    // ==========================================

    @Test
    void getTasksByStatus_shouldFilterByUserAndStatus() {

        User user = createUser(USER_ID);

        Task task = createTask(
                1L, "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        when(taskRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(task));

        List<TaskResponseDTO> response =
                taskService.getTasksByStatus(
                        USER_ID, TaskStatus.PENDING
                );

        assertEquals(1, response.size());
        assertEquals(TaskStatus.PENDING, response.get(0).getStatus());

        verify(taskRepository).findAll(any(Specification.class));
    }

    // ==========================================
    // FILTER TASKS BY PRIORITY
    // ==========================================

    @Test
    void getTasksByPriority_shouldFilterByUserAndPriority() {

        User user = createUser(USER_ID);

        Task task = createTask(
                1L, "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        when(taskRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(task));

        List<TaskResponseDTO> response =
                taskService.getTasksByPriority(
                        USER_ID, TaskPriority.HIGH
                );

        assertEquals(1, response.size());
        assertEquals(TaskPriority.HIGH, response.get(0).getPriority());

        verify(taskRepository).findAll(any(Specification.class));
    }

    // ==========================================
    // FILTER BY STATUS AND PRIORITY
    // ==========================================

    @Test
    void getTasksByStatusAndPriority_shouldFilterByUser() {

        User user = createUser(USER_ID);

        Task task = createTask(
                1L, "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        when(taskRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(task));

        List<TaskResponseDTO> response =
                taskService.getTasksByStatusAndPriority(
                        USER_ID,
                        TaskStatus.PENDING,
                        TaskPriority.HIGH
                );

        assertEquals(1, response.size());

        verify(taskRepository).findAll(any(Specification.class));
    }

    // ==========================================
    // SEARCH TASKS BY TITLE
    // ==========================================

    @Test
    void searchTasksByTitle_shouldSearchWithinUserTasks() {

        User user = createUser(USER_ID);

        Task task = createTask(
                1L, "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        when(taskRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(task));

        List<TaskResponseDTO> response =
                taskService.searchTasksByTitle(USER_ID, "Java");

        assertEquals(1, response.size());
        assertEquals("Learn Java", response.get(0).getTitle());

        verify(taskRepository).findAll(any(Specification.class));
    }

    // ==========================================
    // GET TASK BY ID
    // ==========================================

    @Test
    void getTaskById_shouldReturnTaskWhenOwnedByUser() {

        User user = createUser(USER_ID);

        Task task = createTask(
                1L, "Learn Spring Boot",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        when(taskRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        TaskResponseDTO response =
                taskService.getTaskById(1L, USER_ID);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Learn Spring Boot", response.getTitle());

        verify(taskRepository).findByIdAndUserId(1L, USER_ID);
    }

    @Test
    void getTaskById_shouldThrowExceptionWhenTaskNotOwnedByUser() {

        when(taskRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.getTaskById(1L, USER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(taskRepository).findByIdAndUserId(1L, USER_ID);
    }

    // ==========================================
    // UPDATE TASK
    // ==========================================

    @Test
    void updateTask_shouldUpdateOwnedTaskWithoutChangingOwner() {

        User owner = createUser(USER_ID);
        User anotherUser = createUser(OTHER_USER_ID);

        Task existingTask = createTask(
                1L, "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.LOW,
                owner
        );

        TaskRequestDTO request = createRequest(
                "Learn Spring Boot",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH
        );

        when(taskRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.of(existingTask));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponseDTO response =
                taskService.updateTask(1L, request, USER_ID);

        assertEquals("Learn Spring Boot", response.getTitle());
        assertEquals("Build REST APIs", response.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        assertEquals(TaskPriority.HIGH, response.getPriority());

        // The owner must remain unchanged.
        assertEquals(USER_ID, existingTask.getUser().getId());
        assertNotEquals(anotherUser.getId(),
                existingTask.getUser().getId());

        verify(taskRepository).findByIdAndUserId(1L, USER_ID);
        verify(taskRepository).save(existingTask);
    }

    @Test
    void updateTask_shouldThrowExceptionWhenTaskNotOwnedByUser() {

        TaskRequestDTO request = createRequest(
                "Learn Spring Boot",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH
        );

        when(taskRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.updateTask(1L, request, USER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(taskRepository).findByIdAndUserId(1L, USER_ID);
        verify(taskRepository, never()).save(any(Task.class));
    }

    // ==========================================
    // DELETE TASK
    // ==========================================

    @Test
    void deleteTask_shouldDeleteOwnedTask() {

        User user = createUser(USER_ID);

        Task task = createTask(
                1L, "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                user
        );

        when(taskRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.deleteTask(1L, USER_ID);

        verify(taskRepository).findByIdAndUserId(1L, USER_ID);
        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_shouldNotDeleteTaskNotOwnedByUser() {

        when(taskRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.deleteTask(1L, USER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(taskRepository).findByIdAndUserId(1L, USER_ID);
        verify(taskRepository, never()).delete(any(Task.class));
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private User createUser(Long id) {

        User user = new User();
        user.setId(id);
        user.setName("Test User");
        user.setEmail("user" + id + "@example.com");
        user.setPassword("encodedPassword");

        return user;
    }

    private TaskRequestDTO createRequest(
            String title,
            TaskStatus status,
            TaskPriority priority) {

        TaskRequestDTO request = new TaskRequestDTO();

        request.setTitle(title);
        request.setDescription("Build REST APIs");
        request.setStatus(status);
        request.setPriority(priority);

        return request;
    }

    private Task createTask(
            Long id,
            String title,
            TaskStatus status,
            TaskPriority priority,
            User user) {

        Task task = new Task();

        task.setId(id);
        task.setTitle(title);
        task.setDescription("Test description");
        task.setStatus(status);
        task.setPriority(priority);
        task.setUser(user);

        return task;
    }
}

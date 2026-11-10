
package com.taskflow.controller;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import org.springframework.web.server.ResponseStatusException;

import com.taskflow.dto.TaskRequestDTO;
import com.taskflow.dto.TaskResponseDTO;
import com.taskflow.entity.Role;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.User;
import com.taskflow.security.CustomUserDetails;
import com.taskflow.service.TaskService;

import static org.hamcrest.Matchers.nullValue;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.TaskService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    // ==========================================
    // AUTHENTICATION HELPER
    // ==========================================

    private Authentication testAuthentication() {

        User user = new User();
        user.setId(USER_ID);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);

        CustomUserDetails principal =
                new CustomUserDetails(user);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor
            authenticatedRequest() {

        Authentication authentication = testAuthentication();

        return request -> {
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request) {

        return request.with(authenticatedRequest());
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private TaskResponseDTO createResponse(
            Long id,
            String title,
            TaskStatus taskStatus,
            TaskPriority priority) {

        TaskResponseDTO task = new TaskResponseDTO();

        task.setId(id);
        task.setTitle(title);
        task.setDescription("Test description");
        task.setStatus(taskStatus);
        task.setPriority(priority);

        return task;
    }

    private Page<TaskResponseDTO> createTaskPage(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        List<TaskResponseDTO> tasks = List.of(
                createResponse(
                        1L,
                        "Learn Java",
                        TaskStatus.PENDING,
                        TaskPriority.HIGH
                ),
                createResponse(
                        2L,
                        "Learn Spring Boot",
                        TaskStatus.IN_PROGRESS,
                        TaskPriority.MEDIUM
                )
        );

        return new PageImpl<>(tasks, pageable, 2);
    }

    private void stubDefaultPage() {

        when(taskService.getFilteredTasks(
                eq(USER_ID),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(createTaskPage(0, 10, "id", "asc"));
    }

    // ==========================================
    // CREATE TASK
    // ==========================================

    @Test
    void createTask_shouldReturnCreatedTask() throws Exception {

        TaskResponseDTO response = createResponse(
                1L,
                "Learn Spring Boot",
                TaskStatus.PENDING,
                TaskPriority.HIGH
        );

        when(taskService.createTask(
                any(TaskRequestDTO.class),
                eq(USER_ID)
        )).thenReturn(response);

        mockMvc.perform(authenticated(post("/api/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Learn Spring Boot",
                            "description": "Complete controller testing",
                            "status": "PENDING",
                            "priority": "HIGH"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Task created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title")
                        .value("Learn Spring Boot"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));

        verify(taskService).createTask(
                any(TaskRequestDTO.class),
                eq(USER_ID)
        );
    }

    @Test
    void createTask_shouldRejectEmptyTitle() throws Exception {

        mockMvc.perform(authenticated(post("/api/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "",
                            "description": "Test description",
                            "status": "PENDING",
                            "priority": "HIGH"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(taskService, never()).createTask(
                any(TaskRequestDTO.class),
                any(Long.class)
        );
    }

    @Test
    void createTask_shouldRejectMissingPriority() throws Exception {

        mockMvc.perform(authenticated(post("/api/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Learn Spring Boot",
                            "description": "Practice testing",
                            "status": "PENDING"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(taskService, never()).createTask(
                any(TaskRequestDTO.class),
                any(Long.class)
        );
    }

    @Test
    void createTask_shouldRejectInvalidPriority() throws Exception {

        mockMvc.perform(authenticated(post("/api/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Learn Spring Boot",
                            "description": "Practice testing",
                            "status": "PENDING",
                            "priority": "URGENT"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(taskService, never()).createTask(
                any(TaskRequestDTO.class),
                any(Long.class)
        );
    }

    // ==========================================
    // GET ALL TASKS WITH PAGINATION
    // ==========================================

    @Test
    void getAllTasks_shouldReturnPaginatedTasks() throws Exception {

        stubDefaultPage();

        mockMvc.perform(authenticated(get("/api/tasks")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title")
                        .value("Learn Java"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));

        verify(taskService).getFilteredTasks(
                eq(USER_ID),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldAcceptCustomPagination() throws Exception {

        when(taskService.getFilteredTasks(
                eq(USER_ID),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(createTaskPage(1, 2, "id", "asc"));

        mockMvc.perform(authenticated(get("/api/tasks"))
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2));
    }

    @Test
    void getAllTasks_shouldRejectNegativePage() throws Exception {

        mockMvc.perform(authenticated(get("/api/tasks"))
                .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).getFilteredTasks(
                anyLong(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldRejectInvalidPageSize() throws Exception {

        mockMvc.perform(authenticated(get("/api/tasks"))
                .param("size", "101"))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).getFilteredTasks(
                anyLong(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldFilterByStatus() throws Exception {

        when(taskService.getFilteredTasks(
                eq(USER_ID),
                eq(TaskStatus.PENDING),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(createTaskPage(0, 10, "id", "asc"));

        mockMvc.perform(authenticated(get("/api/tasks"))
                .param("status", "PENDING"))
                .andExpect(status().isOk());

        verify(taskService).getFilteredTasks(
                eq(USER_ID),
                eq(TaskStatus.PENDING),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldFilterByPriority() throws Exception {

        when(taskService.getFilteredTasks(
                eq(USER_ID),
                isNull(),
                eq(TaskPriority.HIGH),
                isNull(),
                any(Pageable.class)
        )).thenReturn(createTaskPage(0, 10, "id", "asc"));

        mockMvc.perform(authenticated(get("/api/tasks"))
                .param("priority", "HIGH"))
                .andExpect(status().isOk());

        verify(taskService).getFilteredTasks(
                eq(USER_ID),
                isNull(),
                eq(TaskPriority.HIGH),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldSearchByKeyword() throws Exception {

        when(taskService.getFilteredTasks(
                eq(USER_ID),
                isNull(),
                isNull(),
                eq("Java"),
                any(Pageable.class)
        )).thenReturn(createTaskPage(0, 10, "id", "asc"));

        mockMvc.perform(authenticated(get("/api/tasks"))
                .param("keyword", "Java"))
                .andExpect(status().isOk());

        verify(taskService).getFilteredTasks(
                eq(USER_ID),
                isNull(),
                isNull(),
                eq("Java"),
                any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldRejectInvalidStatus() throws Exception {

        mockMvc.perform(authenticated(get("/api/tasks"))
                .param("status", "INVALID"))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).getFilteredTasks(
                anyLong(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldRejectInvalidSortField() throws Exception {

        mockMvc.perform(authenticated(get("/api/tasks"))
                .param("sortBy", "unknown"))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).getFilteredTasks(
                anyLong(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        );
    }

    // ==========================================
    // FILTER ENDPOINTS
    // ==========================================

    @Test
    void getTasksByStatus_shouldPassAuthenticatedUserId()
            throws Exception {

        when(taskService.getTasksByStatus(
                USER_ID, TaskStatus.PENDING
        )).thenReturn(List.of());

        mockMvc.perform(authenticated(get("/api/tasks/filter/status"))
                .param("status", "PENDING"))
                .andExpect(status().isOk());

        verify(taskService).getTasksByStatus(
                USER_ID, TaskStatus.PENDING
        );
    }

    @Test
    void getTasksByPriority_shouldPassAuthenticatedUserId()
            throws Exception {

        when(taskService.getTasksByPriority(
                USER_ID, TaskPriority.HIGH
        )).thenReturn(List.of());

        mockMvc.perform(authenticated(get("/api/tasks/filter/priority"))
                .param("priority", "HIGH"))
                .andExpect(status().isOk());

        verify(taskService).getTasksByPriority(
                USER_ID, TaskPriority.HIGH
        );
    }

    @Test
    void getTasksByStatusAndPriority_shouldPassAuthenticatedUserId()
            throws Exception {

        when(taskService.getTasksByStatusAndPriority(
                USER_ID,
                TaskStatus.PENDING,
                TaskPriority.HIGH
        )).thenReturn(List.of());

        mockMvc.perform(authenticated(get("/api/tasks/filter"))
                .param("status", "PENDING")
                .param("priority", "HIGH"))
                .andExpect(status().isOk());

        verify(taskService).getTasksByStatusAndPriority(
                USER_ID,
                TaskStatus.PENDING,
                TaskPriority.HIGH
        );
    }

    // ==========================================
    // SEARCH
    // ==========================================

    @Test
    void searchTasks_shouldPassAuthenticatedUserId()
            throws Exception {

        when(taskService.searchTasksByTitle(USER_ID, "Java"))
                .thenReturn(List.of());

        mockMvc.perform(authenticated(get("/api/tasks/search"))
                .param("keyword", "Java"))
                .andExpect(status().isOk());

        verify(taskService).searchTasksByTitle(USER_ID, "Java");
    }

    // ==========================================
    // GET TASK BY ID
    // ==========================================

    @Test
    void getTaskById_shouldReturnOwnedTask() throws Exception {

        TaskResponseDTO task = createResponse(
                1L,
                "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH
        );

        when(taskService.getTaskById(1L, USER_ID))
                .thenReturn(task);

        mockMvc.perform(authenticated(get("/api/tasks/1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Learn Java"));

        verify(taskService).getTaskById(1L, USER_ID);
    }

    @Test
    void getTaskById_shouldReturnNotFoundWhenTaskIsNotOwned()
            throws Exception {

        when(taskService.getTaskById(9999L, USER_ID))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Task not found"
                ));

        mockMvc.perform(authenticated(get("/api/tasks/9999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Task not found"));

        verify(taskService).getTaskById(9999L, USER_ID);
    }

    // ==========================================
    // UPDATE TASK
    // ==========================================

    @Test
    void updateTask_shouldUpdateOwnedTask() throws Exception {

        TaskResponseDTO response = createResponse(
                1L,
                "Learn Advanced Java",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH
        );

        when(taskService.updateTask(
                eq(1L),
                any(TaskRequestDTO.class),
                eq(USER_ID)
        )).thenReturn(response);

        mockMvc.perform(authenticated(put("/api/tasks/1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Learn Advanced Java",
                            "description": "Updated Java concepts",
                            "status": "IN_PROGRESS",
                            "priority": "HIGH"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title")
                        .value("Learn Advanced Java"));

        verify(taskService).updateTask(
                eq(1L),
                any(TaskRequestDTO.class),
                eq(USER_ID)
        );
    }

    @Test
    void updateTask_shouldReturnNotFoundForUnownedTask()
            throws Exception {

        when(taskService.updateTask(
                eq(9999L),
                any(TaskRequestDTO.class),
                eq(USER_ID)
        )).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Task not found"
        ));

        mockMvc.perform(authenticated(put("/api/tasks/9999"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Updated Task",
                            "description": "Testing ownership",
                            "status": "IN_PROGRESS",
                            "priority": "HIGH"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));

        verify(taskService).updateTask(
                eq(9999L),
                any(TaskRequestDTO.class),
                eq(USER_ID)
        );
    }

    // ==========================================
    // DELETE TASK
    // ==========================================

    @Test
    void deleteTask_shouldDeleteOwnedTask() throws Exception {

        mockMvc.perform(authenticated(delete("/api/tasks/1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Task deleted successfully"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verify(taskService).deleteTask(1L, USER_ID);
    }

    @Test
    void deleteTask_shouldReturnNotFoundForUnownedTask()
            throws Exception {

        doThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Task not found"
        )).when(taskService).deleteTask(9999L, USER_ID);

        mockMvc.perform(authenticated(delete("/api/tasks/9999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Task not found"));

        verify(taskService).deleteTask(9999L, USER_ID);
    }
}

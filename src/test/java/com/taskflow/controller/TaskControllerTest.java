
package com.taskflow.controller;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.taskflow.dto.TaskRequestDTO;
import com.taskflow.dto.TaskResponseDTO;
import com.taskflow.entity.Role;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.User;
import com.taskflow.security.CustomUserDetails;
import com.taskflow.service.TaskService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.nullValue;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

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

        CustomUserDetails principal = new CustomUserDetails(user);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    private RequestPostProcessor authenticatedRequest() {

        Authentication authentication = testAuthentication();

        return request -> {
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    /*
     * These helper methods automatically attach an authenticated
     * principal to every MockMvc request.
     */

    private MockHttpServletRequestBuilder get(String uri) {
        return org.springframework.test.web.servlet.request
                .MockMvcRequestBuilders.get(uri)
                .with(authenticatedRequest());
    }

    private MockHttpServletRequestBuilder post(String uri) {
        return org.springframework.test.web.servlet.request
                .MockMvcRequestBuilders.post(uri)
                .with(authenticatedRequest());
    }

    private MockHttpServletRequestBuilder put(String uri) {
        return org.springframework.test.web.servlet.request
                .MockMvcRequestBuilders.put(uri)
                .with(authenticatedRequest());
    }

    private MockHttpServletRequestBuilder delete(String uri) {
        return org.springframework.test.web.servlet.request
                .MockMvcRequestBuilders.delete(uri)
                .with(authenticatedRequest());
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
    // CREATE TASK TESTS
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

        mockMvc.perform(post("/api/tasks")
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
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(taskService).createTask(
                any(TaskRequestDTO.class), eq(USER_ID)
        );
    }

    @Test
    void createTask_shouldReturnBadRequestWhenTitleIsEmpty()
            throws Exception {

        mockMvc.perform(post("/api/tasks")
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
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed. Please check the provided fields."))
                .andExpect(jsonPath("$.data.title").exists());

        verify(taskService, never()).createTask(
                any(TaskRequestDTO.class), any(Long.class)
        );
    }

    @Test
    void createTask_shouldReturnBadRequestWhenPriorityIsMissing()
            throws Exception {

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Learn Spring Boot",
                            "description": "Practice testing",
                            "status": "PENDING"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.priority").exists());

        verify(taskService, never()).createTask(
                any(TaskRequestDTO.class), any(Long.class)
        );
    }

    @Test
    void createTask_shouldReturnBadRequestWhenPriorityIsInvalid()
            throws Exception {

        mockMvc.perform(post("/api/tasks")
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
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").value(nullValue()));

        verify(taskService, never()).createTask(
                any(TaskRequestDTO.class), any(Long.class)
        );
    }

    @Test
    void createTask_shouldReturnBadRequestWhenTitleExceedsLimit()
            throws Exception {

        String longTitle = "A".repeat(101);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "%s",
                            "description": "Testing title length",
                            "status": "PENDING",
                            "priority": "HIGH"
                        }
                        """.formatted(longTitle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.title").exists());

        verify(taskService, never()).createTask(
                any(TaskRequestDTO.class), any(Long.class)
        );
    }

    @Test
    void createTask_shouldReturnBadRequestWhenDescriptionExceedsLimit()
            throws Exception {

        String longDescription = "A".repeat(501);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Learn Spring Boot",
                            "description": "%s",
                            "status": "PENDING",
                            "priority": "HIGH"
                        }
                        """.formatted(longDescription)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.description").exists());

        verify(taskService, never()).createTask(
                any(TaskRequestDTO.class), any(Long.class)
        );
    }

    // ==========================================
    // GET ALL TASKS - PAGINATION
    // ==========================================

    @Test
    void getAllTasks_shouldReturnPaginatedTasks() throws Exception {

        stubDefaultPage();

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Tasks retrieved successfully"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title")
                        .value("Learn Java"))
                .andExpect(jsonPath("$.data.content[1].title")
                        .value("Learn Spring Boot"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.empty").value(false));

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

        mockMvc.perform(get("/api/tasks")
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2));
    }

    @Test
    void getAllTasks_shouldReturnBadRequestForNegativePage()
            throws Exception {

        mockMvc.perform(get("/api/tasks")
                .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Page number cannot be negative"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verify(taskService, never()).getFilteredTasks(
                anyLong(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldReturnBadRequestForZeroSize()
            throws Exception {

        mockMvc.perform(get("/api/tasks")
                .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(taskService, never()).getFilteredTasks(
                anyLong(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldReturnBadRequestWhenSizeExceedsLimit()
            throws Exception {

        mockMvc.perform(get("/api/tasks")
                .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(taskService, never()).getFilteredTasks(
                anyLong(), any(), any(), any(), any(Pageable.class)
        );
    }

    // ==========================================
    // FILTERING AND SEARCH
    // ==========================================

    @Test
    void getAllTasks_shouldFilterByStatus() throws Exception {

        when(taskService.getFilteredTasks(
                eq(USER_ID),
                eq(TaskStatus.PENDING),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(createTaskPage(0, 10, "id", "asc"));

        mockMvc.perform(get("/api/tasks")
                .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

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

        mockMvc.perform(get("/api/tasks")
                .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

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

        mockMvc.perform(get("/api/tasks")
                .param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(taskService).getFilteredTasks(
                eq(USER_ID),
                isNull(),
                isNull(),
                eq("Java"),
                any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldAcceptEmptyKeyword() throws Exception {

        when(taskService.getFilteredTasks(
                eq(USER_ID),
                isNull(),
                isNull(),
                eq(""),
                any(Pageable.class)
        )).thenReturn(createTaskPage(0, 10, "id", "asc"));

        mockMvc.perform(get("/api/tasks")
                .param("keyword", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(taskService).getFilteredTasks(
                eq(USER_ID),
                isNull(),
                isNull(),
                eq(""),
                any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldCombineFiltersAndPagination()
            throws Exception {

        when(taskService.getFilteredTasks(
                eq(USER_ID),
                eq(TaskStatus.PENDING),
                eq(TaskPriority.HIGH),
                eq("Java"),
                any(Pageable.class)
        )).thenReturn(createTaskPage(0, 2, "title", "desc"));

        mockMvc.perform(get("/api/tasks")
                .param("status", "PENDING")
                .param("priority", "HIGH")
                .param("keyword", "Java")
                .param("page", "0")
                .param("size", "2")
                .param("sortBy", "title")
                .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(taskService).getFilteredTasks(
                eq(USER_ID),
                eq(TaskStatus.PENDING),
                eq(TaskPriority.HIGH),
                eq("Java"),
                any(Pageable.class)
        );
    }

    // ==========================================
    // INVALID QUERY PARAMETERS
    // ==========================================

    @Test
    void getAllTasks_shouldRejectInvalidStatus() throws Exception {

        mockMvc.perform(get("/api/tasks")
                .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(taskService, never()).getFilteredTasks(
                anyLong(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldRejectInvalidPriority() throws Exception {

        mockMvc.perform(get("/api/tasks")
                .param("priority", "URGENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(taskService, never()).getFilteredTasks(
                anyLong(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldRejectInvalidSortField() throws Exception {

        mockMvc.perform(get("/api/tasks")
                .param("sortBy", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(taskService, never()).getFilteredTasks(
                anyLong(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void getAllTasks_shouldRejectInvalidSortDirection()
            throws Exception {

        mockMvc.perform(get("/api/tasks")
                .param("direction", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(taskService, never()).getFilteredTasks(
                anyLong(), any(), any(), any(), any(Pageable.class)
        );
    }

    // ==========================================
    // GET TASK BY ID
    // ==========================================

    @Test
    void getTaskById_shouldReturnTaskWhenIdExists() throws Exception {

        TaskResponseDTO task = createResponse(
                1L,
                "Learn Java",
                TaskStatus.PENDING,
                TaskPriority.HIGH
        );

        when(taskService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Learn Java"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    void getTaskById_shouldReturnNotFoundWhenTaskDoesNotExist()
            throws Exception {

        when(taskService.getTaskById(9999L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found"
                ));

        mockMvc.perform(get("/api/tasks/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Task not found"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ==========================================
    // UPDATE TASK
    // ==========================================

    @Test
    void updateTask_shouldReturnUpdatedTask() throws Exception {

        TaskResponseDTO response = createResponse(
                1L,
                "Learn Advanced Java",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH
        );

        when(taskService.updateTask(
                eq(1L),
                any(TaskRequestDTO.class)
        )).thenReturn(response);

        mockMvc.perform(put("/api/tasks/1")
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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title")
                        .value("Learn Advanced Java"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    void updateTask_shouldReturnNotFoundWhenTaskDoesNotExist()
            throws Exception {

        when(taskService.updateTask(
                eq(9999L),
                any(TaskRequestDTO.class)
        )).thenThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task not found"
        ));

        mockMvc.perform(put("/api/tasks/9999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Updated Task",
                            "description": "Testing nonexistent task",
                            "status": "IN_PROGRESS",
                            "priority": "HIGH"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Task not found"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    // ==========================================
    // DELETE TASK
    // ==========================================

    @Test
    void deleteTask_shouldReturnOkWhenTaskIsDeleted()
            throws Exception {

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Task deleted successfully"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(taskService).deleteTask(1L);
    }

    @Test
    void deleteTask_shouldReturnNotFoundWhenTaskDoesNotExist()
            throws Exception {

        doThrow(
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task not found"
                )
        ).when(taskService).deleteTask(9999L);

        mockMvc.perform(delete("/api/tasks/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Task not found"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verify(taskService).deleteTask(9999L);
    }
}

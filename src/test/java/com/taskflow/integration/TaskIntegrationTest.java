
package com.taskflow.integration;

import com.taskflow.entity.Task;
import com.taskflow.repository.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
class TaskIntegrationTest {

    static final MySQLContainer MYSQL_CONTAINER =
            new MySQLContainer(DockerImageName.parse("mysql:8.0.45"))
                    .withDatabaseName("taskflow_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url",
                MYSQL_CONTAINER::getJdbcUrl);

        registry.add("spring.datasource.username",
                MYSQL_CONTAINER::getUsername);

        registry.add("spring.datasource.password",
                MYSQL_CONTAINER::getPassword);

        registry.add("spring.datasource.driver-class-name",
                MYSQL_CONTAINER::getDriverClassName);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
    }

    // CREATE TASK AND VERIFY DATABASE PERSISTENCE
    @Test
    void createTask_shouldPersistTaskInDatabase() throws Exception {

        String requestBody = """
                {
                    "title": "Java Integration",
                    "description": "Testing database integration",
                    "status": "PENDING",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task created successfully"))
                .andExpect(jsonPath("$.data.title").value("Java Integration"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(1, taskRepository.count());
    }

    // GET TASK BY ID
    @Test
    void getTaskById_shouldReturnPersistedTask() throws Exception {

        createTask("Database Testing", "PENDING", "MEDIUM");

        Task savedTask = taskRepository.findAll().get(0);

        mockMvc.perform(get("/api/tasks/{id}", savedTask.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(savedTask.getId()))
                .andExpect(jsonPath("$.data.title").value("Database Testing"));
    }

    // UPDATE TASK
    @Test
    void updateTask_shouldUpdateDatabaseRecord() throws Exception {

        createTask("Old Title", "PENDING", "LOW");

        Task savedTask = taskRepository.findAll().get(0);

        String updatedRequest = """
                {
                    "title": "Updated Title",
                    "description": "Updated description",
                    "status": "IN_PROGRESS",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(put("/api/tasks/{id}", savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        Task updatedTask = taskRepository.findById(savedTask.getId())
                .orElseThrow();

        assertEquals("Updated Title", updatedTask.getTitle());
        assertEquals("IN_PROGRESS", updatedTask.getStatus().name());
    }

    // DELETE TASK
    @Test
    void deleteTask_shouldRemoveRecordFromDatabase() throws Exception {

        createTask("Task To Delete", "PENDING", "LOW");

        Task savedTask = taskRepository.findAll().get(0);

        mockMvc.perform(delete("/api/tasks/{id}", savedTask.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertFalse(taskRepository.existsById(savedTask.getId()));
    }

    // REAL DATABASE SPECIFICATION FILTERING
    @Test
    void getTasks_shouldApplyCombinedFilters() throws Exception {

        createTask("Java Backend", "PENDING", "HIGH");
        createTask("Java Frontend", "COMPLETED", "HIGH");
        createTask("Python Backend", "PENDING", "HIGH");

        mockMvc.perform(get("/api/tasks")
                        .param("status", "PENDING")
                        .param("priority", "HIGH")
                        .param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title")
                        .value("Java Backend"));
    }

    // REAL DATABASE PAGINATION AND SORTING
    @Test
    void getTasks_shouldApplyPaginationAndSorting() throws Exception {

        createTask("Charlie", "PENDING", "LOW");
        createTask("Alpha", "PENDING", "MEDIUM");
        createTask("Beta", "COMPLETED", "HIGH");

        mockMvc.perform(get("/api/tasks")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "title")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title").value("Alpha"))
                .andExpect(jsonPath("$.data.content[1].title").value("Beta"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2));
    }

    // INVALID PAGINATION
    @Test
    void getTasks_shouldRejectNegativePage() throws Exception {

        mockMvc.perform(get("/api/tasks")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Page number cannot be negative"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // HELPER METHOD TO CREATE TASKS THROUGH THE REAL API
    private void createTask(
            String title,
            String status,
            String priority) throws Exception {

        String requestBody = """
                {
                    "title": "%s",
                    "description": "Integration test task",
                    "status": "%s",
                    "priority": "%s"
                }
                """.formatted(title, status, priority);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }
}

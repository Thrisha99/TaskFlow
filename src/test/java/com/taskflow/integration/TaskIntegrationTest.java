
package com.taskflow.integration;

import com.jayway.jsonpath.JsonPath;
import com.taskflow.entity.Task;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "jwt.secret=VGhpc0lzQVN1ZmZpY2llbnRseUxvbmdUZXN0U2VjcmV0S2V5Rm9ySldU",
        "jwt.expiration=900000"
})
@AutoConfigureMockMvc
class TaskIntegrationTest {

    // ==========================================
    // TESTCONTAINERS MYSQL CONFIGURATION
    // ==========================================

    static final MySQLContainer MYSQL_CONTAINER =
            new MySQLContainer(
                    DockerImageName.parse("mysql:8.0.45")
            )
                    .withDatabaseName("taskflow_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                MYSQL_CONTAINER::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                MYSQL_CONTAINER::getUsername
        );

        registry.add(
                "spring.datasource.password",
                MYSQL_CONTAINER::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL_CONTAINER::getDriverClassName
        );
    }

    // ==========================================
    // DEPENDENCY INJECTION
    // ==========================================

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    // ==========================================
    // TEST CONSTANTS
    // ==========================================

    private static final String USER_A_EMAIL =
            "usera@example.com";

    private static final String USER_B_EMAIL =
            "userb@example.com";

    private static final String PASSWORD =
            "Password123";

    record TestUser(Long id, String token) {
    }

    // ==========================================
    // DATABASE CLEANUP
    // ==========================================

    @BeforeEach
    void cleanDatabase() {

        // Delete tasks before deleting their owners.
        taskRepository.deleteAll();

        // Delete users after all dependent tasks are removed.
        userRepository.deleteAll();
    }

    // ==========================================
    // AUTHENTICATION HELPER
    // ==========================================

    private TestUser registerAndLogin(
            String name,
            String email) throws Exception {

        // --------------------------------------
        // STEP 1: REGISTER USER
        // --------------------------------------

        String registerBody = """
                {
                    "name": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(name, email, PASSWORD);

        MvcResult registerResult = mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String registerResponse =
                registerResult.getResponse().getContentAsString();

        // JsonPath may return Integer or Long.
        Number userIdNumber = JsonPath.read(
                registerResponse,
                "$.data.id"
        );

        Long userId = userIdNumber.longValue();

        // --------------------------------------
        // STEP 2: LOGIN USER
        // --------------------------------------

        String loginBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, PASSWORD);

        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String loginResponse =
                loginResult.getResponse().getContentAsString();

        String token = JsonPath.read(
                loginResponse,
                "$.data.token"
        );

        assertNotNull(userId);
        assertFalse(token.isBlank());

        return new TestUser(userId, token);
    }

    // ==========================================
    // USER CREATION HELPERS
    // ==========================================

    private TestUser createUserA() throws Exception {

        return registerAndLogin(
                "User A",
                USER_A_EMAIL
        );
    }

    private TestUser createUserB() throws Exception {

        return registerAndLogin(
                "User B",
                USER_B_EMAIL
        );
    }

    // ==========================================
    // TASK CREATION HELPER
    // ==========================================

    private Long createTask(
            TestUser user,
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

        MvcResult result = mockMvc.perform(
                post("/api/tasks")
                        .header(
                                "Authorization",
                                "Bearer " + user.token()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String responseBody =
                result.getResponse().getContentAsString();

        Number taskIdNumber = JsonPath.read(
                responseBody,
                "$.data.id"
        );

        return taskIdNumber.longValue();
    }

    // ==========================================
    // TEST 1: CREATE TASK
    // ==========================================

    @Test
    void createTask_shouldPersistTaskInDatabase() throws Exception {

        TestUser user = createUserA();

        Long taskId = createTask(
                user,
                "Java Integration",
                "PENDING",
                "HIGH"
        );

        assertEquals(1, taskRepository.count());

        Task savedTask = taskRepository
                .findByIdAndUserId(taskId, user.id())
                .orElseThrow();

        assertEquals("Java Integration", savedTask.getTitle());
        assertEquals(user.id(), savedTask.getUser().getId());
    }

    // ==========================================
    // TEST 2: GET TASK BY ID
    // ==========================================

    @Test
    void getTaskById_shouldReturnPersistedTask() throws Exception {

        TestUser user = createUserA();

        Long taskId = createTask(
                user,
                "Database Testing",
                "PENDING",
                "MEDIUM"
        );

        mockMvc.perform(
                get("/api/tasks/{id}", taskId)
                        .header(
                                "Authorization",
                                "Bearer " + user.token()
                        )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.title")
                        .value("Database Testing"));
    }

    // ==========================================
    // TEST 3: UPDATE TASK
    // ==========================================

    @Test
    void updateTask_shouldUpdateDatabaseRecord() throws Exception {

        TestUser user = createUserA();

        Long taskId = createTask(
                user,
                "Old Title",
                "PENDING",
                "LOW"
        );

        String updatedRequest = """
                {
                    "title": "Updated Title",
                    "description": "Updated description",
                    "status": "IN_PROGRESS",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(
                put("/api/tasks/{id}", taskId)
                        .header(
                                "Authorization",
                                "Bearer " + user.token()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedRequest)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title")
                        .value("Updated Title"))
                .andExpect(jsonPath("$.data.status")
                        .value("IN_PROGRESS"));

        Task updatedTask = taskRepository
                .findByIdAndUserId(taskId, user.id())
                .orElseThrow();

        assertEquals("Updated Title", updatedTask.getTitle());
        assertEquals("IN_PROGRESS", updatedTask.getStatus().name());
        assertEquals(user.id(), updatedTask.getUser().getId());
    }

    // ==========================================
    // TEST 4: DELETE TASK
    // ==========================================

    @Test
    void deleteTask_shouldRemoveRecordFromDatabase() throws Exception {

        TestUser user = createUserA();

        Long taskId = createTask(
                user,
                "Task To Delete",
                "PENDING",
                "LOW"
        );

        mockMvc.perform(
                delete("/api/tasks/{id}", taskId)
                        .header(
                                "Authorization",
                                "Bearer " + user.token()
                        )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Task deleted successfully"));

        assertFalse(taskRepository.existsById(taskId));
    }

    // ==========================================
    // TEST 5: SPECIFICATION FILTERING
    // ==========================================

    @Test
    void getTasks_shouldApplyCombinedFilters() throws Exception {

        TestUser user = createUserA();

        createTask(user, "Java Backend", "PENDING", "HIGH");
        createTask(user, "Java Frontend", "COMPLETED", "HIGH");
        createTask(user, "Python Backend", "PENDING", "HIGH");

        mockMvc.perform(
                get("/api/tasks")
                        .header(
                                "Authorization",
                                "Bearer " + user.token()
                        )
                        .param("status", "PENDING")
                        .param("priority", "HIGH")
                        .param("keyword", "Java")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title")
                        .value("Java Backend"));
    }

    // ==========================================
    // TEST 6: PAGINATION AND SORTING
    // ==========================================

    @Test
    void getTasks_shouldApplyPaginationAndSorting() throws Exception {

        TestUser user = createUserA();

        createTask(user, "Charlie", "PENDING", "LOW");
        createTask(user, "Alpha", "PENDING", "MEDIUM");
        createTask(user, "Beta", "COMPLETED", "HIGH");

        mockMvc.perform(
                get("/api/tasks")
                        .header(
                                "Authorization",
                                "Bearer " + user.token()
                        )
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "title")
                        .param("direction", "asc")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title")
                        .value("Alpha"))
                .andExpect(jsonPath("$.data.content[1].title")
                        .value("Beta"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2));
    }

    // ==========================================
    // TEST 7: INVALID PAGINATION
    // ==========================================

    @Test
    void getTasks_shouldRejectNegativePage() throws Exception {

        TestUser user = createUserA();

        mockMvc.perform(
                get("/api/tasks")
                        .header(
                                "Authorization",
                                "Bearer " + user.token()
                        )
                        .param("page", "-1")
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Page number cannot be negative"));
    }

    // ==========================================
    // TEST 8: USER TASK OWNERSHIP
    // ==========================================

    @Test
    void users_shouldOnlyRetrieveTheirOwnTasks() throws Exception {

        TestUser userA = createUserA();
        TestUser userB = createUserB();

        createTask(
                userA,
                "User A Private Task",
                "PENDING",
                "HIGH"
        );

        createTask(
                userB,
                "User B Private Task",
                "IN_PROGRESS",
                "MEDIUM"
        );

        // User A should see only their own task.
        mockMvc.perform(
                get("/api/tasks")
                        .header(
                                "Authorization",
                                "Bearer " + userA.token()
                        )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title")
                        .value("User A Private Task"));

        // User B should see only their own task.
        mockMvc.perform(
                get("/api/tasks")
                        .header(
                                "Authorization",
                                "Bearer " + userB.token()
                        )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title")
                        .value("User B Private Task"));
    }

    // ==========================================
    // TEST 9: PREVENT CROSS-USER TASK ACCESS
    // ==========================================

    @Test
    void userB_shouldNotAccessUserATask() throws Exception {

        TestUser userA = createUserA();
        TestUser userB = createUserB();

        Long taskId = createTask(
                userA,
                "User A Private Task",
                "PENDING",
                "HIGH"
        );

        mockMvc.perform(
                get("/api/tasks/{id}", taskId)
                        .header(
                                "Authorization",
                                "Bearer " + userB.token()
                        )
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Task not found"));
    }

    // ==========================================
    // TEST 10: PREVENT UNAUTHORIZED UPDATE
    // ==========================================

    @Test
    void userB_shouldNotUpdateUserATask() throws Exception {

        TestUser userA = createUserA();
        TestUser userB = createUserB();

        Long taskId = createTask(
                userA,
                "User A Original Task",
                "PENDING",
                "LOW"
        );

        String updatedRequest = """
                {
                    "title": "Unauthorized Update",
                    "description": "Should not be updated",
                    "status": "COMPLETED",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(
                put("/api/tasks/{id}", taskId)
                        .header(
                                "Authorization",
                                "Bearer " + userB.token()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedRequest)
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        Task originalTask = taskRepository
                .findByIdAndUserId(taskId, userA.id())
                .orElseThrow();

        assertEquals("User A Original Task", originalTask.getTitle());
        assertEquals("PENDING", originalTask.getStatus().name());
    }

    // ==========================================
    // TEST 11: PREVENT UNAUTHORIZED DELETE
    // ==========================================

    @Test
    void userB_shouldNotDeleteUserATask() throws Exception {

        TestUser userA = createUserA();
        TestUser userB = createUserB();

        Long taskId = createTask(
                userA,
                "User A Protected Task",
                "PENDING",
                "HIGH"
        );

        mockMvc.perform(
                delete("/api/tasks/{id}", taskId)
                        .header(
                                "Authorization",
                                "Bearer " + userB.token()
                        )
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        assertTrue(taskRepository.existsById(taskId));
    }

    // ==========================================
    // TEST 12: UNAUTHENTICATED REQUEST
    // ==========================================

    @Test
    void getTasks_withoutToken_shouldReturnUnauthorized()
            throws Exception {

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }
}

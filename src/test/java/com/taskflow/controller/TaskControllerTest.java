package com.taskflow.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.taskflow.dto.TaskRequestDTO;
import com.taskflow.dto.TaskResponseDTO;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.service.TaskService;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;
    @Test
void createTask_shouldReturnCreatedTask() throws Exception {

    TaskResponseDTO response = new TaskResponseDTO();
    response.setId(1L);
    response.setTitle("Learn Spring Boot");
    response.setDescription("Complete controller testing");
    response.setStatus(TaskStatus.PENDING);
    response.setPriority(TaskPriority.HIGH);

    when(taskService.createTask(org.mockito.ArgumentMatchers.any(TaskRequestDTO.class)))
            .thenReturn(response);

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
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Learn Spring Boot"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.priority").value("HIGH"));
}
@Test
void createTask_shouldReturnBadRequestWhenTitleIsEmpty() throws Exception {

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
            .andExpect(status().isBadRequest());
            verify(taskService, never()).createTask(any(TaskRequestDTO.class));
}
@Test
void createTask_shouldReturnBadRequestWhenPriorityIsMissing() throws Exception {

    mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                        "title": "Learn Spring Boot",
                        "description": "Practice testing",
                        "status": "PENDING"
                    }
                    """))
            .andExpect(status().isBadRequest());

    verify(taskService, never()).createTask(any(TaskRequestDTO.class));
}
@Test
void createTask_shouldReturnBadRequestWhenPriorityIsInvalid() throws Exception {

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
            .andExpect(status().isBadRequest());

    verify(taskService, never()).createTask(any(TaskRequestDTO.class));
}
@Test
void createTask_shouldReturnBadRequestWhenTitleExceedsLimit() throws Exception {

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
            .andExpect(status().isBadRequest());

    verify(taskService, never()).createTask(any(TaskRequestDTO.class));
}
@Test
void createTask_shouldReturnBadRequestWhenDescriptionExceedsLimit() throws Exception {

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
            .andExpect(status().isBadRequest());

    verify(taskService, never()).createTask(any(TaskRequestDTO.class));
}
@Test
void getAllTasks_shouldReturnAllTasks() throws Exception {

    TaskResponseDTO task1 = new TaskResponseDTO();
    task1.setId(1L);
    task1.setTitle("Learn Java");
    task1.setDescription("Core Java concepts");
    task1.setStatus(TaskStatus.PENDING);
    task1.setPriority(TaskPriority.HIGH);

    TaskResponseDTO task2 = new TaskResponseDTO();
    task2.setId(2L);
    task2.setTitle("Learn Spring Boot");
    task2.setDescription("Backend development");
    task2.setStatus(TaskStatus.IN_PROGRESS);
    task2.setPriority(TaskPriority.MEDIUM);

    when(taskService.getAllTasks()).thenReturn(List.of(task1, task2));

    mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].title").value("Learn Java"))
            .andExpect(jsonPath("$[1].title").value("Learn Spring Boot"));
}
@Test
void getTaskById_shouldReturnTaskWhenIdExists() throws Exception {

    TaskResponseDTO task = new TaskResponseDTO();
    task.setId(1L);
    task.setTitle("Learn Java");
    task.setDescription("Core Java concepts");
    task.setStatus(TaskStatus.PENDING);
    task.setPriority(TaskPriority.HIGH);

    when(taskService.getTaskById(1L)).thenReturn(task);

    mockMvc.perform(get("/api/tasks/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Learn Java"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.priority").value("HIGH"));
}
@Test
void getTaskById_shouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {

    when(taskService.getTaskById(9999L))
            .thenThrow(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Task not found"
            ));

    mockMvc.perform(get("/api/tasks/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Task not found"))
            .andExpect(jsonPath("$.status").value(404));
}
@Test
void updateTask_shouldReturnUpdatedTask() throws Exception {

    TaskResponseDTO response = new TaskResponseDTO();
    response.setId(1L);
    response.setTitle("Learn Advanced Java");
    response.setDescription("Updated Java concepts");
    response.setStatus(TaskStatus.IN_PROGRESS);
    response.setPriority(TaskPriority.HIGH);

    when(taskService.updateTask(
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.any(TaskRequestDTO.class)
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
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Learn Advanced Java"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.priority").value("HIGH"));
} 
@Test
void updateTask_shouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {

    when(taskService.updateTask(
            org.mockito.ArgumentMatchers.eq(9999L),
            org.mockito.ArgumentMatchers.any(TaskRequestDTO.class)
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
            .andExpect(jsonPath("$.message").value("Task not found"))
            .andExpect(jsonPath("$.status").value(404));
}
@Test
void deleteTask_shouldReturnOkWhenTaskIsDeleted() throws Exception {

    mockMvc.perform(delete("/api/tasks/1"))
            .andExpect(status().isNoContent());

    verify(taskService).deleteTask(1L);
}
@Test
void deleteTask_shouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {

    org.mockito.Mockito.doThrow(
            new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Task not found"
            )
    ).when(taskService).deleteTask(9999L);

    mockMvc.perform(delete("/api/tasks/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Task not found"))
            .andExpect(jsonPath("$.status").value(404));

    verify(taskService).deleteTask(9999L);
}
}
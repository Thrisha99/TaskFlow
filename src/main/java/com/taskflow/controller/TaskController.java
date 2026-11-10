
package com.taskflow.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.taskflow.dto.ApiResponse;
import com.taskflow.dto.PageResponse;
import com.taskflow.dto.TaskRequestDTO;
import com.taskflow.dto.TaskResponseDTO;
import com.taskflow.entity.TaskPriority;
import com.taskflow.entity.TaskStatus;
import com.taskflow.security.CustomUserDetails;
import com.taskflow.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/tasks")
@Tag(
    name = "Task Management",
    description = "APIs for creating, retrieving, updating, deleting, filtering and searching tasks."
)
public class TaskController {

    private final TaskService taskService;
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // CREATE TASK
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new task",
        description = "Creates a task using the provided title, description, status and priority."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Task created successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid task details or validation failure"
        )
    })
   // CREATE TASK

public ApiResponse<TaskResponseDTO> createTask(
        @Valid @RequestBody TaskRequestDTO taskRequestDTO,
        Authentication authentication) {

    CustomUserDetails userDetails =
            (CustomUserDetails) authentication.getPrincipal();

    Long userId = userDetails.getId();

    TaskResponseDTO createdTask =
            taskService.createTask(taskRequestDTO, userId);

    return ApiResponse.success(
            "Task created successfully",
            createdTask
    );
}

    // GET ALL TASKS WITH PAGINATION, SORTING AND FILTERING
    @GetMapping
    @Operation(
        summary = "Get tasks with pagination and filters",
        description = "Retrieves tasks using pagination, sorting, status filtering, priority filtering and case-insensitive title keyword search. All filters are optional."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tasks retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid pagination, sorting or filter parameter"
        )
    })
    public ApiResponse<PageResponse<TaskResponseDTO>> getAllTasks(

            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of tasks per page (1 to 100)", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(
                description = "Field used to sort tasks. Allowed: id, title, description, status, priority",
                example = "title"
            )
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Sorting direction: asc or desc", example = "asc")
            @RequestParam(defaultValue = "asc") String direction,

            @Parameter(description = "Filter tasks by status", example = "PENDING")
            @RequestParam(required = false) TaskStatus status,

            @Parameter(description = "Filter tasks by priority", example = "HIGH")
            @RequestParam(required = false) TaskPriority priority,

            @Parameter(description = "Search task titles using a keyword", example = "Java")
           @RequestParam(required = false) String keyword,
Authentication authentication) {
CustomUserDetails userDetails =
        (CustomUserDetails) authentication.getPrincipal();

Long userId = userDetails.getId();
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        if (size <= 0 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 100"
            );
        }

        List<String> allowedSortFields = List.of(
                "id",
                "title",
                "description",
                "status",
                "priority"
        );

        if (!allowedSortFields.contains(sortBy)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sorting field: " + sortBy
            );
        }

        if (!direction.equalsIgnoreCase("asc")
                && !direction.equalsIgnoreCase("desc")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sorting direction. Use 'asc' or 'desc'"
            );
        }

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TaskResponseDTO> tasks = taskService.getFilteredTasks(
              userId,  status,
                priority,
                keyword,
                pageable
        );

        PageResponse<TaskResponseDTO> pageResponse =
                new PageResponse<>(tasks);

        return ApiResponse.success(
                "Tasks retrieved successfully",
                pageResponse
        );
    }

    // FILTER TASKS BY STATUS
    @GetMapping("/filter/status")
    @Operation(
        summary = "Filter tasks by status",
        description = "Retrieves all tasks matching the specified status."
    )
    public ApiResponse<List<TaskResponseDTO>> getTasksByStatus(
            @Parameter(description = "Task status", example = "PENDING")
            @RequestParam TaskStatus status) {

        List<TaskResponseDTO> tasks =
                taskService.getTasksByStatus(status);

        return ApiResponse.success(
                "Tasks filtered by status successfully",
                tasks
        );
    }

    // FILTER TASKS BY PRIORITY
    @GetMapping("/filter/priority")
    @Operation(
        summary = "Filter tasks by priority",
        description = "Retrieves all tasks matching the specified priority."
    )
    public ApiResponse<List<TaskResponseDTO>> getTasksByPriority(
            @Parameter(description = "Task priority", example = "HIGH")
            @RequestParam TaskPriority priority) {

        List<TaskResponseDTO> tasks =
                taskService.getTasksByPriority(priority);

        return ApiResponse.success(
                "Tasks filtered by priority successfully",
                tasks
        );
    }

    // FILTER TASKS BY STATUS AND PRIORITY
    @GetMapping("/filter")
    @Operation(
        summary = "Filter tasks by status and priority",
        description = "Retrieves tasks that match both the specified status and priority."
    )
    public ApiResponse<List<TaskResponseDTO>> getTasksByStatusAndPriority(
            @Parameter(description = "Task status", example = "PENDING")
            @RequestParam TaskStatus status,

            @Parameter(description = "Task priority", example = "HIGH")
            @RequestParam TaskPriority priority) {

        List<TaskResponseDTO> tasks =
                taskService.getTasksByStatusAndPriority(status, priority);

        return ApiResponse.success(
                "Tasks filtered successfully",
                tasks
        );
    }

    // GET TASK BY ID
    @GetMapping("/{id}")
    @Operation(
        summary = "Get task by ID",
        description = "Retrieves a specific task using its unique ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Task retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task not found"
        )
    })
    public ApiResponse<TaskResponseDTO> getTaskById(
            @Parameter(description = "Unique ID of the task", example = "1")
            @PathVariable Long id) {

        TaskResponseDTO task = taskService.getTaskById(id);

        return ApiResponse.success(
                "Task retrieved successfully",
                task
        );
    }

    // SEARCH TASKS BY TITLE
    @GetMapping("/search")
    @Operation(
        summary = "Search tasks by title",
        description = "Searches task titles using the provided keyword."
    )
    public ApiResponse<List<TaskResponseDTO>> searchTasks(
            @Parameter(description = "Keyword to search in task titles", example = "Java")
            @RequestParam String keyword) {

        List<TaskResponseDTO> tasks =
                taskService.searchTasksByTitle(keyword);

        return ApiResponse.success(
                "Task search completed successfully",
                tasks
        );
    }

    // UPDATE TASK
    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing task",
        description = "Updates the details of an existing task using its ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Task updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid task details"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task not found"
        )
    })
    public ApiResponse<TaskResponseDTO> updateTask(
            @Parameter(description = "Unique ID of the task to update", example = "1")
            @PathVariable Long id,

            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated task details",
                required = true
            )
            @RequestBody TaskRequestDTO updatedTask) {

        TaskResponseDTO task =
                taskService.updateTask(id, updatedTask);

        return ApiResponse.success(
                "Task updated successfully",
                task
        );
    }

    // DELETE TASK
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a task",
        description = "Deletes a task permanently using its unique ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Task deleted successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Task not found"
        )
    })
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @Parameter(description = "Unique ID of the task to delete", example = "1")
            @PathVariable Long id) {

        taskService.deleteTask(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Task deleted successfully",
                        null
                )
        );
    }
}

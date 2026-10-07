package com.edstem.interviewprep.task.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.common.exception.ErrorResponse;
import com.edstem.interviewprep.task.dto.request.CreateTaskRequest;
import com.edstem.interviewprep.task.dto.request.UpdateTaskRequest;
import com.edstem.interviewprep.task.dto.response.TaskResponse;
import com.edstem.interviewprep.task.entity.TaskStatus;
import com.edstem.interviewprep.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Create, view, update, delete and filter tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@PostMapping
	@Operation(summary = "Create a task", description = "Status defaults to TODO when omitted.")
	@ApiResponse(responseCode = "201", description = "Task created")
	@ApiResponse(responseCode = "400", description = "Invalid input, with a message per invalid field",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
		TaskResponse created = taskService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();

		return ResponseEntity.created(location).body(created);
	}

	@GetMapping
	@Operation(summary = "List tasks", description = "Oldest first. Optionally filter by status.")
	@ApiResponse(responseCode = "200", description = "Tasks matching the filter")
	@ApiResponse(responseCode = "400", description = "Unknown status value",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public List<TaskResponse> list(@RequestParam(required = false) TaskStatus status) {
		return taskService.list(status);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one task")
	@ApiResponse(responseCode = "200", description = "The task")
	@ApiResponse(responseCode = "400", description = "Malformed task id",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Task not found",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public TaskResponse get(@PathVariable UUID id) {
		return taskService.get(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Replace a task", description = "Full replace: title and status are required.")
	@ApiResponse(responseCode = "200", description = "Task updated")
	@ApiResponse(responseCode = "400", description = "Invalid input, with a message per invalid field",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Task not found",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
		return taskService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a task")
	@ApiResponse(responseCode = "204", description = "Task deleted")
	@ApiResponse(responseCode = "404", description = "Task not found",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		taskService.delete(id);

		return ResponseEntity.noContent().build();
	}
}

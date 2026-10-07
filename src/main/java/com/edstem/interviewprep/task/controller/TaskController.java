package com.edstem.interviewprep.task.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.task.dto.request.CreateTaskRequest;
import com.edstem.interviewprep.task.dto.request.UpdateTaskRequest;
import com.edstem.interviewprep.task.dto.response.TaskResponse;
import com.edstem.interviewprep.task.entity.TaskStatus;
import com.edstem.interviewprep.task.service.TaskService;
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
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@PostMapping
	public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
		TaskResponse created = taskService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();

		return ResponseEntity.created(location).body(created);
	}

	@GetMapping
	public List<TaskResponse> list(@RequestParam(required = false) TaskStatus status) {
		return taskService.list(status);
	}

	@GetMapping("/{id}")
	public TaskResponse get(@PathVariable UUID id) {
		return taskService.get(id);
	}

	@PutMapping("/{id}")
	public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
		return taskService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		taskService.delete(id);

		return ResponseEntity.noContent().build();
	}
}

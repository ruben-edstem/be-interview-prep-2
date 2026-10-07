package com.edstem.interviewprep.task.service;

import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.task.dto.request.CreateTaskRequest;
import com.edstem.interviewprep.task.dto.request.UpdateTaskRequest;
import com.edstem.interviewprep.task.dto.response.TaskResponse;
import com.edstem.interviewprep.task.entity.Task;
import com.edstem.interviewprep.task.entity.TaskStatus;
import com.edstem.interviewprep.task.exception.TaskNotFoundException;
import com.edstem.interviewprep.task.repository.TaskRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

	private static final Sort OLDEST_FIRST = Sort.by("createdAt");

	private final TaskRepository taskRepository;

	public TaskService(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	@Transactional
	public TaskResponse create(CreateTaskRequest request) {
		TaskStatus status = request.status() != null ? request.status() : TaskStatus.TODO;
		Task task = new Task(request.title(), request.description(), status, request.dueDate());

		return TaskResponse.from(taskRepository.save(task));
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> list(TaskStatus status) {
		List<Task> tasks = status == null
				? taskRepository.findAll(OLDEST_FIRST)
				: taskRepository.findByStatus(status, OLDEST_FIRST);

		return tasks.stream().map(TaskResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public TaskResponse get(UUID id) {
		return TaskResponse.from(findTask(id));
	}

	@Transactional
	public TaskResponse update(UUID id, UpdateTaskRequest request) {
		Task task = findTask(id);
		task.update(request.title(), request.description(), request.status(), request.dueDate());

		return TaskResponse.from(task);
	}

	@Transactional
	public void delete(UUID id) {
		taskRepository.delete(findTask(id));
	}

	private Task findTask(UUID id) {
		return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}
}

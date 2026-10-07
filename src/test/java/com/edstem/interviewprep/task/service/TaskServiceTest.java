package com.edstem.interviewprep.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.edstem.interviewprep.task.dto.request.CreateTaskRequest;
import com.edstem.interviewprep.task.dto.request.UpdateTaskRequest;
import com.edstem.interviewprep.task.dto.response.TaskResponse;
import com.edstem.interviewprep.task.entity.Task;
import com.edstem.interviewprep.task.entity.TaskStatus;
import com.edstem.interviewprep.task.exception.TaskNotFoundException;
import com.edstem.interviewprep.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	private TaskRepository taskRepository;

	@InjectMocks
	private TaskService taskService;

	@Test
	void createDefaultsStatusToTodoWhenAbsent() {
		CreateTaskRequest request = new CreateTaskRequest("Write report", null, null, null);
		when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TaskResponse response = taskService.create(request);

		assertThat(response.status()).isEqualTo(TaskStatus.TODO);
		assertThat(response.title()).isEqualTo("Write report");
	}

	@Test
	void createKeepsStatusWhenProvided() {
		CreateTaskRequest request = new CreateTaskRequest("Ship it", null, TaskStatus.IN_PROGRESS, null);
		when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TaskResponse response = taskService.create(request);

		assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
	}

	@Test
	void listWithoutStatusReturnsAllTasks() {
		Task task = new Task("One", null, TaskStatus.TODO, null);
		when(taskRepository.findAll(any(Sort.class))).thenReturn(List.of(task));

		List<TaskResponse> responses = taskService.list(null);

		assertThat(responses).extracting(TaskResponse::title).containsExactly("One");
		verify(taskRepository, never()).findByStatus(any(), any());
	}

	@Test
	void listWithStatusFiltersByStatus() {
		Task task = new Task("Done one", null, TaskStatus.DONE, null);
		when(taskRepository.findByStatus(any(TaskStatus.class), any(Sort.class))).thenReturn(List.of(task));

		List<TaskResponse> responses = taskService.list(TaskStatus.DONE);

		assertThat(responses).extracting(TaskResponse::status).containsExactly(TaskStatus.DONE);
	}

	@Test
	void getReturnsTaskWhenFound() {
		UUID id = UUID.randomUUID();
		Task task = new Task("Find me", null, TaskStatus.TODO, null);
		when(taskRepository.findById(id)).thenReturn(Optional.of(task));

		TaskResponse response = taskService.get(id);

		assertThat(response.title()).isEqualTo("Find me");
	}

	@Test
	void getThrowsWhenTaskDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(taskRepository.findById(id)).thenReturn(Optional.empty());

		TaskNotFoundException thrown = assertThrows(TaskNotFoundException.class, () -> taskService.get(id));

		assertThat(thrown.getMessage()).contains(id.toString());
	}

	@Test
	void updateReplacesAllFields() {
		UUID id = UUID.randomUUID();
		Task task = new Task("Old", "old description", TaskStatus.TODO, null);
		LocalDate dueDate = LocalDate.now().plusDays(7);
		UpdateTaskRequest request = new UpdateTaskRequest("New", "new description", TaskStatus.DONE, dueDate);
		when(taskRepository.findById(id)).thenReturn(Optional.of(task));

		TaskResponse response = taskService.update(id, request);

		assertThat(response.title()).isEqualTo("New");
		assertThat(response.description()).isEqualTo("new description");
		assertThat(response.status()).isEqualTo(TaskStatus.DONE);
		assertThat(response.dueDate()).isEqualTo(dueDate);
	}

	@Test
	void updateThrowsWhenTaskDoesNotExist() {
		UUID id = UUID.randomUUID();
		UpdateTaskRequest request = new UpdateTaskRequest("New", null, TaskStatus.DONE, null);
		when(taskRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(TaskNotFoundException.class, () -> taskService.update(id, request));
	}

	@Test
	void deleteRemovesExistingTask() {
		UUID id = UUID.randomUUID();
		Task task = new Task("Remove me", null, TaskStatus.TODO, null);
		when(taskRepository.findById(id)).thenReturn(Optional.of(task));

		taskService.delete(id);

		verify(taskRepository).delete(task);
	}

	@Test
	void deleteThrowsWhenTaskDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(taskRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(TaskNotFoundException.class, () -> taskService.delete(id));

		verify(taskRepository, never()).delete(any());
	}
}

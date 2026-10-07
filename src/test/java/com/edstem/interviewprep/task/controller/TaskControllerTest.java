package com.edstem.interviewprep.task.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.task.dto.request.CreateTaskRequest;
import com.edstem.interviewprep.task.dto.request.UpdateTaskRequest;
import com.edstem.interviewprep.task.dto.response.TaskResponse;
import com.edstem.interviewprep.task.entity.TaskStatus;
import com.edstem.interviewprep.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TaskService taskService;

	@Test
	void createReturns201WithLocationAndBody() throws Exception {
		UUID id = UUID.randomUUID();
		when(taskService.create(any(CreateTaskRequest.class)))
				.thenReturn(response(id, "Write report", TaskStatus.TODO));

		mockMvc.perform(post("/api/v1/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Write report\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/tasks/" + id))
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.title").value("Write report"))
				.andExpect(jsonPath("$.status").value("TODO"));
	}

	@Test
	void listPassesStatusFilterToService() throws Exception {
		when(taskService.list(TaskStatus.DONE)).thenReturn(List.of(response(UUID.randomUUID(), "Done", TaskStatus.DONE)));

		mockMvc.perform(get("/api/v1/tasks").param("status", "DONE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].status").value("DONE"));
	}

	@Test
	void listWithoutStatusReturnsAllTasks() throws Exception {
		when(taskService.list(null)).thenReturn(List.of(
				response(UUID.randomUUID(), "One", TaskStatus.TODO),
				response(UUID.randomUUID(), "Two", TaskStatus.DONE)));

		mockMvc.perform(get("/api/v1/tasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void getReturnsTask() throws Exception {
		UUID id = UUID.randomUUID();
		when(taskService.get(id)).thenReturn(response(id, "Find me", TaskStatus.IN_PROGRESS));

		mockMvc.perform(get("/api/v1/tasks/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Find me"));
	}

	@Test
	void updateReturnsUpdatedTask() throws Exception {
		UUID id = UUID.randomUUID();
		when(taskService.update(any(UUID.class), any(UpdateTaskRequest.class)))
				.thenReturn(response(id, "Renamed", TaskStatus.DONE));

		mockMvc.perform(put("/api/v1/tasks/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Renamed\",\"status\":\"DONE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Renamed"))
				.andExpect(jsonPath("$.status").value("DONE"));
	}

	@Test
	void deleteReturns204() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(delete("/api/v1/tasks/{id}", id))
				.andExpect(status().isNoContent());

		verify(taskService).delete(id);
	}

	private TaskResponse response(UUID id, String title, TaskStatus status) {
		return new TaskResponse(id, title, null, status, LocalDate.now().plusDays(1), Instant.now());
	}
}

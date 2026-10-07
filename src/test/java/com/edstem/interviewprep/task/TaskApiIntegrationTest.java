package com.edstem.interviewprep.task;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.edstem.interviewprep.task.repository.TaskRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest {

	private static final String TASKS = "/api/v1/tasks";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TaskRepository taskRepository;

	@BeforeEach
	void cleanDatabase() {
		taskRepository.deleteAll();
	}

	@Test
	void taskLifecycleFromCreateToDelete() throws Exception {
		String dueDate = LocalDate.now().plusDays(5).toString();
		String created = mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Write report\",\"description\":\"Q3\",\"dueDate\":\"" + dueDate + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("TODO"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(created, "$.id");

		mockMvc.perform(get(TASKS + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Write report"))
				.andExpect(jsonPath("$.dueDate").value(dueDate));

		mockMvc.perform(put(TASKS + "/" + id).contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Write final report\",\"status\":\"DONE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Write final report"))
				.andExpect(jsonPath("$.status").value("DONE"))
				.andExpect(jsonPath("$.description").doesNotExist());

		mockMvc.perform(delete(TASKS + "/" + id)).andExpect(status().isNoContent());

		mockMvc.perform(get(TASKS + "/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("TASK_NOT_FOUND"));
	}

	@Test
	void listFiltersByStatus() throws Exception {
		createTask("Todo task", "TODO");
		createTask("Working task", "IN_PROGRESS");
		createTask("Done task", "DONE");

		mockMvc.perform(get(TASKS)).andExpect(jsonPath("$", hasSize(3)));
		mockMvc.perform(get(TASKS).param("status", "IN_PROGRESS"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("Working task"));
	}

	@Test
	void updatingUnknownTaskReturns404() throws Exception {
		mockMvc.perform(put(TASKS + "/00000000-0000-0000-0000-000000000000")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Ghost\",\"status\":\"DONE\"}"))
				.andExpect(status().isNotFound());
	}

	private void createTask(String title, String status) throws Exception {
		mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"" + title + "\",\"status\":\"" + status + "\"}"))
				.andExpect(status().isCreated());
	}
}

package com.edstem.interviewprep.common.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.edstem.interviewprep.task.controller.TaskController;
import com.edstem.interviewprep.task.dto.request.CreateTaskRequest;
import com.edstem.interviewprep.task.dto.response.TaskResponse;
import com.edstem.interviewprep.task.entity.TaskStatus;
import com.edstem.interviewprep.task.exception.TaskNotFoundException;
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
class GlobalExceptionHandlerTest {

	private static final String TASKS = "/api/v1/tasks";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TaskService taskService;

	@Test
	void blankTitleReturns400WithFieldMessage() throws Exception {
		mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.path").value(TASKS))
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='title')].message").value(hasItem("Title is required")));
	}

	@Test
	void titleLongerThan100CharactersReturns400() throws Exception {
		String body = "{\"title\":\"" + "a".repeat(101) + "\"}";

		mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='title')].message")
						.value(hasItem("Title must be at most 100 characters")));
	}

	@Test
	void titleOfExactly100CharactersIsAccepted() throws Exception {
		String title = "a".repeat(100);
		when(taskService.create(any(CreateTaskRequest.class)))
				.thenReturn(new TaskResponse(UUID.randomUUID(), title, null, TaskStatus.TODO, null, Instant.now()));

		mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"" + title + "\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void pastDueDateReturns400() throws Exception {
		String body = "{\"title\":\"Late\",\"dueDate\":\"" + LocalDate.now().minusDays(1) + "\"}";

		mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='dueDate')].message")
						.value(hasItem("Due date cannot be in the past")));
	}

	@Test
	void everyInvalidFieldIsReportedTogether() throws Exception {
		String body = "{\"title\":\"\",\"dueDate\":\"" + LocalDate.now().minusDays(1) + "\"}";

		mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors", hasSize(2)));
	}

	@Test
	void unknownStatusInBodyReturns400WithAllowedValues() throws Exception {
		mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Bad status\",\"status\":\"STARTED\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value(containsString("TODO")));
	}

	@Test
	void unparseableDueDateReturns400() throws Exception {
		mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Bad date\",\"dueDate\":\"tomorrow\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("dueDate"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("Must be a valid date in yyyy-MM-dd format"));
	}

	@Test
	void updateWithoutStatusReturns400() throws Exception {
		mockMvc.perform(put(TASKS + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"No status\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='status')].message").value(hasItem("Status is required")));
	}

	@Test
	void updateWithBlankTitleReturns400() throws Exception {
		mockMvc.perform(put(TASKS + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\" \",\"status\":\"DONE\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='title')].message").value(hasItem("Title is required")));
	}

	@Test
	void updateWithTitleLongerThan100CharactersReturns400() throws Exception {
		String body = "{\"title\":\"" + "a".repeat(101) + "\",\"status\":\"DONE\"}";

		mockMvc.perform(put(TASKS + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='title')].message")
						.value(hasItem("Title must be at most 100 characters")));
	}

	@Test
	void updateWithPastDueDateReturns400() throws Exception {
		String body = "{\"title\":\"Late\",\"status\":\"DONE\",\"dueDate\":\"" + LocalDate.now().minusDays(1) + "\"}";

		mockMvc.perform(put(TASKS + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='dueDate')].message")
						.value(hasItem("Due date cannot be in the past")));
	}

	@Test
	void updateWithDescriptionLongerThan1000CharactersReturns400() throws Exception {
		String body = "{\"title\":\"Wordy\",\"status\":\"DONE\",\"description\":\"" + "d".repeat(1001) + "\"}";

		mockMvc.perform(put(TASKS + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='description')].message")
						.value(hasItem("Description must be at most 1000 characters")));
	}

	@Test
	void createWithDescriptionLongerThan1000CharactersReturns400() throws Exception {
		String body = "{\"title\":\"Wordy\",\"description\":\"" + "d".repeat(1001) + "\"}";

		mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='description')].message")
						.value(hasItem("Description must be at most 1000 characters")));
	}

	@Test
	void malformedJsonReturns400() throws Exception {
		mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
	}

	@Test
	void unknownTaskReturns404() throws Exception {
		UUID id = UUID.randomUUID();
		when(taskService.get(id)).thenThrow(new TaskNotFoundException(id));

		mockMvc.perform(get(TASKS + "/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("TASK_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Task not found: " + id))
				.andExpect(jsonPath("$.path").value(TASKS + "/" + id));
	}

	@Test
	void malformedTaskIdReturns400() throws Exception {
		mockMvc.perform(get(TASKS + "/not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_PARAMETER"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("id"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("Must be a valid UUID"));
	}

	@Test
	void unknownStatusFilterReturns400() throws Exception {
		mockMvc.perform(get(TASKS).param("status", "STARTED"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value(containsString("IN_PROGRESS")));
	}

	@Test
	void unexpectedFailureReturns500WithoutLeakingDetails() throws Exception {
		UUID id = UUID.randomUUID();
		when(taskService.get(id)).thenThrow(new IllegalStateException("jdbc:h2:mem password=secret"));

		mockMvc.perform(get(TASKS + "/" + id))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("An unexpected error occurred"))
				.andExpect(content().string(not(containsString("secret"))));
	}

	@Test
	void unsupportedMethodReturns405InSameFormat() throws Exception {
		mockMvc.perform(patch(TASKS))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
				.andExpect(header().string("Allow", containsString("POST")));
	}

	@Test
	void unknownRouteReturns404InSameFormat() throws Exception {
		mockMvc.perform(get("/api/v1/nothing-here"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}
}

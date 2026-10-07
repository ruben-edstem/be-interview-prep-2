package com.edstem.interviewprep.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiDocsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void openApiSpecListsTheTaskEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/tasks']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/tasks/{id}']").exists());
	}

	@Test
	void openApiSpecCarriesTitleAndDocumentsErrorResponses() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Backend Interview Prep API"))
				.andExpect(jsonPath("$.paths['/api/v1/tasks'].post.summary").value("Create a task"))
				.andExpect(jsonPath("$.paths['/api/v1/tasks'].post.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/tasks/{id}'].get.responses['404']").exists())
				.andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.fieldErrors").exists());
	}

	@Test
	void swaggerUiIsServed() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", containsString("/swagger-ui/index.html")));

		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}
}

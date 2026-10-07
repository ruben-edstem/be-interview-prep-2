package com.edstem.interviewprep.urlshortener.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.edstem.interviewprep.urlshortener.entity.ShortUrl;
import com.edstem.interviewprep.urlshortener.exception.CodeGenerationException;
import com.edstem.interviewprep.urlshortener.service.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShortUrlController.class)
class ShortUrlControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShortUrlService service;

	@Test
	void createReturnsCodeAndShortUrl() throws Exception {
		when(service.create(eq("https://example.com/a/very/long/path"), any()))
				.thenReturn(new ShortUrl("abc1234", "https://example.com/a/very/long/path", null));

		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com/a/very/long/path\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("abc1234"))
				.andExpect(jsonPath("$.shortUrl").value("http://localhost/abc1234"))
				.andExpect(jsonPath("$.originalUrl").value("https://example.com/a/very/long/path"));
	}

	@Test
	void createPassesTheExpiryToTheService() throws Exception {
		Instant expiresAt = Instant.parse("2999-01-01T00:00:00Z");
		when(service.create("https://example.com", expiresAt))
				.thenReturn(new ShortUrl("abc1234", "https://example.com", expiresAt));

		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\",\"expiresAt\":\"2999-01-01T00:00:00Z\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.expiresAt").value("2999-01-01T00:00:00Z"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"not a url",
			"example.com",
			"ftp://example.com/file",
			"javascript:alert(1)",
			"http://",
			"https:///path-without-host"
	})
	void createRejectsInvalidUrls(String url) throws Exception {
		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"" + url + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors.url").exists());

		verify(service, never()).create(any(), any());
	}

	@Test
	void createRejectsMissingUrl() throws Exception {
		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.url").exists());
	}

	@Test
	void createRejectsAnExpiryInThePast() throws Exception {
		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\",\"expiresAt\":\"2000-01-01T00:00:00Z\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.expiresAt").exists());
	}

	@Test
	void createRejectsMalformedJson() throws Exception {
		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createReportsCodeGenerationFailureWithoutLeakingDetails() throws Exception {
		when(service.create(any(), any())).thenThrow(new CodeGenerationException());

		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("CODE_GENERATION_FAILED"));
	}
}

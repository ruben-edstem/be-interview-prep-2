package com.edstem.interviewprep.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.edstem.interviewprep.urlshortener.entity.ShortUrl;
import com.edstem.interviewprep.urlshortener.repository.ShortUrlRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ShortUrlRepository repository;

	@Test
	void shortenRedirectAndReadStats() throws Exception {
		String code = shorten("https://example.com/some/long/path?x=1", null);

		mockMvc.perform(get("/" + code))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/some/long/path?x=1"));

		mockMvc.perform(get("/api/v1/urls/" + code + "/stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.originalUrl").value("https://example.com/some/long/path?x=1"))
				.andExpect(jsonPath("$.visitCount").value(1))
				.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	void shorteningTheSameUrlTwiceGivesTwoIndependentLinks() throws Exception {
		String first = shorten("https://example.com/same", null);
		String second = shorten("https://example.com/same", null);

		mockMvc.perform(get("/" + first)).andExpect(status().isFound());

		assertThat(first).isNotEqualTo(second);
		mockMvc.perform(get("/api/v1/urls/" + first + "/stats")).andExpect(jsonPath("$.visitCount").value(1));
		mockMvc.perform(get("/api/v1/urls/" + second + "/stats")).andExpect(jsonPath("$.visitCount").value(0));
	}

	@Test
	void concurrentVisitsAreAllCounted() throws Exception {
		String code = shorten("https://example.com/popular", null);
		int visits = 200;
		ExecutorService executor = Executors.newFixedThreadPool(20);
		List<Callable<Integer>> requests = new ArrayList<>();
		for (int i = 0; i < visits; i++) {
			requests.add(() -> mockMvc.perform(get("/" + code)).andReturn().getResponse().getStatus());
		}

		List<Future<Integer>> results = executor.invokeAll(requests);
		executor.shutdown();

		for (Future<Integer> result : results) {
			assertThat(result.get()).isEqualTo(302);
		}
		mockMvc.perform(get("/api/v1/urls/" + code + "/stats"))
				.andExpect(jsonPath("$.visitCount").value(visits));
	}

	@Test
	void expiredLinkIsGoneButKeepsItsStats() throws Exception {
		repository.saveAndFlush(new ShortUrl("expired1", "https://example.com/old",
				Instant.now().minus(1, ChronoUnit.HOURS)));

		mockMvc.perform(get("/expired1"))
				.andExpect(status().isGone());

		mockMvc.perform(get("/api/v1/urls/expired1/stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visitCount").value(0));
	}

	@Test
	void unknownCodeIsNotFound() throws Exception {
		mockMvc.perform(get("/zzzzzzz")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/urls/zzzzzzz/stats")).andExpect(status().isNotFound());
	}

	@Test
	void linkWithAFutureExpiryStillRedirects() throws Exception {
		String code = shorten("https://example.com/later", Instant.now().plus(1, ChronoUnit.DAYS));

		mockMvc.perform(get("/" + code)).andExpect(status().isFound());
	}

	private String shorten(String url, Instant expiresAt) throws Exception {
		String body = expiresAt == null
				? "{\"url\":\"" + url + "\"}"
				: "{\"url\":\"" + url + "\",\"expiresAt\":\"" + expiresAt + "\"}";
		String response = mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(response);
		assertThat(json.get("code").asText()).hasSizeLessThanOrEqualTo(8);
		assertThat(json.get("shortUrl").asText()).endsWith("/" + json.get("code").asText());
		return json.get("code").asText();
	}
}

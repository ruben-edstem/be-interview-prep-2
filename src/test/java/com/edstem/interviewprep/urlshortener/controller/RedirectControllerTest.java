package com.edstem.interviewprep.urlshortener.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.urlshortener.exception.ShortUrlExpiredException;
import com.edstem.interviewprep.urlshortener.exception.ShortUrlNotFoundException;
import com.edstem.interviewprep.urlshortener.service.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RedirectController.class)
class RedirectControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShortUrlService service;

	@Test
	void redirectsToTheOriginalUrlWithoutAllowingCaching() throws Exception {
		when(service.visit("abc1234")).thenReturn("https://example.com/a?b=1");

		mockMvc.perform(get("/abc1234"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/a?b=1"))
				.andExpect(header().string("Cache-Control", "no-store"));
	}

	@Test
	void unknownCodeReturnsNotFound() throws Exception {
		when(service.visit("nothere")).thenThrow(new ShortUrlNotFoundException("nothere"));

		mockMvc.perform(get("/nothere"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));
	}

	@Test
	void expiredCodeReturnsGone() throws Exception {
		when(service.visit("old0001")).thenThrow(new ShortUrlExpiredException("old0001"));

		mockMvc.perform(get("/old0001"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("SHORT_URL_EXPIRED"));
	}

	@Test
	void codeLongerThanEightCharactersIsNotRouted() throws Exception {
		mockMvc.perform(get("/abcdefghi"))
				.andExpect(status().isNotFound());
	}
}

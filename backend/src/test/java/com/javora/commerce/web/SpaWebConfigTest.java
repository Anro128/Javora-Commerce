package com.javora.commerce.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javora.commerce.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

// Uses src/test/resources/static/index.html as a stand-in for the React build.
@WebMvcTest
@Import({SecurityConfig.class, SpaWebConfig.class})
class SpaWebConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void clientRouteFallsBackToIndexHtml() throws Exception {
		mockMvc.perform(get("/products/some-product"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("javora-spa")));
	}

	@Test
	void unknownApiPathIsNotServedAsSpa() throws Exception {
		mockMvc.perform(get("/api/v1/public/unknown"))
				.andExpect(status().isNotFound());
	}

}

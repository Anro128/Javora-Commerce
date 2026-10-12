package com.javora.commerce.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javora.commerce.common.config.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import({SecurityConfig.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void notFoundReturnsProblemDetail() throws Exception {
		mockMvc.perform(get("/api/v1/public/test/missing"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("Product not found: 42"));
	}

	@Test
	void businessErrorReturnsGivenStatus() throws Exception {
		mockMvc.perform(get("/api/v1/public/test/business"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Insufficient stock"));
	}

	@Test
	void validationErrorListsInvalidFields() throws Exception {
		mockMvc.perform(post("/api/v1/public/test/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Validation failed"))
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.email").exists());
	}

	@Test
	void unexpectedErrorHidesInternalDetails() throws Exception {
		mockMvc.perform(get("/api/v1/public/test/crash"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
	}

	record SampleRequest(@NotBlank String name, @Email String email) {
	}

	@RestController
	static class TestController {

		@GetMapping("/api/v1/public/test/missing")
		void missing() {
			throw new ResourceNotFoundException("Product", 42);
		}

		@GetMapping("/api/v1/public/test/business")
		void business() {
			throw new BusinessException("Insufficient stock");
		}

		@PostMapping("/api/v1/public/test/validate")
		void validate(@Valid @RequestBody SampleRequest request) {
		}

		@GetMapping("/api/v1/public/test/crash")
		void crash() {
			throw new IllegalStateException("database password leaked");
		}

	}

}

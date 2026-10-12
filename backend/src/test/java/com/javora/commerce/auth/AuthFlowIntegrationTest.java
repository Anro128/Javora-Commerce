package com.javora.commerce.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javora.commerce.TestcontainersConfiguration;
import com.javora.commerce.auth.repository.RefreshTokenRepository;
import com.javora.commerce.common.util.TokenGenerator;
import com.javora.commerce.notification.MailService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

	private static final String PASSWORD = "Passw0rdLong";
	private static final String REFRESH_COOKIE = "refresh_token";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@MockitoBean
	private MailService mailService;

	@Test
	void registerCreatesAccountAndIssuesSession() throws Exception {
		String email = uniqueEmail();

		MvcResult result = mockMvc.perform(jsonPost("/api/v1/auth/register", registerJson(email.toUpperCase(), PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.user.email").value(email))
				.andExpect(jsonPath("$.data.user.role").value("CUSTOMER"))
				.andExpect(jsonPath("$.data.user.emailVerified").value(false))
				.andReturn();

		String setCookie = result.getResponse().getHeader("Set-Cookie");
		assertThat(setCookie).contains(REFRESH_COOKIE + "=", "HttpOnly", "SameSite=Strict", "Path=/api/v1/auth");
		verify(mailService).sendVerificationEmail(eq(email), eq("Test User"), anyString());
	}

	@Test
	void registerRejectsDuplicateEmailAndWeakPassword() throws Exception {
		String email = uniqueEmail();
		register(email);

		mockMvc.perform(jsonPost("/api/v1/auth/register", registerJson(email, PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Email is already registered"));

		mockMvc.perform(jsonPost("/api/v1/auth/register", registerJson(uniqueEmail(), "onlyletters")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void loginRejectsWrongPasswordAndUnknownEmailWithSameMessage() throws Exception {
		String email = uniqueEmail();
		register(email);

		mockMvc.perform(jsonPost("/api/v1/auth/login", loginJson(email, "WrongPass1")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid email or password"));

		mockMvc.perform(jsonPost("/api/v1/auth/login", loginJson(uniqueEmail(), PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid email or password"));
	}

	@Test
	void accessTokenGrantsAccessAndStaleBearerDoesNotBreakPublicAuthCalls() throws Exception {
		String email = uniqueEmail();
		register(email);

		MockHttpServletRequestBuilder login = jsonPost("/api/v1/auth/login", loginJson(email, PASSWORD))
				.header("Authorization", "Bearer not-a-valid-token");
		String accessToken = JsonPath.read(
				mockMvc.perform(login).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
				"$.data.accessToken");

		mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value(email));

		mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer garbage"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void refreshRotatesTokenAndLogoutRevokesIt() throws Exception {
		String email = uniqueEmail();
		String firstRefresh = refreshCookie(register(email));

		MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, firstRefresh)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andReturn();
		String secondRefresh = refreshCookie(refreshed);
		assertThat(secondRefresh).isNotEqualTo(firstRefresh);

		mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie(REFRESH_COOKIE, secondRefresh)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, secondRefresh)))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
	}

	@Test
	void replayingAnOldRefreshTokenRevokesTheWholeFamily() throws Exception {
		String email = uniqueEmail();
		String firstRefresh = refreshCookie(register(email));
		String secondRefresh = refreshCookie(mockMvc
				.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, firstRefresh)))
				.andExpect(status().isOk())
				.andReturn());

		// Move the revocation outside the grace period to simulate a genuine replay.
		refreshTokenRepository.findByTokenHash(TokenGenerator.sha256Hex(firstRefresh)).ifPresent(token -> {
			token.setRevokedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
			refreshTokenRepository.save(token);
		});

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, firstRefresh)))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, secondRefresh)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void emailVerificationTokenWorksOnlyOnce() throws Exception {
		String email = uniqueEmail();
		MvcResult registered = register(email);
		String token = captureVerificationToken(email);
		String accessToken = accessToken(registered);

		mockMvc.perform(jsonPost("/api/v1/auth/verify-email", "{\"token\":\"" + token + "\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(jsonPath("$.data.emailVerified").value(true));

		mockMvc.perform(jsonPost("/api/v1/auth/verify-email", "{\"token\":\"" + token + "\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(jsonPost("/api/v1/auth/verify-email", "{\"token\":\"bogus\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void passwordResetChangesPasswordAndRevokesSessions() throws Exception {
		String email = uniqueEmail();
		String refresh = refreshCookie(register(email));

		mockMvc.perform(jsonPost("/api/v1/auth/forgot-password", "{\"email\":\"" + uniqueEmail() + "\"}"))
				.andExpect(status().isOk());
		verify(mailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());

		mockMvc.perform(jsonPost("/api/v1/auth/forgot-password", "{\"email\":\"" + email + "\"}"))
				.andExpect(status().isOk());
		ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
		verify(mailService).sendPasswordResetEmail(eq(email), anyString(), token.capture());

		String newPassword = "BrandNew123";
		mockMvc.perform(jsonPost("/api/v1/auth/reset-password",
						"{\"token\":\"" + token.getValue() + "\",\"newPassword\":\"" + newPassword + "\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(jsonPost("/api/v1/auth/login", loginJson(email, PASSWORD))).andExpect(status().isUnauthorized());
		mockMvc.perform(jsonPost("/api/v1/auth/login", loginJson(email, newPassword))).andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, refresh)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(jsonPost("/api/v1/auth/reset-password",
						"{\"token\":\"" + token.getValue() + "\",\"newPassword\":\"AnotherPass1\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void changePasswordRequiresCurrentPassword() throws Exception {
		String email = uniqueEmail();
		String accessToken = accessToken(register(email));

		mockMvc.perform(jsonPost("/api/v1/auth/change-password",
						"{\"currentPassword\":\"WrongPass1\",\"newPassword\":\"BrandNew123\"}")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Current password is incorrect"));

		mockMvc.perform(jsonPost("/api/v1/auth/change-password",
						"{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"BrandNew123\"}")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());

		mockMvc.perform(jsonPost("/api/v1/auth/login", loginJson(email, "BrandNew123"))).andExpect(status().isOk());
	}

	@Test
	void profileCanBeUpdated() throws Exception {
		String accessToken = accessToken(register(uniqueEmail()));

		mockMvc.perform(authedPut("/api/v1/users/me", accessToken,
						"{\"fullName\":\"  New Name  \",\"phone\":\"+628123456789\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fullName").value("New Name"))
				.andExpect(jsonPath("$.data.phone").value("+628123456789"));

		mockMvc.perform(authedPut("/api/v1/users/me", accessToken, "{\"fullName\":\"\",\"phone\":\"abc\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.fullName").exists())
				.andExpect(jsonPath("$.errors.phone").exists());
	}

	@Test
	void addressesKeepExactlyOneDefaultAndAreOwnedByTheirUser() throws Exception {
		String owner = accessToken(register(uniqueEmail()));
		String stranger = accessToken(register(uniqueEmail()));

		MvcResult first = mockMvc.perform(authedPost("/api/v1/users/me/addresses", owner, addressJson("Home", false)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.defaultAddress").value(true))
				.andReturn();
		MvcResult second = mockMvc.perform(authedPost("/api/v1/users/me/addresses", owner, addressJson("Office", true)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.defaultAddress").value(true))
				.andReturn();
		int firstId = JsonPath.read(first.getResponse().getContentAsString(), "$.data.id");
		int secondId = JsonPath.read(second.getResponse().getContentAsString(), "$.data.id");

		mockMvc.perform(get("/api/v1/users/me/addresses").header("Authorization", "Bearer " + owner))
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].id").value(secondId))
				.andExpect(jsonPath("$.data[0].defaultAddress").value(true))
				.andExpect(jsonPath("$.data[1].defaultAddress").value(false));

		mockMvc.perform(authedPut("/api/v1/users/me/addresses/" + firstId, stranger, addressJson("Hacked", false)))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/v1/users/me/addresses/" + firstId).header("Authorization", "Bearer " + stranger))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete("/api/v1/users/me/addresses/" + secondId).header("Authorization", "Bearer " + owner))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/users/me/addresses").header("Authorization", "Bearer " + owner))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(firstId))
				.andExpect(jsonPath("$.data[0].defaultAddress").value(true));
	}

	@Test
	void adminAreaIsForbiddenForCustomers() throws Exception {
		String customer = accessToken(register(uniqueEmail()));

		mockMvc.perform(get("/api/v1/admin/anything").header("Authorization", "Bearer " + customer))
				.andExpect(status().isForbidden());

		String adminToken = JsonPath.read(
				mockMvc.perform(jsonPost("/api/v1/auth/login", loginJson("admin@javora.local", "Admin12345")))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.data.user.role").value("ADMIN"))
						.andReturn().getResponse().getContentAsString(),
				"$.data.accessToken");

		// No controller exists under /admin yet, so an admin passes security and reaches a 404.
		mockMvc.perform(get("/api/v1/admin/anything").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound());
	}

	private String captureVerificationToken(String email) {
		ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
		verify(mailService).sendVerificationEmail(eq(email), any(), token.capture());
		return token.getValue();
	}

	private MvcResult register(String email) throws Exception {
		return mockMvc.perform(jsonPost("/api/v1/auth/register", registerJson(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn();
	}

	private static String accessToken(MvcResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
	}

	private static String refreshCookie(MvcResult result) {
		Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE);
		assertThat(cookie).isNotNull();
		return cookie.getValue();
	}

	private static MockHttpServletRequestBuilder jsonPost(String url, String body) {
		return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static MockHttpServletRequestBuilder authedPost(String url, String accessToken, String body) {
		return jsonPost(url, body).header("Authorization", "Bearer " + accessToken);
	}

	private static MockHttpServletRequestBuilder authedPut(String url, String accessToken, String body) {
		return put(url)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.header("Authorization", "Bearer " + accessToken);
	}

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

	private static String registerJson(String email, String password) {
		return "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"Test User\"}".formatted(email, password);
	}

	private static String loginJson(String email, String password) {
		return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
	}

	private static String addressJson(String label, boolean defaultAddress) {
		return ("{\"label\":\"%s\",\"recipientName\":\"Test User\",\"phone\":\"081234567890\","
				+ "\"province\":\"DKI Jakarta\",\"city\":\"Jakarta Selatan\",\"district\":\"Kebayoran\","
				+ "\"postalCode\":\"12190\",\"street\":\"Jl. Merdeka No. 1\",\"defaultAddress\":%s}")
				.formatted(label, defaultAddress);
	}

}

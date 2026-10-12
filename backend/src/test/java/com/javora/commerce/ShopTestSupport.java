package com.javora.commerce;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javora.commerce.notification.MailService;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// Shared fixtures for tests that drive the real HTTP API as an admin and as customers.
@IntegrationTest
public abstract class ShopTestSupport {

	protected static final String PASSWORD = "Passw0rd1";

	@Autowired
	protected MockMvc mockMvc;

	@MockitoBean
	private MailService mailService;

	protected String adminToken;

	public record Customer(String email, String token, long addressId) {
	}

	public record Item(long productId, long variantId, String slug) {
	}

	@BeforeEach
	void loginAsAdmin() throws Exception {
		adminToken = login("admin@javora.local", "Admin12345");
	}

	protected String login(String email, String password) throws Exception {
		return JsonPath.read(
				body(mockMvc.perform(json(post("/api/v1/auth/login"),
								"{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
						.andExpect(status().isOk())),
				"$.data.accessToken");
	}

	// Registers a customer and gives them one saved address in the given province.
	protected Customer newCustomer(String province) throws Exception {
		String email = "buyer-" + UUID.randomUUID() + "@example.com";
		body(mockMvc.perform(json(post("/api/v1/auth/register"),
						"{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"Buyer\"}".formatted(email, PASSWORD)))
				.andExpect(status().isCreated()));
		String token = login(email, PASSWORD);

		String address = body(mockMvc.perform(json(post("/api/v1/users/me/addresses").header("Authorization", bearer(token)),
						("{\"recipientName\":\"Buyer\",\"phone\":\"081234567890\",\"province\":\"%s\",\"city\":\"Kota\","
								+ "\"district\":\"Kecamatan\",\"postalCode\":\"12345\",\"street\":\"Jl. Contoh 1\"}")
								.formatted(province)))
				.andExpect(status().isCreated()));
		return new Customer(email, token, longAt(address, "$.data.id"));
	}

	protected Item createProduct(String name, int price, int stock, int weightGram, String status) throws Exception {
		String category = body(mockMvc.perform(json(post("/api/v1/admin/categories").header("Authorization", bearer(adminToken)),
						"{\"name\":\"Cat %s\"}".formatted(UUID.randomUUID().toString().substring(0, 8))))
				.andExpect(status().isCreated()));
		long categoryId = longAt(category, "$.data.id");

		String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 10);
		String product = body(mockMvc.perform(json(post("/api/v1/admin/products").header("Authorization", bearer(adminToken)),
						("{\"categoryId\":%d,\"name\":\"%s\",\"basePrice\":%d,\"weightGram\":%d,\"status\":\"%s\","
								+ "\"variants\":[{\"sku\":\"%s\",\"name\":\"Default\",\"price\":%d,\"stock\":%d}]}")
								.formatted(categoryId, name, price, weightGram, status, sku, price, stock)))
				.andExpect(status().isCreated()));
		return new Item(
				longAt(product, "$.data.id"),
				longAt(product, "$.data.variants[0].id"),
				JsonPath.read(product, "$.data.slug"));
	}

	protected void setStock(long variantId, int quantity) throws Exception {
		mockMvc.perform(put("/api/v1/admin/inventory/" + variantId)
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"quantity\":%d}".formatted(quantity)))
				.andExpect(status().isOk());
	}

	protected ResultActions addToCart(Customer customer, long variantId, int quantity) throws Exception {
		return mockMvc.perform(json(post("/api/v1/cart/items").header("Authorization", bearer(customer.token())),
				"{\"variantId\":%d,\"quantity\":%d}".formatted(variantId, quantity)));
	}

	protected ResultActions checkout(Customer customer, String courier, String service) throws Exception {
		return mockMvc.perform(json(post("/api/v1/checkout").header("Authorization", bearer(customer.token())),
				"{\"addressId\":%d,\"courier\":\"%s\",\"service\":\"%s\"}"
						.formatted(customer.addressId(), courier, service)));
	}

	protected int availableOf(Item item) throws Exception {
		String detail = body(mockMvc.perform(get("/api/v1/public/products/" + item.slug())).andExpect(status().isOk()));
		return JsonPath.read(detail, "$.data.variants[0].available");
	}

	protected static String bearer(String token) {
		return "Bearer " + token;
	}

	protected static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String content) {
		return builder.contentType(MediaType.APPLICATION_JSON).content(content);
	}

	protected static String body(ResultActions actions) throws Exception {
		return actions.andReturn().getResponse().getContentAsString();
	}

	protected static String body(MvcResult result) throws Exception {
		return result.getResponse().getContentAsString();
	}

	protected static long longAt(String json, String path) {
		return ((Number) JsonPath.read(json, path)).longValue();
	}

}

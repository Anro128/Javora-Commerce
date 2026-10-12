package com.javora.commerce.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javora.commerce.IntegrationTest;
import com.javora.commerce.common.storage.StorageService;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class CatalogIntegrationTest {

	private static final String ADMIN_EMAIL = "admin@javora.local";
	private static final String ADMIN_PASSWORD = "Admin12345";
	private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

	private final AtomicInteger counter = new AtomicInteger();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StorageService storageService;

	private String adminToken;

	@BeforeEach
	void setUp() throws Exception {
		when(storageService.store(anyString(), any(), anyLong(), anyString(), anyString()))
				.thenAnswer(invocation -> "products/%d.%s".formatted(counter.incrementAndGet(), invocation.getArgument(4)));
		when(storageService.publicUrl(anyString()))
				.thenAnswer(invocation -> "http://cdn.test/" + invocation.getArgument(0));
		adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
	}

	@Test
	void adminCatalogIsClosedToAnonymousAndCustomers() throws Exception {
		mockMvc.perform(get("/api/v1/admin/products")).andExpect(status().isUnauthorized());

		String email = "shopper-" + UUID.randomUUID() + "@example.com";
		mockMvc.perform(json(post("/api/v1/auth/register"),
						"{\"email\":\"%s\",\"password\":\"Passw0rd1\",\"fullName\":\"Shopper\"}".formatted(email)))
				.andExpect(status().isCreated());
		String customerToken = login(email, "Passw0rd1");

		mockMvc.perform(get("/api/v1/admin/products").header("Authorization", "Bearer " + customerToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/admin/inventory").header("Authorization", "Bearer " + customerToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void publicCatalogIgnoresStaleOrInvalidBearerTokens() throws Exception {
		mockMvc.perform(get("/api/v1/public/categories").header("Authorization", "Bearer expired.or.invalid"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/public/products").header("Authorization", "Bearer expired.or.invalid"))
				.andExpect(status().isOk());
	}

	@Test
	void publishedProductsAppearInTheShopWithLiveStock() throws Exception {
		String token = unique("wool");
		long category = createCategory("Outerwear " + token, null);
		long product = createProduct(category, "Wool Coat " + token, 750000, "ACTIVE",
				variantJson("COAT-" + token + "-M", "Medium", 750000, 5),
				variantJson("COAT-" + token + "-L", "Large", 780000, 0));
		String slug = slugOf(product);

		mockMvc.perform(get("/api/v1/public/products").param("q", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].name").value("Wool Coat " + token))
				.andExpect(jsonPath("$.data.content[0].inStock").value(true))
				.andExpect(jsonPath("$.data.content[0].categoryName").value("Outerwear " + token));

		mockMvc.perform(get("/api/v1/public/products/" + slug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.variants.length()").value(2))
				.andExpect(jsonPath("$.data.variants[0].available").value(5))
				.andExpect(jsonPath("$.data.variants[0].inStock").value(true))
				.andExpect(jsonPath("$.data.variants[1].inStock").value(false));

		// Stock is read live, so it must change even while the product content is cached.
		long mediumVariant = longAt(
				body(mockMvc.perform(get("/api/v1/admin/products/" + product).header("Authorization", bearer()))),
				"$.data.variants[0].id");
		mockMvc.perform(put("/api/v1/admin/inventory/" + mediumVariant)
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"quantity\":0}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/public/products/" + slug))
				.andExpect(jsonPath("$.data.variants[0].available").value(0))
				.andExpect(jsonPath("$.data.variants[0].inStock").value(false));
		mockMvc.perform(get("/api/v1/public/products").param("q", token))
				.andExpect(jsonPath("$.data.content[0].inStock").value(false));
	}

	@Test
	void draftAndArchivedProductsAreHiddenFromTheShop() throws Exception {
		String token = unique("hidden");
		long category = createCategory("Hidden " + token, null);
		long draft = createProduct(category, "Draft Item " + token, 1000, "DRAFT", variantJson("DRF-" + token, "One", 1000, 1));
		long live = createProduct(category, "Live Item " + token, 1000, "ACTIVE", variantJson("LIV-" + token, "One", 1000, 1));

		mockMvc.perform(get("/api/v1/public/products").param("q", token))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].name").value("Live Item " + token));
		mockMvc.perform(get("/api/v1/public/products/" + slugOf(draft))).andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/v1/admin/products/" + draft + "/status")
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"ACTIVE\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/public/products").param("q", token))
				.andExpect(jsonPath("$.data.totalElements").value(2));

		mockMvc.perform(delete("/api/v1/admin/products/" + live).header("Authorization", bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ARCHIVED"));
		mockMvc.perform(get("/api/v1/public/products/" + slugOf(live))).andExpect(status().isNotFound());
	}

	@Test
	void listingSupportsCategoryTreePriceRangeAndSorting() throws Exception {
		String token = unique("tree");
		long parent = createCategory("Parent " + token, null);
		long child = createCategory("Child " + token, parent);
		String parentSlug = categorySlug(parent);

		createProduct(parent, "Cheap " + token, 100000, "ACTIVE", variantJson("CHP-" + token, "One", 100000, 1));
		createProduct(child, "Middle " + token, 300000, "ACTIVE", variantJson("MID-" + token, "One", 300000, 1));
		createProduct(child, "Pricey " + token, 900000, "ACTIVE", variantJson("PRC-" + token, "One", 900000, 1));

		mockMvc.perform(get("/api/v1/public/products").param("category", parentSlug).param("sort", "price_desc"))
				.andExpect(jsonPath("$.data.totalElements").value(3))
				.andExpect(jsonPath("$.data.content[0].name").value("Pricey " + token))
				.andExpect(jsonPath("$.data.content[2].name").value("Cheap " + token));

		mockMvc.perform(get("/api/v1/public/products").param("category", categorySlug(child)).param("sort", "price_asc"))
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.content[0].name").value("Middle " + token));

		mockMvc.perform(get("/api/v1/public/products")
						.param("category", parentSlug).param("minPrice", "200000").param("maxPrice", "500000"))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].name").value("Middle " + token));

		mockMvc.perform(get("/api/v1/public/products").param("category", "no-such-category"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(0));

		mockMvc.perform(get("/api/v1/public/products").param("category", parentSlug).param("size", "2").param("page", "1"))
				.andExpect(jsonPath("$.data.content.length()").value(1))
				.andExpect(jsonPath("$.data.totalPages").value(2));
	}

	@Test
	void searchTreatsWildcardsAsPlainText() throws Exception {
		String token = unique("wild");
		long category = createCategory("Wild " + token, null);
		createProduct(category, "Sale 50% Off " + token, 1000, "ACTIVE", variantJson("W1-" + token, "One", 1000, 1));
		createProduct(category, "Plain Item " + token, 1000, "ACTIVE", variantJson("W2-" + token, "One", 1000, 1));

		mockMvc.perform(get("/api/v1/public/products").param("q", "50%"))
				.andExpect(jsonPath("$.data.totalElements").value(1));
		mockMvc.perform(get("/api/v1/public/products").param("q", "%"))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void categoryTreeIsPublicAndRefreshesAfterAdminChanges() throws Exception {
		String token = unique("cache");
		mockMvc.perform(get("/api/v1/public/categories")).andExpect(status().isOk());

		long category = createCategory("Fresh " + token, null);

		String tree = body(mockMvc.perform(get("/api/v1/public/categories")));
		assertThat(tree).contains("Fresh " + token);

		mockMvc.perform(put("/api/v1/admin/categories/" + category)
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Fresh %s\",\"active\":false}".formatted(token)))
				.andExpect(status().isOk());

		assertThat(body(mockMvc.perform(get("/api/v1/public/categories")))).doesNotContain("Fresh " + token);
	}

	@Test
	void productEditsAreVisibleDespiteCaching() throws Exception {
		String token = unique("edit");
		long category = createCategory("Edit " + token, null);
		long product = createProduct(category, "Before " + token, 5000, "ACTIVE", variantJson("EDT-" + token, "One", 5000, 2));
		String slug = slugOf(product);

		mockMvc.perform(get("/api/v1/public/products/" + slug)).andExpect(jsonPath("$.data.name").value("Before " + token));

		mockMvc.perform(put("/api/v1/admin/products/" + product)
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"categoryId":%d,"name":"After %s","description":"New text","basePrice":6000,
								 "weightGram":300,"status":"ACTIVE"}""".formatted(category, token)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/public/products/" + slug))
				.andExpect(jsonPath("$.data.name").value("After " + token))
				.andExpect(jsonPath("$.data.price").value(6000))
				.andExpect(jsonPath("$.data.description").value("New text"));
	}

	@Test
	void skuMustBeUniqueAndInputIsValidated() throws Exception {
		String token = unique("sku");
		long category = createCategory("Sku " + token, null);
		createProduct(category, "First " + token, 1000, "ACTIVE", variantJson("SKU-" + token, "One", 1000, 1));

		mockMvc.perform(json(post("/api/v1/admin/products").header("Authorization", bearer()), productJson(
						category, "Second " + token, 1000, "ACTIVE", variantJson("SKU-" + token, "One", 1000, 1))))
				.andExpect(status().isConflict());

		mockMvc.perform(json(post("/api/v1/admin/products").header("Authorization", bearer()), productJson(
						category, "Twins " + token, 1000, "ACTIVE",
						variantJson("TWIN-" + token, "A", 1000, 1), variantJson("twin-" + token, "B", 1000, 1))))
				.andExpect(status().isConflict());

		mockMvc.perform(json(post("/api/v1/admin/products").header("Authorization", bearer()),
						"{\"categoryId\":%d,\"name\":\"\",\"basePrice\":-5,\"weightGram\":0,\"variants\":[]}".formatted(category)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.basePrice").exists())
				.andExpect(jsonPath("$.errors.weightGram").exists())
				.andExpect(jsonPath("$.errors.variants").exists());
	}

	@Test
	void categoriesCannotBeDeletedWhileUsedOrMovedIntoThemselves() throws Exception {
		String token = unique("tree2");
		long parent = createCategory("Top " + token, null);
		long child = createCategory("Sub " + token, parent);
		createProduct(child, "Held " + token, 1000, "ACTIVE", variantJson("HLD-" + token, "One", 1000, 1));

		mockMvc.perform(delete("/api/v1/admin/categories/" + parent).header("Authorization", bearer()))
				.andExpect(status().isConflict());
		mockMvc.perform(delete("/api/v1/admin/categories/" + child).header("Authorization", bearer()))
				.andExpect(status().isConflict());

		mockMvc.perform(put("/api/v1/admin/categories/" + parent)
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Top %s\",\"parentId\":%d}".formatted(token, child)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void imagesCanBeUploadedReorderedAndRemoved() throws Exception {
		String token = unique("img");
		long category = createCategory("Img " + token, null);
		long product = createProduct(category, "Photo " + token, 1000, "ACTIVE", variantJson("IMG-" + token, "One", 1000, 1));

		mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", product)
						.file(new MockMultipartFile("file", "evil.png", "image/png", "<script>alert(1)</script>".getBytes()))
						.header("Authorization", bearer()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Only JPEG, PNG, and WebP images are supported"));

		mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", product)
						.file(new MockMultipartFile("file", "a.png", "image/png", PNG))
						.header("Authorization", bearer()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.images.length()").value(1));
		String afterSecond = body(mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", product)
				.file(new MockMultipartFile("file", "b.png", "image/png", PNG))
				.header("Authorization", bearer())));

		long first = longAt(afterSecond, "$.data.images[0].id");
		long second = longAt(afterSecond, "$.data.images[1].id");

		mockMvc.perform(get("/api/v1/public/products").param("q", token))
				.andExpect(jsonPath("$.data.content[0].imageUrl").value(startsWith("http://cdn.test/products/")));

		mockMvc.perform(put("/api/v1/admin/products/%d/images/%d/primary".formatted(product, second))
						.header("Authorization", bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.images[0].id").value(second));

		mockMvc.perform(delete("/api/v1/admin/products/%d/images/%d".formatted(product, first))
						.header("Authorization", bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.images.length()").value(1));
		verify(storageService).delete(anyString());
	}

	@Test
	void inventoryListFlagsLowStockAndRejectsNegativeQuantities() throws Exception {
		String token = unique("inv");
		long category = createCategory("Inv " + token, null);
		createProduct(category, "Stocked " + token, 1000, "ACTIVE",
				variantJson("INV-HI-" + token, "Plenty", 1000, 50), variantJson("INV-LO-" + token, "Scarce", 1000, 2));

		String response = body(mockMvc.perform(get("/api/v1/admin/inventory")
				.param("q", token).header("Authorization", bearer())));
		assertThat((Integer) JsonPath.read(response, "$.data.totalElements")).isEqualTo(2);

		mockMvc.perform(get("/api/v1/admin/inventory")
						.param("q", token).param("lowStock", "true").header("Authorization", bearer()))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].sku").value("INV-LO-" + token))
				.andExpect(jsonPath("$.data.content[0].lowStock").value(true))
				.andExpect(jsonPath("$.data.content[0].available").value(2));

		long variantId = longAt(response, "$.data.content[0].variantId");
		mockMvc.perform(put("/api/v1/admin/inventory/" + variantId)
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"quantity\":-1}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/api/v1/admin/inventory/999999999")
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"quantity\":3}"))
				.andExpect(status().isNotFound());
	}

	private long createCategory(String name, Long parentId) throws Exception {
		String json = parentId == null
				? "{\"name\":\"%s\"}".formatted(name)
				: "{\"name\":\"%s\",\"parentId\":%d}".formatted(name, parentId);
		String response = body(mockMvc.perform(json(post("/api/v1/admin/categories").header("Authorization", bearer()), json))
				.andExpect(status().isCreated()));
		return ((Number) JsonPath.read(response, "$.data.id")).longValue();
	}

	private long createProduct(long categoryId, String name, int price, String status, String... variants) throws Exception {
		String response = body(mockMvc.perform(json(post("/api/v1/admin/products").header("Authorization", bearer()),
						productJson(categoryId, name, price, status, variants)))
				.andExpect(status().isCreated()));
		return ((Number) JsonPath.read(response, "$.data.id")).longValue();
	}

	private String slugOf(long productId) throws Exception {
		return JsonPath.read(
				body(mockMvc.perform(get("/api/v1/admin/products/" + productId).header("Authorization", bearer()))),
				"$.data.slug");
	}

	private String categorySlug(long categoryId) throws Exception {
		String list = body(mockMvc.perform(get("/api/v1/admin/categories").header("Authorization", bearer())));
		List<String> slugs = JsonPath.read(list, "$.data[?(@.id == %d)].slug".formatted(categoryId));
		return slugs.get(0);
	}

	private String login(String email, String password) throws Exception {
		String response = body(mockMvc.perform(json(post("/api/v1/auth/login"),
						"{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
				.andExpect(status().isOk()));
		return JsonPath.read(response, "$.data.accessToken");
	}

	private String bearer() {
		return "Bearer " + adminToken;
	}

	private static String productJson(long categoryId, String name, int price, String status, String... variants) {
		return "{\"categoryId\":%d,\"name\":\"%s\",\"description\":\"Test product\",\"basePrice\":%d,\"weightGram\":500,\"status\":\"%s\",\"variants\":[%s]}"
				.formatted(categoryId, name, price, status, String.join(",", variants));
	}

	private static String variantJson(String sku, String name, int price, int stock) {
		return "{\"sku\":\"%s\",\"name\":\"%s\",\"price\":%d,\"stock\":%d}".formatted(sku, name, price, stock);
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String content) {
		return builder.contentType(MediaType.APPLICATION_JSON).content(content);
	}

	private static long longAt(String json, String path) {
		return ((Number) JsonPath.read(json, path)).longValue();
	}

	private static String body(ResultActions actions) throws Exception {
		return actions.andReturn().getResponse().getContentAsString();
	}

	private static String unique(String prefix) {
		return prefix + UUID.randomUUID().toString().substring(0, 8);
	}

}

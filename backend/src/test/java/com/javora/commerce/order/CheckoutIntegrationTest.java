package com.javora.commerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javora.commerce.ShopTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CheckoutIntegrationTest extends ShopTestSupport {

	@Test
	void shippingCostDependsOnDestinationAndWeight() throws Exception {
		Customer local = newCustomer("DKI Jakarta");
		Customer remote = newCustomer("Papua");
		Item item = createProduct("Heavy Box", 100000, 10, 2500, "ACTIVE");
		addToCart(local, item.variantId(), 1);
		addToCart(remote, item.variantId(), 1);

		// 2.5 kg rounds up to 3 billable kg: 9000 + 2 * 3000 in the origin zone, 22000 + 2 * 9000 far away.
		mockMvc.perform(get("/api/v1/shipping/options").param("addressId", String.valueOf(local.addressId()))
						.header("Authorization", bearer(local.token())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(4))
				.andExpect(jsonPath("$.data[?(@.courier=='jne' && @.service=='reg')].cost").value(15000));
		mockMvc.perform(get("/api/v1/shipping/options").param("addressId", String.valueOf(remote.addressId()))
						.header("Authorization", bearer(remote.token())))
				.andExpect(jsonPath("$.data[?(@.courier=='jne' && @.service=='reg')].cost").value(40000));
	}

	@Test
	void shippingOptionsRejectEmptyCartsAndForeignAddresses() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Customer other = newCustomer("DKI Jakarta");
		Item item = createProduct("Some Item", 10000, 5, 100, "ACTIVE");

		mockMvc.perform(get("/api/v1/shipping/options").param("addressId", String.valueOf(buyer.addressId()))
						.header("Authorization", bearer(buyer.token())))
				.andExpect(status().isBadRequest());

		addToCart(buyer, item.variantId(), 1);
		mockMvc.perform(get("/api/v1/shipping/options").param("addressId", String.valueOf(other.addressId()))
						.header("Authorization", bearer(buyer.token())))
				.andExpect(status().isNotFound());
	}

	@Test
	void previewCalculatesTotalsWithoutTouchingStock() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item item = createProduct("Preview Item", 200000, 5, 1000, "ACTIVE");
		addToCart(buyer, item.variantId(), 2);

		mockMvc.perform(json(post("/api/v1/checkout/preview").header("Authorization", bearer(buyer.token())),
						"{\"addressId\":%d,\"courier\":\"jne\",\"service\":\"reg\"}".formatted(buyer.addressId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.subtotal").value(400000))
				.andExpect(jsonPath("$.data.shippingCost").value(12000))
				.andExpect(jsonPath("$.data.discount").value(0))
				.andExpect(jsonPath("$.data.total").value(412000))
				.andExpect(jsonPath("$.data.totalWeightGram").value(2000));

		assertThat(availableOf(item)).isEqualTo(5);
	}

	@Test
	void placingAnOrderReservesStockAndEmptiesTheCart() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item item = createProduct("Order Item", 150000, 10, 500, "ACTIVE");
		addToCart(buyer, item.variantId(), 3);

		String order = body(checkout(buyer, "jnt", "ez")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.orderNumber").value(matchesPattern("JVR-\\d{8}-[A-Z0-9]{6}")))
				.andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.data.subtotal").value(450000))
				.andExpect(jsonPath("$.data.shippingCost").value(11000))
				.andExpect(jsonPath("$.data.total").value(461000))
				.andExpect(jsonPath("$.data.items[0].quantity").value(3))
				.andExpect(jsonPath("$.data.items[0].unitPrice").value(150000))
				.andExpect(jsonPath("$.data.address.recipientName").value("Buyer")));
		String orderNumber = JsonPath.read(order, "$.data.orderNumber");

		mockMvc.perform(get("/api/v1/cart").header("Authorization", bearer(buyer.token())))
				.andExpect(jsonPath("$.data.items.length()").value(0));
		assertThat(availableOf(item)).isEqualTo(7);

		mockMvc.perform(get("/api/v1/admin/inventory").param("q", item.slug().substring(0, 5))
						.header("Authorization", bearer(adminToken)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/orders/" + orderNumber).header("Authorization", bearer(buyer.token())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].productName").value("Order Item"));
	}

	@Test
	void orderKeepsTheOriginalPriceAfterTheCatalogChanges() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item item = createProduct("Snapshot Item", 80000, 5, 200, "ACTIVE");
		addToCart(buyer, item.variantId(), 1);
		String orderNumber = JsonPath.read(body(checkout(buyer, "jne", "reg").andExpect(status().isCreated())),
				"$.data.orderNumber");

		String sku = JsonPath.read(
				body(mockMvc.perform(get("/api/v1/admin/products/" + item.productId()).header("Authorization", bearer(adminToken)))),
				"$.data.variants[0].sku");
		mockMvc.perform(json(put("/api/v1/admin/products/" + item.productId() + "/variants/" + item.variantId())
						.header("Authorization", bearer(adminToken)),
						"{\"sku\":\"%s\",\"name\":\"Renamed\",\"price\":999000,\"active\":true}".formatted(sku)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/orders/" + orderNumber).header("Authorization", bearer(buyer.token())))
				.andExpect(jsonPath("$.data.items[0].unitPrice").value(80000))
				.andExpect(jsonPath("$.data.items[0].variantName").value("Default"));
	}

	@Test
	void checkoutRejectsBadInputWithoutReservingAnything() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Customer stranger = newCustomer("DKI Jakarta");
		Item item = createProduct("Guarded Item", 50000, 5, 200, "ACTIVE");

		checkout(buyer, "jne", "reg").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Your cart is empty"));

		addToCart(buyer, item.variantId(), 2);
		checkout(buyer, "teleport", "instant").andExpect(status().isBadRequest());
		mockMvc.perform(json(post("/api/v1/checkout").header("Authorization", bearer(buyer.token())),
						"{\"addressId\":%d,\"courier\":\"jne\",\"service\":\"reg\"}".formatted(stranger.addressId())))
				.andExpect(status().isNotFound());
		mockMvc.perform(json(post("/api/v1/checkout").header("Authorization", bearer(buyer.token())), "{}"))
				.andExpect(status().isBadRequest());

		assertThat(availableOf(item)).isEqualTo(5);
	}

	@Test
	void checkoutIsBlockedWhenStockDroppedBelowTheCartQuantity() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item item = createProduct("Dropping Item", 50000, 5, 200, "ACTIVE");
		addToCart(buyer, item.variantId(), 4);
		setStock(item.variantId(), 1);

		checkout(buyer, "jne", "reg").andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Only 1 left of Dropping Item (Default)"));
		assertThat(availableOf(item)).isEqualTo(1);
	}

	@Test
	void cancellingAnOrderReleasesItsStockExactlyOnce() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item item = createProduct("Cancelled Item", 50000, 5, 200, "ACTIVE");
		addToCart(buyer, item.variantId(), 3);
		String orderNumber = JsonPath.read(body(checkout(buyer, "jne", "reg").andExpect(status().isCreated())),
				"$.data.orderNumber");
		assertThat(availableOf(item)).isEqualTo(2);

		mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/cancel").header("Authorization", bearer(buyer.token())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("CANCELLED"));
		assertThat(availableOf(item)).isEqualTo(5);

		mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/cancel").header("Authorization", bearer(buyer.token())))
				.andExpect(status().isConflict());
		assertThat(availableOf(item)).isEqualTo(5);
	}

	@Test
	void ordersAreVisibleOnlyToTheirOwner() throws Exception {
		Customer owner = newCustomer("DKI Jakarta");
		Customer stranger = newCustomer("DKI Jakarta");
		Item item = createProduct("Private Item", 50000, 5, 200, "ACTIVE");
		addToCart(owner, item.variantId(), 1);
		String orderNumber = JsonPath.read(body(checkout(owner, "jne", "reg").andExpect(status().isCreated())),
				"$.data.orderNumber");

		mockMvc.perform(get("/api/v1/orders/" + orderNumber).header("Authorization", bearer(stranger.token())))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/cancel").header("Authorization", bearer(stranger.token())))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/orders").header("Authorization", bearer(stranger.token())))
				.andExpect(jsonPath("$.data.totalElements").value(0));
		mockMvc.perform(get("/api/v1/orders").header("Authorization", bearer(owner.token())))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].orderNumber").value(orderNumber))
				.andExpect(jsonPath("$.data.content[0].itemCount").value(1));
	}

	@Test
	void concurrentCheckoutsNeverSellMoreThanTheStock() throws Exception {
		int stock = 3;
		int buyers = 8;
		Item item = createProduct("Last Units", 75000, stock, 300, "ACTIVE");

		List<Customer> customers = new ArrayList<>();
		for (int i = 0; i < buyers; i++) {
			Customer customer = newCustomer("DKI Jakarta");
			addToCart(customer, item.variantId(), 1).andExpect(status().isCreated());
			customers.add(customer);
		}

		ExecutorService pool = Executors.newFixedThreadPool(buyers);
		try {
			List<Callable<Integer>> tasks = customers.stream()
					.<Callable<Integer>>map(customer -> () -> checkout(customer, "jne", "reg")
							.andReturn().getResponse().getStatus())
					.toList();
			List<Integer> statuses = new ArrayList<>();
			for (Future<Integer> future : pool.invokeAll(tasks)) {
				statuses.add(future.get());
			}

			assertThat(statuses.stream().filter(code -> code == 201).count()).isEqualTo(stock);
			assertThat(statuses.stream().filter(code -> code == 409).count()).isEqualTo(buyers - stock);
		} finally {
			pool.shutdownNow();
		}

		assertThat(availableOf(item)).isZero();
		String variant = body(mockMvc.perform(get("/api/v1/admin/products/" + item.productId())
				.header("Authorization", bearer(adminToken))));
		assertThat((Integer) JsonPath.read(variant, "$.data.variants[0].reserved")).isEqualTo(stock);
		assertThat((Integer) JsonPath.read(variant, "$.data.variants[0].quantity")).isEqualTo(stock);
	}

	@Test
	void stockBelowReservationsCannotBeSetByAdmin() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item item = createProduct("Reserved Item", 50000, 5, 200, "ACTIVE");
		addToCart(buyer, item.variantId(), 4);
		checkout(buyer, "jne", "reg").andExpect(status().isCreated());

		mockMvc.perform(put("/api/v1/admin/inventory/" + item.variantId())
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"quantity\":2}"))
				.andExpect(status().isConflict());
		setStock(item.variantId(), 4);
	}

}

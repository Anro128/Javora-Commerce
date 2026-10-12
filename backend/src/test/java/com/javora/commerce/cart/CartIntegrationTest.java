package com.javora.commerce.cart;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javora.commerce.ShopTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CartIntegrationTest extends ShopTestSupport {

	@Test
	void cartRequiresLogin() throws Exception {
		mockMvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
		mockMvc.perform(json(post("/api/v1/cart/items"), "{\"variantId\":1,\"quantity\":1}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void addingTheSameItemTwiceIncreasesTheQuantity() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item item = createProduct("Cart Mug", 45000, 10, 400, "ACTIVE");

		addToCart(buyer, item.variantId(), 2).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].quantity").value(2))
				.andExpect(jsonPath("$.data.items[0].unitPrice").value(45000))
				.andExpect(jsonPath("$.data.items[0].lineTotal").value(90000));
		addToCart(buyer, item.variantId(), 3).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.items[0].quantity").value(5))
				.andExpect(jsonPath("$.data.subtotal").value(225000))
				.andExpect(jsonPath("$.data.totalWeightGram").value(2000))
				.andExpect(jsonPath("$.data.itemCount").value(5))
				.andExpect(jsonPath("$.data.hasIssues").value(false));
	}

	@Test
	void quantityCannotExceedAvailableStock() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item item = createProduct("Scarce Lamp", 100000, 3, 500, "ACTIVE");

		addToCart(buyer, item.variantId(), 4).andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Only 3 available"));
		addToCart(buyer, item.variantId(), 3).andExpect(status().isCreated());
		addToCart(buyer, item.variantId(), 1).andExpect(status().isConflict());

		setStock(item.variantId(), 0);
		addToCart(newCustomer("DKI Jakarta"), item.variantId(), 1).andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("This item is out of stock"));
	}

	@Test
	void inputIsValidatedAndDraftProductsCannotBeBought() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item draft = createProduct("Draft Chair", 100000, 5, 500, "DRAFT");

		addToCart(buyer, draft.variantId(), 1).andExpect(status().isConflict());
		addToCart(buyer, draft.variantId(), 0).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.quantity").exists());
		addToCart(buyer, 999999999L, 1).andExpect(status().isNotFound());
	}

	@Test
	void quantitiesCanBeChangedAndItemsRemoved() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item first = createProduct("Item One", 10000, 10, 100, "ACTIVE");
		Item second = createProduct("Item Two", 20000, 10, 100, "ACTIVE");
		addToCart(buyer, first.variantId(), 1);
		addToCart(buyer, second.variantId(), 1);

		mockMvc.perform(json(put("/api/v1/cart/items/" + first.variantId()).header("Authorization", bearer(buyer.token())),
						"{\"quantity\":4}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].quantity").value(4))
				.andExpect(jsonPath("$.data.subtotal").value(60000));

		mockMvc.perform(delete("/api/v1/cart/items/" + second.variantId()).header("Authorization", bearer(buyer.token())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1));

		mockMvc.perform(json(put("/api/v1/cart/items/" + second.variantId()).header("Authorization", bearer(buyer.token())),
						"{\"quantity\":1}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete("/api/v1/cart").header("Authorization", bearer(buyer.token())))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/cart").header("Authorization", bearer(buyer.token())))
				.andExpect(jsonPath("$.data.items.length()").value(0))
				.andExpect(jsonPath("$.data.subtotal").value(0));
	}

	@Test
	void customersNeverSeeEachOthersCarts() throws Exception {
		Customer alice = newCustomer("DKI Jakarta");
		Customer bob = newCustomer("DKI Jakarta");
		Item item = createProduct("Shared Pen", 5000, 10, 50, "ACTIVE");
		addToCart(alice, item.variantId(), 2);

		mockMvc.perform(get("/api/v1/cart").header("Authorization", bearer(bob.token())))
				.andExpect(jsonPath("$.data.items.length()").value(0));
	}

	@Test
	void stockProblemsAreFlaggedOnTheCartLine() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item item = createProduct("Fading Item", 10000, 5, 100, "ACTIVE");
		addToCart(buyer, item.variantId(), 4);

		setStock(item.variantId(), 2);
		mockMvc.perform(get("/api/v1/cart").header("Authorization", bearer(buyer.token())))
				.andExpect(jsonPath("$.data.items[0].issue").value("LOW_STOCK"))
				.andExpect(jsonPath("$.data.items[0].available").value(2))
				.andExpect(jsonPath("$.data.hasIssues").value(true));

		setStock(item.variantId(), 0);
		mockMvc.perform(get("/api/v1/cart").header("Authorization", bearer(buyer.token())))
				.andExpect(jsonPath("$.data.items[0].issue").value("OUT_OF_STOCK"));
	}

	@Test
	void guestCartIsMergedWithoutFailingOnProblemItems() throws Exception {
		Customer buyer = newCustomer("DKI Jakarta");
		Item plenty = createProduct("Merge Plenty", 10000, 50, 100, "ACTIVE");
		Item limited = createProduct("Merge Limited", 10000, 2, 100, "ACTIVE");
		Item draft = createProduct("Merge Draft", 10000, 5, 100, "DRAFT");
		addToCart(buyer, plenty.variantId(), 1);

		String guestItems = """
				{"items":[{"variantId":%d,"quantity":2},{"variantId":%d,"quantity":9},
				          {"variantId":%d,"quantity":1},{"variantId":999999999,"quantity":1}]}"""
				.formatted(plenty.variantId(), limited.variantId(), draft.variantId());

		mockMvc.perform(json(post("/api/v1/cart/merge").header("Authorization", bearer(buyer.token())), guestItems))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].quantity").value(3))
				.andExpect(jsonPath("$.data.items[1].quantity").value(2));
	}

	@Test
	void guestPreviewShowsCurrentPricesWithoutLogin() throws Exception {
		Item item = createProduct("Guest Item", 25000, 5, 300, "ACTIVE");

		mockMvc.perform(post("/api/v1/public/cart/preview")
						.header("Authorization", "Bearer stale.token.value")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[{\"variantId\":%d,\"quantity\":2},{\"variantId\":999999999,\"quantity\":1}]}"
								.formatted(item.variantId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].productName").value("Guest Item"))
				.andExpect(jsonPath("$.data.subtotal").value(50000));
	}

}

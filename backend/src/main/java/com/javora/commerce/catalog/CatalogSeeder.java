package com.javora.commerce.catalog;

import com.javora.commerce.catalog.dto.CategoryRequest;
import com.javora.commerce.catalog.dto.ProductCreateRequest;
import com.javora.commerce.catalog.dto.VariantCreateRequest;
import com.javora.commerce.catalog.entity.ProductStatus;
import com.javora.commerce.catalog.repository.ProductRepository;
import com.javora.commerce.catalog.service.CategoryService;
import com.javora.commerce.catalog.service.ProductAdminService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Fills an empty development database with a small demo catalog so the shop is not blank.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.catalog.seed-demo-data", havingValue = "true")
public class CatalogSeeder implements ApplicationRunner {

	private final ProductRepository productRepository;
	private final CategoryService categoryService;
	private final ProductAdminService productAdminService;

	@Override
	public void run(ApplicationArguments args) {
		if (productRepository.count() > 0) {
			return;
		}

		Long fashion = category("Fashion", null, 1);
		Long shoes = category("Shoes", fashion, 1);
		Long tshirts = category("T-Shirts", fashion, 2);
		Long electronics = category("Electronics", null, 2);
		Long audio = category("Audio", electronics, 1);
		Long home = category("Home & Living", null, 3);

		product(shoes, "Urban Runner Sneakers", "Lightweight everyday sneakers with a breathable mesh upper.", 549000, 800,
				variant("URB-RUN-40", "Size 40", 549000, 12), variant("URB-RUN-41", "Size 41", 549000, 8),
				variant("URB-RUN-42", "Size 42", 549000, 3));
		product(shoes, "Classic Leather Loafers", "Hand finished leather loafers for the office and beyond.", 899000, 950,
				variant("CLS-LOA-41", "Size 41", 899000, 5), variant("CLS-LOA-42", "Size 42", 899000, 0));
		product(tshirts, "Essential Cotton Tee", "Soft 100% combed cotton t-shirt with a relaxed fit.", 129000, 220,
				variant("ESS-TEE-M", "Medium", 129000, 40), variant("ESS-TEE-L", "Large", 129000, 25),
				variant("ESS-TEE-XL", "X-Large", 135000, 10));
		product(tshirts, "Graphic Print Tee", "Limited run graphic tee printed on heavyweight cotton.", 159000, 240,
				variant("GRA-TEE-M", "Medium", 159000, 15), variant("GRA-TEE-L", "Large", 159000, 2));
		product(audio, "Wireless Over-Ear Headphones", "Bluetooth 5.3 headphones with 40 hours of battery life.", 1299000,
				450, variant("WIR-HDP-BLK", "Black", 1299000, 20), variant("WIR-HDP-SLV", "Silver", 1299000, 6));
		product(audio, "Portable Bluetooth Speaker", "Water resistant speaker with deep bass and 12 hour playtime.", 499000,
				600, variant("PRT-SPK-STD", "Standard", 499000, 30));
		product(home, "Ceramic Pour Over Set", "Dripper, carafe, and two cups in a matte ceramic finish.", 349000, 1200,
				variant("CER-POS-WHT", "White", 349000, 9), variant("CER-POS-GRY", "Grey", 349000, 4));
		product(home, "Scented Soy Candle", "Hand poured soy wax candle with a 45 hour burn time.", 89000, 300,
				variant("SOY-CND-LAV", "Lavender", 89000, 50), variant("SOY-CND-VAN", "Vanilla", 89000, 35));

		log.info("Demo catalog created with {} products", productRepository.count());
	}

	private Long category(String name, Long parentId, int sortOrder) {
		return categoryService.create(new CategoryRequest(name, parentId, null, sortOrder, true)).id();
	}

	private void product(Long categoryId, String name, String description, int price, int weightGram,
			VariantCreateRequest... variants) {
		productAdminService.create(new ProductCreateRequest(
				categoryId, name, description, BigDecimal.valueOf(price), weightGram, ProductStatus.ACTIVE,
				List.of(variants)));
	}

	private static VariantCreateRequest variant(String sku, String name, int price, int stock) {
		return new VariantCreateRequest(sku, name, BigDecimal.valueOf(price), stock);
	}

}

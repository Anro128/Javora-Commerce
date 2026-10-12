package com.javora.commerce.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import com.javora.commerce.user.entity.Address;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZoneBasedShippingProviderTest {

	private final ZoneBasedShippingProvider provider = new ZoneBasedShippingProvider("DKI Jakarta");

	private static Address inProvince(String province) {
		Address address = new Address();
		address.setProvince(province);
		return address;
	}

	private static BigDecimal cost(List<ShippingOption> options, String courier, String service) {
		return options.stream()
				.filter(option -> option.courier().equals(courier) && option.service().equals(service))
				.findFirst().orElseThrow().cost();
	}

	@Test
	void zoneIsChosenFromTheDestinationProvince() {
		assertThat(cost(provider.quote(inProvince("dki jakarta"), 500), "jne", "reg")).isEqualByComparingTo("9000");
		assertThat(cost(provider.quote(inProvince("Jawa Barat"), 500), "jne", "reg")).isEqualByComparingTo("12000");
		assertThat(cost(provider.quote(inProvince("Papua"), 500), "jne", "reg")).isEqualByComparingTo("22000");
	}

	@Test
	void weightIsBilledPerStartedKilogram() {
		Address jakarta = inProvince("DKI Jakarta");

		assertThat(cost(provider.quote(jakarta, 1), "jne", "reg")).isEqualByComparingTo("9000");
		assertThat(cost(provider.quote(jakarta, 1000), "jne", "reg")).isEqualByComparingTo("9000");
		assertThat(cost(provider.quote(jakarta, 1001), "jne", "reg")).isEqualByComparingTo("12000");
		assertThat(cost(provider.quote(jakarta, 0), "jne", "reg")).isEqualByComparingTo("9000");
	}

	@Test
	void optionsAreSortedFromCheapestToMostExpensive() {
		List<ShippingOption> options = provider.quote(inProvince("DKI Jakarta"), 1500);

		assertThat(options).hasSize(4);
		assertThat(options).isSortedAccordingTo((first, second) -> first.cost().compareTo(second.cost()));
		assertThat(options.get(options.size() - 1).service()).isEqualTo("yes");
	}

}

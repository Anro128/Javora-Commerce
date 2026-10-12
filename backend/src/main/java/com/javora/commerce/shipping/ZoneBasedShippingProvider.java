package com.javora.commerce.shipping;

import com.javora.commerce.user.entity.Address;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Deterministic tariff table by destination zone and weight, used until a live courier API is connected.
@Component
public class ZoneBasedShippingProvider implements ShippingRateProvider {

	private static final Set<String> JAVA_PROVINCES = Set.of(
			"dki jakarta", "jawa barat", "jawa tengah", "di yogyakarta", "daerah istimewa yogyakarta",
			"jawa timur", "banten");

	private record Service(
			String courier, String courierName, String service, String serviceName,
			int[] base, int[] perExtraKg, int etdMin, int etdMax) {
	}

	private static final List<Service> SERVICES = List.of(
			new Service("jne", "JNE", "reg", "Regular", new int[] {9000, 12000, 22000}, new int[] {3000, 5000, 9000}, 2, 3),
			new Service("jne", "JNE", "yes", "Next day", new int[] {18000, 24000, 44000}, new int[] {6000, 10000, 18000}, 1, 1),
			new Service("jnt", "J&T Express", "ez", "EZ", new int[] {8000, 11000, 20000}, new int[] {3000, 4500, 8500}, 2, 4),
			new Service("sicepat", "SiCepat", "reg", "Regular", new int[] {9000, 11500, 21000}, new int[] {3000, 4800, 8800}, 2, 3));

	private final String originProvince;

	public ZoneBasedShippingProvider(@Value("${app.shipping.origin-province:DKI Jakarta}") String originProvince) {
		this.originProvince = normalize(originProvince);
	}

	@Override
	public List<ShippingOption> quote(Address destination, int totalWeightGram) {
		int zone = zoneFor(destination.getProvince());
		int billableKg = Math.max(1, (int) Math.ceil(totalWeightGram / 1000.0));

		return SERVICES.stream()
				.map(service -> new ShippingOption(
						service.courier(),
						service.courierName(),
						service.service(),
						service.serviceName(),
						BigDecimal.valueOf((long) service.base()[zone] + (long) (billableKg - 1) * service.perExtraKg()[zone]),
						service.etdMin(),
						service.etdMax()))
				.sorted((first, second) -> first.cost().compareTo(second.cost()))
				.toList();
	}

	// Zone 0 is the origin province, 1 the rest of Java, and 2 everywhere else.
	private int zoneFor(String province) {
		String normalized = normalize(province);
		if (normalized.equals(originProvince)) {
			return 0;
		}
		return JAVA_PROVINCES.contains(normalized) ? 1 : 2;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

}

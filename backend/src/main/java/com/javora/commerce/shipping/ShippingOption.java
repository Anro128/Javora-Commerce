package com.javora.commerce.shipping;

import java.math.BigDecimal;

public record ShippingOption(
		String courier,
		String courierName,
		String service,
		String serviceName,
		BigDecimal cost,
		int etdMinDays,
		int etdMaxDays) {
}

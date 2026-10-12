package com.javora.commerce.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
		List<CartLine> items,
		BigDecimal subtotal,
		int totalWeightGram,
		int itemCount,
		boolean hasIssues) {

	public static CartResponse of(List<CartLine> lines) {
		BigDecimal subtotal = lines.stream()
				.filter(CartLine::purchasable)
				.map(CartLine::lineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		int weight = lines.stream().filter(CartLine::purchasable).mapToInt(line -> line.weightGram() * line.quantity()).sum();
		int count = lines.stream().mapToInt(CartLine::quantity).sum();
		boolean issues = lines.stream().anyMatch(CartLine::hasIssue);
		return new CartResponse(lines, subtotal, weight, count, issues);
	}

}

package com.javora.commerce.order;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

final class OrderNumbers {

	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private OrderNumbers() {
	}

	// Format JVR-20261012-K7M2QX; the alphabet leaves out look-alike characters.
	static String next() {
		StringBuilder suffix = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			suffix.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return "JVR-%s-%s".formatted(LocalDate.now(ZoneOffset.UTC).format(DATE), suffix);
	}

}

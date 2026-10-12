package com.javora.commerce.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class SlugUtil {

	private SlugUtil() {
	}

	public static String slugify(String value) {
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		String slug = normalized.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-+|-+$", "");
		return slug.isEmpty() ? "item" : slug;
	}

	// Appends -2, -3, ... until the slug is not taken.
	public static String uniqueSlug(String value, int maxLength, Predicate<String> exists) {
		String base = slugify(value);
		if (base.length() > maxLength - 6) {
			base = base.substring(0, maxLength - 6).replaceAll("-+$", "");
		}
		String candidate = base;
		int suffix = 2;
		while (exists.test(candidate)) {
			candidate = base + "-" + suffix++;
		}
		return candidate;
	}

}

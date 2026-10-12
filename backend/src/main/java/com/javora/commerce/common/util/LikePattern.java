package com.javora.commerce.common.util;

import java.util.Locale;

public final class LikePattern {

	public static final char ESCAPE = '\\';

	private LikePattern() {
	}

	// Builds a lower-cased contains pattern with the user supplied wildcards escaped.
	public static String contains(String text) {
		if (text == null || text.isBlank()) {
			return "%";
		}
		String escaped = text.trim().toLowerCase(Locale.ROOT)
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		return "%" + escaped + "%";
	}

}

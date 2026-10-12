package com.javora.commerce.common.util;

import java.util.Optional;

public final class ImageSniffer {

	public record ImageType(String contentType, String extension) {
	}

	private ImageSniffer() {
	}

	// Detects the real format from the file signature instead of trusting the client supplied type.
	public static Optional<ImageType> detect(byte[] bytes) {
		if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
			return Optional.of(new ImageType("image/jpeg", "jpg"));
		}
		if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
			return Optional.of(new ImageType("image/png", "png"));
		}
		if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
				&& bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
			return Optional.of(new ImageType("image/webp", "webp"));
		}
		return Optional.empty();
	}

}

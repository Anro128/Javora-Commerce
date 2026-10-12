package com.javora.commerce.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class UtilTest {

	@Test
	void slugifyRemovesAccentsSymbolsAndEdgeDashes() {
		assertThat(SlugUtil.slugify("  Café Crème & Co.  ")).isEqualTo("cafe-creme-co");
		assertThat(SlugUtil.slugify("!!!")).isEqualTo("item");
	}

	@Test
	void uniqueSlugAppendsCounterUntilFree() {
		Set<String> taken = Set.of("red-shoes", "red-shoes-2");

		assertThat(SlugUtil.uniqueSlug("Red Shoes", 100, taken::contains)).isEqualTo("red-shoes-3");
		assertThat(SlugUtil.uniqueSlug("Blue Shoes", 100, taken::contains)).isEqualTo("blue-shoes");
	}

	@Test
	void uniqueSlugRespectsMaximumLength() {
		String slug = SlugUtil.uniqueSlug("a".repeat(300), 40, candidate -> false);

		assertThat(slug.length()).isLessThanOrEqualTo(40);
	}

	@Test
	void likePatternEscapesWildcards() {
		assertThat(LikePattern.contains("50%_off")).isEqualTo("%50\\%\\_off%");
		assertThat(LikePattern.contains("  Shoe ")).isEqualTo("%shoe%");
		assertThat(LikePattern.contains(null)).isEqualTo("%");
		assertThat(LikePattern.contains("   ")).isEqualTo("%");
	}

	@Test
	void imageSnifferTrustsSignatureNotFileName() {
		byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
		byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
		byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
		byte[] text = "<?php echo 1; ?>".getBytes();

		assertThat(ImageSniffer.detect(jpeg)).hasValueSatisfying(type -> assertThat(type.extension()).isEqualTo("jpg"));
		assertThat(ImageSniffer.detect(png)).hasValueSatisfying(type -> assertThat(type.contentType()).isEqualTo("image/png"));
		assertThat(ImageSniffer.detect(webp)).hasValueSatisfying(type -> assertThat(type.extension()).isEqualTo("webp"));
		assertThat(ImageSniffer.detect(text)).isEmpty();
		assertThat(ImageSniffer.detect(new byte[0])).isEmpty();
	}

}

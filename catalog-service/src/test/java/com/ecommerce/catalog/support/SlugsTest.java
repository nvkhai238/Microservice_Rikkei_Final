package com.ecommerce.catalog.support;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SlugsTest {

    @Test
    void vietnameseNamesBecomeAsciiSlugs() {
        assertThat(Slugs.slugify("Áo Thun Nam Đỏ - Size XL!")).isEqualTo("ao-thun-nam-do-size-xl");
        assertThat(Slugs.slugify("   ")).isEqualTo("item");
    }

    @Test
    void uniqueAppendsCounter() {
        Set<String> taken = Set.of("ao", "ao-2");
        assertThat(Slugs.unique("ao", taken::contains)).isEqualTo("ao-3");
    }

    @Test
    void validatesSlugFormat() {
        assertThat(Slugs.isValid("ao-thun")).isTrue();
        assertThat(Slugs.isValid("Ao Thun")).isFalse();
        assertThat(Slugs.isValid("../etc")).isFalse();
    }
}

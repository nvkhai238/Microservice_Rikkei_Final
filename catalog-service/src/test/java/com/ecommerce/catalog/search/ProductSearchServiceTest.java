package com.ecommerce.catalog.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSearchServiceTest {

    @Test
    void buildsPrefixQueryFromWords() {
        assertThat(ProductSearchService.toPrefixTsQuery("Áo thun  ĐỎ")).isEqualTo("áo:* & thun:* & đỏ:*");
        assertThat(ProductSearchService.toPrefixTsQuery("CM-TEE")).isEqualTo("cm:* & tee:*");
    }

    @Test
    void stripsTsQueryOperatorsSoUsersCannotInjectThem() {
        assertThat(ProductSearchService.toPrefixTsQuery("a & !b | c:* <-> d"))
                .isEqualTo("a:* & b:* & c:* & d:*");
        assertThat(ProductSearchService.toPrefixTsQuery("'; DROP TABLE products; --"))
                .isEqualTo("drop:* & table:* & products:*");
        assertThat(ProductSearchService.toPrefixTsQuery("  ")).isNull();
    }
}

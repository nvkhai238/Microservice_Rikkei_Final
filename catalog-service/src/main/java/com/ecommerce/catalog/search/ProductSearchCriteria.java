package com.ecommerce.catalog.search;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Tham số tìm kiếm (FR-CAT-014..016). sort: relevance | price_asc | price_desc | newest | updated */
public record ProductSearchCriteria(String q, UUID categoryId, UUID brandId, BigDecimal minPrice,
                                   BigDecimal maxPrice, Boolean inStock, Map<String, String> attributes,
                                   String sort, int page, int size) {
}

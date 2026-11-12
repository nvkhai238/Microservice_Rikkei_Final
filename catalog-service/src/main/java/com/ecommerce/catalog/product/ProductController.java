package com.ecommerce.catalog.product;

import com.ecommerce.catalog.search.ProductSearchCriteria;
import com.ecommerce.catalog.search.ProductSearchService;
import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.response.PageRequests;
import com.ecommerce.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** API công khai cho khách xem/tìm sản phẩm (UC-02). */
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductSearchService searchService;
    private final ProductQueryService queryService;

    /**
     * Ví dụ: GET /api/v1/products?q=ao thun&categoryId=...&minPrice=100000&maxPrice=500000&inStock=true
     * &attr.color=Đỏ&attr.size=M&sort=price_asc&page=0&size=20
     */
    @GetMapping("/api/v1/products")
    public ApiResponse<PageResponse<ProductDtos.ProductSummary>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam Map<String, String> allParams) {
        PageRequest pageRequest = PageRequests.of(page, size, org.springframework.data.domain.Sort.unsorted());
        Map<String, String> attributes = new HashMap<>();
        allParams.forEach((k, v) -> {
            if (k.startsWith("attr.") && k.length() > 5 && !v.isBlank()) {
                attributes.put(k.substring(5).toLowerCase(), v.trim());
            }
        });
        String query = q != null && q.length() > 200 ? q.substring(0, 200) : q;
        return ApiResponse.success(searchService.search(new ProductSearchCriteria(query, categoryId, brandId,
                minPrice, maxPrice, inStock, attributes, sort, pageRequest.getPageNumber(), pageRequest.getPageSize())));
    }

    @GetMapping("/api/v1/products/{idOrSlug}")
    public ApiResponse<ProductDtos.ProductDetail> detail(@PathVariable String idOrSlug) {
        return ApiResponse.success(queryService.detail(idOrSlug));
    }
}

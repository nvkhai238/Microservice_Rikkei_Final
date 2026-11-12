package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Catalog nghe event này để cập nhật chỉ báo "còn hàng" (FR-CAT-015/017) mà không phải gọi order. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockLevelChangedPayload(UUID skuId, long available, long version) {
}

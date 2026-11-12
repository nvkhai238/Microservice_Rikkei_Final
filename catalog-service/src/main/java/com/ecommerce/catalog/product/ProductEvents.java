package com.ecommerce.catalog.product;

import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.payload.ProductChangedPayload;
import com.ecommerce.common.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Sau mỗi thay đổi: tính lại chỉ mục tìm kiếm + phát ProductChanged qua Outbox (FR-CAT-012).
 * Consumer (cache, search engine ngoài nếu có) dùng "version" để bỏ qua event cũ đến muộn.
 */
@Component
@RequiredArgsConstructor
public class ProductEvents {

    private final ProductRepository products;
    private final SearchIndexer searchIndexer;
    private final EventPublisher events;

    public void changed(Product product, String changeType) {
        products.flush();   // đẩy UPDATE xuống DB để version tăng và search_vector đọc được dữ liệu mới
        searchIndexer.refresh(product.getId());
        events.publish("Product", product.getId(), EventTypes.PRODUCT_CHANGED,
                new ProductChangedPayload(product.getId(), product.getVersion(), changeType));
    }
}

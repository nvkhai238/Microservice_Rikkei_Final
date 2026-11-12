package com.ecommerce.catalog.config;

import com.ecommerce.common.messaging.EventTopology;

/**
 * Tên queue service này nghe. Topology (exchange, queue, binding, DLQ) được khai báo tập trung ở
 * {@link EventTopology} trong common.
 */
public final class MessagingConfig {
    public static final String INVENTORY_QUEUE = EventTopology.CATALOG_INVENTORY_QUEUE;

    private MessagingConfig() {
    }
}

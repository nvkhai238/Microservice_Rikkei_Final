package com.ecommerce.order.config;

import com.ecommerce.common.messaging.EventTopology;

/**
 * Tên queue service này nghe. Topology (exchange, queue, binding, DLQ) được khai báo tập trung ở
 * {@link EventTopology} trong common.
 */
public final class MessagingConfig {
    public static final String PAYMENT_EVENTS_QUEUE = EventTopology.ORDER_PAYMENT_QUEUE;

    private MessagingConfig() {
    }
}

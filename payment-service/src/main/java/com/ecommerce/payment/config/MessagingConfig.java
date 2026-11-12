package com.ecommerce.payment.config;

import com.ecommerce.common.messaging.EventTopology;

/**
 * Tên queue service này nghe. Topology (exchange, queue, binding, DLQ) được khai báo tập trung ở
 * {@link EventTopology} trong common.
 */
public final class MessagingConfig {
    public static final String ORDER_EVENTS_QUEUE = EventTopology.PAYMENT_ORDER_QUEUE;

    private MessagingConfig() {
    }
}

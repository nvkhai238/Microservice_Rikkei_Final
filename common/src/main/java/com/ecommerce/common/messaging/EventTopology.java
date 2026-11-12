package com.ecommerce.common.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;

import com.ecommerce.common.event.EventTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Topology RabbitMQ dùng chung:
 *  producer --(routing key = eventType)--> [ecommerce.events] (topic)
 *        --binding--> queue của từng consumer (vd: payment.order-events)
 *              |  xử lý lỗi quá số lần retry
 *              v
 *        [ecommerce.events.dlx] (direct) --> queue.dlq  (Dead Letter Queue, chờ người xử lý)
 * Mỗi consumer có queue RIÊNG => cùng một event OrderCreated, payment và notification đều nhận được bản của mình.
 * Exchange/queue đều durable + message persistent => broker restart không mất event.
 * <p><b>Vì sao MỌI service khai báo TOÀN BỘ topology?</b> RabbitMQ bỏ (drop) message nếu lúc publish chưa có
 * queue nào bind. Nếu order chạy trước payment lần đầu, OrderCreated sẽ mất. Khai báo hết ở mọi service =>
 * service nào lên trước cũng tạo đủ queue; queue durable nên sau đó tồn tại mãi.
 */
public final class EventTopology {
    public static final String EXCHANGE = "ecommerce.events";
    public static final String DEAD_LETTER_EXCHANGE = "ecommerce.events.dlx";
    public static final String DLQ_SUFFIX = ".dlq";

    // ---- Queue của từng consumer: tên + các eventType nó nghe (hợp đồng, xem docs/EVENTS.md) ----
    public static final String CATALOG_INVENTORY_QUEUE = "catalog.inventory-events";
    public static final String ORDER_PAYMENT_QUEUE = "order.payment-events";
    public static final String PAYMENT_ORDER_QUEUE = "payment.order-events";
    public static final String NOTIFICATION_QUEUE = "notification.events";

    public static Map<String, List<String>> bindings() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(CATALOG_INVENTORY_QUEUE, List.of(EventTypes.STOCK_LEVEL_CHANGED));
        m.put(ORDER_PAYMENT_QUEUE, List.of(EventTypes.PAYMENT_SUCCEEDED, EventTypes.PAYMENT_FAILED,
                EventTypes.REFUND_COMPLETED, EventTypes.REFUND_FAILED));
        m.put(PAYMENT_ORDER_QUEUE, List.of(EventTypes.ORDER_CREATED, EventTypes.ORDER_CANCELLED,
                EventTypes.REFUND_REQUESTED));
        m.put(NOTIFICATION_QUEUE, List.of(EventTypes.USER_REGISTERED, EventTypes.EMAIL_VERIFICATION_REQUESTED,
                EventTypes.PASSWORD_RESET_REQUESTED, EventTypes.ORDER_CREATED, EventTypes.ORDER_CONFIRMED,
                EventTypes.ORDER_STATUS_CHANGED, EventTypes.ORDER_CANCELLED, EventTypes.PAYMENT_FAILED,
                EventTypes.REFUND_COMPLETED));
        return m;
    }

    /** Toàn bộ topology: exchange, DLX, mọi queue + DLQ + binding. */
    public static Declarables all() {
        List<Declarable> all = new ArrayList<>();
        bindings().forEach((queue, types) -> all.addAll(queueWithDlq(queue, types.toArray(String[]::new))
                .getDeclarables()));
        return new Declarables(all);
    }

    private EventTopology() {
    }

    /** Khai báo một queue có DLQ, bind với các eventType cần nghe. */
    public static Declarables queueWithDlq(String queueName, String... eventTypes) {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(queueName)
                .build();
        Queue dlq = QueueBuilder.durable(queueName + DLQ_SUFFIX).build();

        List<Declarable> all = new ArrayList<>(List.of(exchange, dlx, queue, dlq,
                BindingBuilder.bind(dlq).to(dlx).with(queueName)));
        for (String type : eventTypes) {
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(type);
            all.add(binding);
        }
        return new Declarables(all);
    }

}

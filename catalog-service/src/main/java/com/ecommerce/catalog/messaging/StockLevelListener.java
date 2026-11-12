package com.ecommerce.catalog.messaging;

import com.ecommerce.catalog.config.MessagingConfig;
import com.ecommerce.catalog.sku.SkuRepository;
import com.ecommerce.common.event.EventEnvelope;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.payload.StockLevelChangedPayload;
import com.ecommerce.common.messaging.EventMessages;
import com.ecommerce.common.web.CorrelationId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Cập nhật read model "còn hàng" của catalog từ event của order-service.
 * Idempotent tự nhiên: cập nhật có điều kiện theo version => nhận trùng hay sai thứ tự đều an toàn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockLevelListener {

    private final EventMessages eventMessages;
    private final SkuRepository skus;
    private final TransactionTemplate tx;

    @RabbitListener(queues = MessagingConfig.INVENTORY_QUEUE)
    public void onMessage(Message message) {
        EventEnvelope<JsonNode> envelope = eventMessages.parse(message);
        if (!EventTypes.STOCK_LEVEL_CHANGED.equals(envelope.eventType())) {
            return;
        }
        StockLevelChangedPayload payload = eventMessages.payload(envelope, StockLevelChangedPayload.class);
        CorrelationId.runWith(envelope.correlationId(), () -> {
            Integer updated = tx.execute(s -> skus.updateAvailability(payload.skuId(), payload.available(),
                    payload.version()));
            log.debug("Stock level for sku {} -> {} (applied={})", payload.skuId(), payload.available(), updated);
        });
    }
}

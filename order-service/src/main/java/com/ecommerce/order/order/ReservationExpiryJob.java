package com.ecommerce.order.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Hủy đơn PENDING quá hạn giữ kho và trả hàng về kho (FR-INV-011, UC-04).
 * An toàn khi chạy lặp và chạy song song nhiều instance (FR-INV-015):
 * <ul>
 *   <li>FOR UPDATE SKIP LOCKED: mỗi instance khóa một nhóm đơn khác nhau.</li>
 *   <li>expire() kiểm tra lại status == PENDING; release() chỉ trả reservation còn ACTIVE => chạy lại không trả 2 lần.</li>
 *   <li>Restart giữa chừng: transaction chưa commit thì rollback, lần chạy sau làm lại từ đầu (NFR-AVL-006).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {

    private final OrderRepository orders;
    private final OrderLifecycleService lifecycle;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final com.ecommerce.order.config.OrderProperties props;

    @Scheduled(fixedDelayString = "${app.inventory.expiry-job-interval:30s}", initialDelayString = "10s")
    public void expireReservations() {
        int total = 0;
        Integer processed;
        do {
            processed = tx.execute(status -> {
                List<UUID> ids = orders.lockExpiredPending(clock.instant(), props.inventory().expiryBatchSize());
                ids.forEach(lifecycle::expire);
                return ids.size();
            });
            total += processed == null ? 0 : processed;
        } while (processed != null && processed == props.inventory().expiryBatchSize());
        if (total > 0) {
            log.info("Expired {} pending order(s) and released their reservations", total);
        }
    }
}

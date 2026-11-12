package com.ecommerce.order.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test chống oversell trên PostgreSQL THẬT (Docker), không mock (UC-05, FR-INV-006..015).
 * RabbitMQ không cần: event chỉ được ghi vào bảng outbox.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.port=1",                    // không có broker: outbox chỉ ghi DB
        "app.outbox.poll-interval=1h",
        "app.inventory.expiry-job-interval=1h",
        "app.security.jwk-set-uri=http://localhost:1/jwks",
        "spring.datasource.hikari.maximum-pool-size=60"
})
class InventoryConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    InventoryService inventory;
    @Autowired
    StockRepository stocks;
    @Autowired
    TransactionTemplate tx;
    @Autowired
    JdbcTemplate jdbc;

    private UUID sku;

    @BeforeEach
    void setUp() {
        sku = UUID.randomUUID();
    }

    private void receive(UUID skuId, long qty) {
        inventory.receive(skuId, qty, "test");
    }

    private void reserve(UUID orderId, List<InventoryService.Line> lines) {
        tx.executeWithoutResult(s -> inventory.reserve(orderId, lines, Instant.now().plusSeconds(900)));
    }

    @Test
    void flashSale_100ConcurrentBuyers_onlyOneWinsAndStockNeverNegative() throws Exception {
        receive(sku, 1);
        int buyers = 100;
        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    reserve(UUID.randomUUID(), List.of(new InventoryService.Line(sku, 1)));
                    wins.incrementAndGet();
                } catch (InsufficientStockException e) {
                    soldOut.incrementAndGet();   // kết quả "hết hàng" rõ ràng cho người thua
                }
                return null;
            }));
        }
        start.countDown();   // thả 100 luồng cùng lúc
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        StockLevel level = stocks.find(sku, "MAIN").orElseThrow();
        assertThat(wins.get()).isEqualTo(1);
        assertThat(soldOut.get()).isEqualTo(buyers - 1);
        assertThat(level.reserved()).isEqualTo(1);
        assertThat(level.available()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservations WHERE sku_id = ?", Long.class, sku))
                .isEqualTo(1L);
    }

    @Test
    void manyBuyersOnLimitedStock_neverExceedStock() throws Exception {
        receive(sku, 10);
        ExecutorService pool = Executors.newFixedThreadPool(40);
        AtomicInteger units = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            int qty = 1 + (i % 3);
            futures.add(pool.submit(() -> {
                try {
                    reserve(UUID.randomUUID(), List.of(new InventoryService.Line(sku, qty)));
                    units.addAndGet(qty);
                } catch (InsufficientStockException ignored) {
                    // hết hàng
                }
                return null;
            }));
        }
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();
        StockLevel level = stocks.find(sku, "MAIN").orElseThrow();
        assertThat(units.get()).isLessThanOrEqualTo(10);
        assertThat(level.reserved()).isEqualTo(units.get());
        assertThat(level.available()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void reserveIsIdempotentPerOrder() {
        receive(sku, 5);
        UUID orderId = UUID.randomUUID();
        reserve(orderId, List.of(new InventoryService.Line(sku, 2)));
        reserve(orderId, List.of(new InventoryService.Line(sku, 2)));   // gửi lặp
        assertThat(stocks.find(sku, "MAIN").orElseThrow().reserved()).isEqualTo(2);
    }

    @Test
    void multiLineReservationIsAllOrNothing() {
        UUID other = UUID.randomUUID();
        receive(sku, 5);
        receive(other, 1);
        UUID orderId = UUID.randomUUID();
        assertThatThrownBy(() -> reserve(orderId, List.of(new InventoryService.Line(sku, 2),
                new InventoryService.Line(other, 3)))).isInstanceOf(InsufficientStockException.class);
        // Dòng đủ hàng cũng KHÔNG bị giữ
        assertThat(stocks.find(sku, "MAIN").orElseThrow().reserved()).isZero();
        assertThat(stocks.find(other, "MAIN").orElseThrow().reserved()).isZero();
    }

    @Test
    void releaseAndCommitHappenExactlyOnce() {
        receive(sku, 5);
        UUID released = UUID.randomUUID();
        UUID committed = UUID.randomUUID();
        reserve(released, List.of(new InventoryService.Line(sku, 2)));
        reserve(committed, List.of(new InventoryService.Line(sku, 1)));

        tx.executeWithoutResult(s -> inventory.release(released, ReservationStatus.RELEASED, "test"));
        tx.executeWithoutResult(s -> inventory.release(released, ReservationStatus.RELEASED, "test"));   // lặp
        tx.executeWithoutResult(s -> inventory.commit(committed));
        tx.executeWithoutResult(s -> inventory.commit(committed));   // lặp

        StockLevel level = stocks.find(sku, "MAIN").orElseThrow();
        assertThat(level.onHand()).isEqualTo(4);   // trừ 1 do commit
        assertThat(level.reserved()).isZero();

        tx.executeWithoutResult(s -> inventory.restock(committed, "cancel"));
        tx.executeWithoutResult(s -> inventory.restock(committed, "cancel"));   // lặp
        assertThat(stocks.find(sku, "MAIN").orElseThrow().onHand()).isEqualTo(5);
    }

    @Test
    void ledgerIsAppendOnly() {
        receive(sku, 1);
        assertThatThrownBy(() -> jdbc.update("UPDATE stock_movements SET quantity = 999 WHERE sku_id = ?", sku))
                .hasMessageContaining("append-only");
    }
}

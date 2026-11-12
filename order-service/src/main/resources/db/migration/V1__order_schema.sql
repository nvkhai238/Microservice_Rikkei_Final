-- =====================================================================
-- order-service: giỏ hàng, tồn kho + reservation, đơn hàng, Saga
-- =====================================================================

-- ---------- Giỏ hàng (FR-CART-*) ----------
CREATE TABLE carts (
    id           UUID PRIMARY KEY,
    customer_id  UUID        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_carts_customer UNIQUE (customer_id)        -- FR-CART-001: một giỏ cho mọi kênh
);

CREATE TABLE cart_items (
    id                 UUID PRIMARY KEY,
    cart_id            UUID          NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    sku_id             UUID          NOT NULL,                -- tham chiếu "mờ" sang catalog, KHÔNG có FK xuyên DB (DR-002)
    quantity           INT           NOT NULL,
    unit_price_at_add  NUMERIC(19,2) NOT NULL,                -- để báo "giá đã thay đổi" (FR-CART-007)
    added_at           TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_cart_items_sku UNIQUE (cart_id, sku_id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0)
);

-- ---------- Tồn kho (FR-INV-*) ----------
-- available = on_hand - reserved. CHECK là lưới an toàn CUỐI CÙNG: dù code có bug, DB không bao giờ để tồn âm (BR-004)
CREATE TABLE stocks (
    sku_id          UUID        NOT NULL,
    warehouse_code  VARCHAR(32) NOT NULL,
    on_hand         BIGINT      NOT NULL DEFAULT 0,
    reserved        BIGINT      NOT NULL DEFAULT 0,
    version         BIGINT      NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (sku_id, warehouse_code),
    CONSTRAINT ck_stocks_on_hand CHECK (on_hand >= 0),
    CONSTRAINT ck_stocks_reserved CHECK (reserved >= 0),
    CONSTRAINT ck_stocks_available CHECK (reserved <= on_hand)
);

-- Sổ cái biến động tồn: chỉ được THÊM (FR-INV-004, DR-005)
CREATE TABLE stock_movements (
    id              UUID PRIMARY KEY,
    sku_id          UUID         NOT NULL,
    warehouse_code  VARCHAR(32)  NOT NULL,
    movement_type   VARCHAR(24)  NOT NULL,   -- RECEIPT | ADJUSTMENT | RESERVE | RELEASE | SALE | RESTOCK
    quantity        BIGINT       NOT NULL,
    on_hand_delta   BIGINT       NOT NULL,
    reserved_delta  BIGINT       NOT NULL,
    on_hand_after   BIGINT       NOT NULL,
    reserved_after  BIGINT       NOT NULL,
    reference_type  VARCHAR(32),             -- ORDER | MANUAL
    reference_id    VARCHAR(64),
    reason          VARCHAR(500),
    actor           VARCHAR(64)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_stock_movements_sku ON stock_movements (sku_id, created_at DESC);
CREATE INDEX ix_stock_movements_ref ON stock_movements (reference_type, reference_id);
CREATE TRIGGER stock_movements_append_only BEFORE UPDATE OR DELETE ON stock_movements
    FOR EACH ROW EXECUTE FUNCTION forbid_mutation();

-- Giữ chỗ (FR-INV-007). UNIQUE(order_id, sku_id): giữ lặp cho cùng đơn không thể tạo 2 bản ghi (FR-INV-008)
CREATE TABLE reservations (
    id              UUID PRIMARY KEY,
    order_id        UUID        NOT NULL,
    sku_id          UUID        NOT NULL,
    warehouse_code  VARCHAR(32) NOT NULL,
    quantity        INT         NOT NULL,
    status          VARCHAR(16) NOT NULL,     -- ACTIVE | COMMITTED | RELEASED | EXPIRED | RESTOCKED
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_reservations_order_sku UNIQUE (order_id, sku_id),
    CONSTRAINT ck_reservations_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_reservations_active_expiry ON reservations (expires_at) WHERE status = 'ACTIVE';

-- ---------- Đơn hàng (FR-ORD-*) ----------
CREATE TABLE orders (
    id                      UUID PRIMARY KEY,
    order_code              VARCHAR(32)   NOT NULL,   -- FR-ORD-007: không chứa PII
    customer_id             UUID          NOT NULL,
    status                  VARCHAR(16)   NOT NULL,   -- PENDING | CONFIRMED | SHIPPING | COMPLETED | CANCELLED
    payment_status          VARCHAR(24)   NOT NULL,
    payment_method          VARCHAR(32)   NOT NULL,
    payment_id              UUID,
    currency                VARCHAR(3)    NOT NULL,
    subtotal                NUMERIC(19,2) NOT NULL,
    discount_total          NUMERIC(19,2) NOT NULL,
    tax_total               NUMERIC(19,2) NOT NULL,
    shipping_fee            NUMERIC(19,2) NOT NULL,
    grand_total             NUMERIC(19,2) NOT NULL,
    note                    VARCHAR(500),
    -- FR-ORD-005: snapshot địa chỉ + liên hệ tại thời điểm đặt
    ship_recipient_name     VARCHAR(100)  NOT NULL,
    ship_phone              VARCHAR(20)   NOT NULL,
    ship_line1              VARCHAR(200)  NOT NULL,
    ship_ward               VARCHAR(100)  NOT NULL,
    ship_district           VARCHAR(100)  NOT NULL,
    ship_province           VARCHAR(100)  NOT NULL,
    ship_postal_code        VARCHAR(10),
    reservation_expires_at  TIMESTAMPTZ   NOT NULL,
    cancel_requested_at     TIMESTAMPTZ,
    cancel_reason           VARCHAR(500),
    cancelled_by            VARCHAR(64),
    created_at              TIMESTAMPTZ   NOT NULL,
    updated_at              TIMESTAMPTZ   NOT NULL,
    version                 BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_orders_code UNIQUE (order_code),
    CONSTRAINT ck_orders_totals CHECK (grand_total >= 0 AND subtotal >= 0)
);
CREATE INDEX ix_orders_customer ON orders (customer_id, created_at DESC);
CREATE INDEX ix_orders_status ON orders (status, created_at DESC);
CREATE INDEX ix_orders_pending_expiry ON orders (reservation_expires_at) WHERE status = 'PENDING';

-- FR-ORD-004: snapshot dòng hàng. KHÔNG đổi theo catalog sau khi đặt (BR-002, DR-003)
CREATE TABLE order_items (
    id            UUID PRIMARY KEY,
    order_id      UUID          NOT NULL REFERENCES orders (id),
    sku_id        UUID          NOT NULL,
    sku_code      VARCHAR(64)   NOT NULL,
    product_id    UUID          NOT NULL,
    product_name  VARCHAR(200)  NOT NULL,
    variant_name  VARCHAR(200),
    attributes    TEXT          NOT NULL,
    image_url     VARCHAR(300),
    unit_price    NUMERIC(19,2) NOT NULL,
    quantity      INT           NOT NULL,
    discount      NUMERIC(19,2) NOT NULL,
    line_total    NUMERIC(19,2) NOT NULL,
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_order_items_order ON order_items (order_id);

-- FR-ORD-011: lịch sử chuyển trạng thái (chỉ thêm)
CREATE TABLE order_status_history (
    id           UUID PRIMARY KEY,
    order_id     UUID         NOT NULL REFERENCES orders (id),
    from_status  VARCHAR(16),
    to_status    VARCHAR(16)  NOT NULL,
    reason       VARCHAR(500),
    actor        VARCHAR(64)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_order_status_history_order ON order_status_history (order_id, created_at);
CREATE TRIGGER order_status_history_append_only BEFORE UPDATE OR DELETE ON order_status_history
    FOR EACH ROW EXECUTE FUNCTION forbid_mutation();

-- FR-ORD-002: khóa idempotency theo từng khách hàng
CREATE TABLE idempotency_keys (
    customer_id   UUID         NOT NULL,
    idem_key      VARCHAR(100) NOT NULL,
    request_hash  VARCHAR(64)  NOT NULL,
    order_id      UUID         NOT NULL,
    outcome       VARCHAR(32)  NOT NULL,     -- CREATED | REJECTED_INSUFFICIENT_STOCK
    created_at    TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (customer_id, idem_key)
);

-- ---------- Saga (FR-SAGA-001, FR-SAGA-010) ----------
CREATE TABLE saga_instances (
    id              UUID PRIMARY KEY,
    order_id        UUID         NOT NULL,
    saga_type       VARCHAR(32)  NOT NULL,   -- PLACE_ORDER | CANCEL_ORDER
    status          VARCHAR(24)  NOT NULL,   -- STARTED | AWAITING_PAYMENT | AWAITING_REFUND | COMPLETED | COMPENSATED | FAILED
    current_step    VARCHAR(64)  NOT NULL,
    correlation_id  VARCHAR(64),
    last_error      VARCHAR(1000),
    retry_count     INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_saga_order_type UNIQUE (order_id, saga_type)
);
CREATE INDEX ix_saga_status ON saga_instances (status, updated_at DESC);

CREATE TABLE saga_steps (
    id          UUID PRIMARY KEY,
    saga_id     UUID          NOT NULL REFERENCES saga_instances (id),
    step        VARCHAR(64)   NOT NULL,
    status      VARCHAR(16)   NOT NULL,     -- DONE | FAILED | COMPENSATED | INFO
    detail      VARCHAR(1000),
    actor       VARCHAR(64)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_saga_steps_saga ON saga_steps (saga_id, created_at);
CREATE TRIGGER saga_steps_append_only BEFORE UPDATE OR DELETE ON saga_steps
    FOR EACH ROW EXECUTE FUNCTION forbid_mutation();

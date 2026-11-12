-- =====================================================================
-- payment-service: payment intent, giao dịch, hoàn tiền, webhook, đối soát
-- =====================================================================

-- FR-PAY-001: một payment intent gắn với đơn, số tiền, tiền tệ, provider, trạng thái
CREATE TABLE payments (
    id               UUID PRIMARY KEY,
    order_id         UUID          NOT NULL,
    order_code       VARCHAR(32)   NOT NULL,
    customer_id      UUID          NOT NULL,
    amount           NUMERIC(19,2) NOT NULL,     -- lấy từ snapshot tổng tiền của đơn (FR-PAY-002)
    currency         VARCHAR(3)    NOT NULL,
    provider         VARCHAR(32)   NOT NULL,
    status           VARCHAR(24)   NOT NULL,     -- FR-PAY-006
    provider_ref     VARCHAR(100),               -- FR-PAY-005: mã tham chiếu phía provider, KHÔNG lưu dữ liệu thẻ
    checkout_url     VARCHAR(500),
    expires_at       TIMESTAMPTZ   NOT NULL,
    captured_amount  NUMERIC(19,2) NOT NULL DEFAULT 0,
    refunded_amount  NUMERIC(19,2) NOT NULL DEFAULT 0,
    failure_reason   VARCHAR(200),
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_payments_order UNIQUE (order_id),          -- FR-PAY-003: idempotent theo đơn
    CONSTRAINT uq_payments_provider_ref UNIQUE (provider_ref),
    -- BR-009 ở mức DB: tổng hoàn KHÔNG BAO GIỜ vượt số đã thu
    CONSTRAINT ck_payments_refund_limit CHECK (refunded_amount <= captured_amount),
    CONSTRAINT ck_payments_amounts CHECK (amount >= 0 AND captured_amount >= 0 AND refunded_amount >= 0)
);
CREATE INDEX ix_payments_status ON payments (status, updated_at DESC);

-- FR-PAY-019: sổ giao dịch tài chính bất biến. Điều chỉnh = thêm giao dịch bù có liên kết.
CREATE TABLE payment_transactions (
    id                 UUID PRIMARY KEY,
    payment_id         UUID          NOT NULL REFERENCES payments (id),
    type               VARCHAR(16)   NOT NULL,   -- CAPTURE | FAILURE | REFUND
    amount             NUMERIC(19,2) NOT NULL,
    currency           VARCHAR(3)    NOT NULL,
    provider_ref       VARCHAR(100),
    related_refund_id  UUID,
    note               VARCHAR(500),
    created_at         TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_payment_transactions_payment ON payment_transactions (payment_id, created_at);
CREATE TRIGGER payment_transactions_append_only BEFORE UPDATE OR DELETE ON payment_transactions
    FOR EACH ROW EXECUTE FUNCTION forbid_mutation();

-- FR-PAY-014..017
CREATE TABLE refunds (
    id               UUID PRIMARY KEY,
    payment_id       UUID          NOT NULL REFERENCES payments (id),
    order_id         UUID          NOT NULL,
    amount           NUMERIC(19,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL,
    reason           VARCHAR(500)  NOT NULL,
    status           VARCHAR(16)   NOT NULL,     -- PENDING | SUCCEEDED | FAILED
    idempotency_key  VARCHAR(120)  NOT NULL,
    requested_by     VARCHAR(64)   NOT NULL,
    provider_ref     VARCHAR(100),
    failure_reason   VARCHAR(500),
    attempts         INT           NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_refunds_idempotency UNIQUE (idempotency_key),   -- FR-PAY-016
    CONSTRAINT ck_refunds_amount CHECK (amount > 0)
);
CREATE INDEX ix_refunds_payment ON refunds (payment_id);
CREATE INDEX ix_refunds_pending ON refunds (updated_at) WHERE status = 'PENDING';

-- FR-PAY-009: chống webhook trùng theo id sự kiện của provider + lưu vết để điều tra
CREATE TABLE webhook_events (
    id                 UUID PRIMARY KEY,
    provider           VARCHAR(32)  NOT NULL,
    provider_event_id  VARCHAR(100) NOT NULL,
    event_type         VARCHAR(64)  NOT NULL,
    payload            TEXT         NOT NULL,
    result             VARCHAR(64),
    received_at        TIMESTAMPTZ  NOT NULL,
    processed_at       TIMESTAMPTZ,
    CONSTRAINT uq_webhook_events UNIQUE (provider, provider_event_id)
);

-- FR-PAY-018: đối soát
CREATE TABLE reconciliation_runs (
    id           UUID PRIMARY KEY,
    started_at   TIMESTAMPTZ NOT NULL,
    finished_at  TIMESTAMPTZ,
    checked      INT         NOT NULL DEFAULT 0,
    mismatches   INT         NOT NULL DEFAULT 0,
    triggered_by VARCHAR(64) NOT NULL
);
CREATE TABLE reconciliation_items (
    id                UUID PRIMARY KEY,
    run_id            UUID          NOT NULL REFERENCES reconciliation_runs (id),
    payment_id        UUID          NOT NULL,
    order_code        VARCHAR(32)   NOT NULL,
    internal_status   VARCHAR(24)   NOT NULL,
    provider_status   VARCHAR(24),
    internal_captured NUMERIC(19,2),
    provider_captured NUMERIC(19,2),
    internal_refunded NUMERIC(19,2),
    provider_refunded NUMERIC(19,2),
    issue             VARCHAR(200)  NOT NULL
);
CREATE INDEX ix_reconciliation_items_run ON reconciliation_items (run_id);

-- ---------------------------------------------------------------------
-- MÔ PHỎNG HỆ THỐNG BÊN NGOÀI: "cổng thanh toán giả" (MockPaymentProvider).
-- Trong thực tế dữ liệu này nằm ở VNPay/Stripe; để ở đây chỉ để demo và để có dữ liệu đối soát.
-- ---------------------------------------------------------------------
CREATE TABLE mock_provider_sessions (
    provider_ref     VARCHAR(100)  PRIMARY KEY,
    order_ref        UUID          NOT NULL,
    order_code       VARCHAR(32)   NOT NULL,
    amount           NUMERIC(19,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL,
    status           VARCHAR(24)   NOT NULL,    -- OPEN | PAID | FAILED | CANCELLED | REFUNDED | PARTIALLY_REFUNDED
    refunded_amount  NUMERIC(19,2) NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL
);
CREATE TABLE mock_provider_refunds (
    id               VARCHAR(100)  PRIMARY KEY,
    provider_ref     VARCHAR(100)  NOT NULL REFERENCES mock_provider_sessions (provider_ref),
    idempotency_key  VARCHAR(120)  NOT NULL UNIQUE,
    amount           NUMERIC(19,2) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL
);

-- =====================================================================
-- Bảng nền tảng có ở MỌI service (file này nằm trong common.jar).
-- Flyway của từng service đọc cả classpath:db/platform và classpath:db/migration.
-- Version 0.x để luôn chạy trước V1__ của service.
-- =====================================================================

-- Hàm dùng cho trigger: chặn sửa/xóa bảng chỉ-được-thêm (audit, ledger, lịch sử) - DR-005, FR-ADM-003
CREATE OR REPLACE FUNCTION forbid_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Table % is append-only: % is not allowed', TG_TABLE_NAME, TG_OP;
END;
$$ LANGUAGE plpgsql;

-- Transactional Outbox (CON-06)
CREATE TABLE outbox_event (
    id              UUID PRIMARY KEY,          -- = eventId
    aggregate_type  VARCHAR(64)  NOT NULL,
    aggregate_id    VARCHAR(64)  NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    event_version   INT          NOT NULL,
    payload         TEXT         NOT NULL,     -- EventEnvelope JSON
    correlation_id  VARCHAR(64),
    status          VARCHAR(16)  NOT NULL,     -- PENDING | PUBLISHED
    attempts        INT          NOT NULL DEFAULT 0,
    last_error      VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL,
    published_at    TIMESTAMPTZ
);
-- Partial index: chỉ index các dòng PENDING => job quét rất nhanh dù bảng lớn
CREATE INDEX ix_outbox_event_pending ON outbox_event (created_at) WHERE status = 'PENDING';

-- Consumer idempotent (COM-08)
CREATE TABLE processed_event (
    event_id      UUID         NOT NULL,
    consumer      VARCHAR(100) NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (event_id, consumer)
);

-- Audit (FR-ADM-002, FR-ADM-003)
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY,
    actor_id        VARCHAR(64)  NOT NULL,
    action          VARCHAR(64)  NOT NULL,
    target_type     VARCHAR(64)  NOT NULL,
    target_id       VARCHAR(64),
    before_data     TEXT,
    after_data      TEXT,
    result          VARCHAR(16)  NOT NULL,
    correlation_id  VARCHAR(64),
    client_ip       VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_audit_log_target ON audit_log (target_type, target_id, created_at DESC);
CREATE INDEX ix_audit_log_actor ON audit_log (actor_id, created_at DESC);
CREATE TRIGGER audit_log_append_only BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION forbid_mutation();

-- =====================================================================
-- notification-service: read model người nhận + nhật ký gửi
-- =====================================================================

-- Bản sao TỐI THIỂU thông tin liên hệ, dựng từ event UserRegistered (FR-NOT-004: không query DB của identity)
CREATE TABLE recipients (
    user_id     UUID PRIMARY KEY,
    email       VARCHAR(254) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

-- FR-NOT-005: mỗi lần gửi có trạng thái + provider message id.
-- UNIQUE(event_id, channel, template_code): cùng event không bao giờ tạo 2 thông báo cùng loại (FR-NOT-007)
CREATE TABLE notification_logs (
    id                   UUID PRIMARY KEY,
    event_id             UUID          NOT NULL,
    event_type           VARCHAR(100)  NOT NULL,
    template_code        VARCHAR(64)   NOT NULL,
    template_version     INT           NOT NULL,
    channel              VARCHAR(16)   NOT NULL,     -- EMAIL | SMS | PUSH
    locale               VARCHAR(8)    NOT NULL,
    recipient_user_id    UUID,
    recipient_masked     VARCHAR(254),               -- FR-NOT-009: KHÔNG lưu email đầy đủ trong log
    subject              VARCHAR(300),
    status               VARCHAR(16)   NOT NULL,     -- QUEUED | SENT | FAILED | SKIPPED
    provider_message_id  VARCHAR(200),
    attempts             INT           NOT NULL DEFAULT 0,
    last_error           VARCHAR(1000),
    created_at           TIMESTAMPTZ   NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL,
    sent_at              TIMESTAMPTZ,
    CONSTRAINT uq_notification_once UNIQUE (event_id, channel, template_code)
);
CREATE INDEX ix_notification_logs_status ON notification_logs (status, created_at DESC);

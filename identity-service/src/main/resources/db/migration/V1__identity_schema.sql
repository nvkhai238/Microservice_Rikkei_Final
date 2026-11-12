-- =====================================================================
-- identity-service: tài khoản, vai trò, phiên (refresh token), token một lần, địa chỉ
-- =====================================================================

-- FR-IAM-017: vai trò tối thiểu
CREATE TABLE roles (
    code         VARCHAR(32) PRIMARY KEY,
    description  VARCHAR(200) NOT NULL
);
INSERT INTO roles (code, description) VALUES
    ('CUSTOMER',        'Khách hàng'),
    ('CATALOG_STAFF',   'Nhân viên catalog'),
    ('INVENTORY_STAFF', 'Nhân viên kho'),
    ('ORDER_STAFF',     'Nhân viên vận hành đơn'),
    ('FINANCE_STAFF',   'Nhân viên tài chính'),
    ('ADMIN',           'Quản trị viên');

CREATE TABLE users (
    id                     UUID PRIMARY KEY,
    email                  VARCHAR(254) NOT NULL,
    password_hash          VARCHAR(100) NOT NULL,     -- bcrypt, KHÔNG BAO GIỜ lưu mật khẩu rõ (FR-IAM-003)
    full_name              VARCHAR(100) NOT NULL,
    phone                  VARCHAR(20),
    status                 VARCHAR(24)  NOT NULL,     -- PENDING_VERIFICATION | ACTIVE | DISABLED
    email_verified         BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts  INT          NOT NULL DEFAULT 0,
    locked_until           TIMESTAMPTZ,
    last_login_at          TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email UNIQUE (email),
    -- DR-011: email luôn được chuẩn hóa chữ thường trước khi lưu => unique đúng nghĩa
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(email))
);

CREATE TABLE user_roles (
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_code  VARCHAR(32) NOT NULL REFERENCES roles (code),
    PRIMARY KEY (user_id, role_code)
);

-- FR-IAM-011: refresh token xoay vòng theo "family". Chỉ lưu SHA-256 của token.
CREATE TABLE refresh_tokens (
    id             UUID PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash     VARCHAR(64) NOT NULL,
    family_id      UUID        NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    revoke_reason  VARCHAR(32),               -- ROTATED | LOGOUT | REUSE_DETECTED | PASSWORD_CHANGED | ADMIN_REVOKED...
    replaced_by    UUID,
    client_ip      VARCHAR(64),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_user_active ON refresh_tokens (user_id) WHERE revoked_at IS NULL;

-- FR-IAM-005, FR-IAM-013: token xác minh email / đặt lại mật khẩu: có hạn, dùng một lần, lưu hash
CREATE TABLE one_time_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     VARCHAR(24) NOT NULL,         -- EMAIL_VERIFICATION | PASSWORD_RESET
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_one_time_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_one_time_tokens_user ON one_time_tokens (user_id, purpose) WHERE used_at IS NULL;

-- FR-IAM-015: sổ địa chỉ
CREATE TABLE addresses (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_name  VARCHAR(100) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    line1           VARCHAR(200) NOT NULL,
    ward            VARCHAR(100) NOT NULL,
    district        VARCHAR(100) NOT NULL,
    province        VARCHAR(100) NOT NULL,
    postal_code     VARCHAR(10),
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX ix_addresses_user ON addresses (user_id);
-- Mỗi người tối đa MỘT địa chỉ mặc định: DB tự chặn, kể cả khi code có bug
CREATE UNIQUE INDEX uq_addresses_one_default ON addresses (user_id) WHERE is_default;

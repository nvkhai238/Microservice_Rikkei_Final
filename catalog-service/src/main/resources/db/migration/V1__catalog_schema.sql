-- =====================================================================
-- catalog-service: danh mục, thương hiệu, sản phẩm, SKU, hình ảnh
-- Catalog dùng PostgreSQL thay MongoDB + Elasticsearch (xem README). Full-text search dùng tsvector của Postgres.
-- =====================================================================

-- unaccent: bỏ dấu tiếng Việt để tìm "ao thun" ra "Áo thun". Là extension "trusted" => owner DB tự tạo được.
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE categories (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    parent_id   UUID REFERENCES categories (id),
    status      VARCHAR(16)  NOT NULL,       -- ACTIVE | INACTIVE (FR-CAT-001: kích hoạt/ngừng)
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_categories_slug UNIQUE (slug),
    CONSTRAINT ck_categories_not_self_parent CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE INDEX ix_categories_parent ON categories (parent_id);

CREATE TABLE brands (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_brands_slug UNIQUE (slug)
);

CREATE TABLE products (
    id             UUID PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    slug           VARCHAR(220) NOT NULL,
    description    TEXT,
    category_id    UUID         NOT NULL REFERENCES categories (id),
    brand_id       UUID REFERENCES brands (id),
    status         VARCHAR(16)  NOT NULL,     -- DRAFT | PUBLISHED | ARCHIVED
    -- Chỉ mục full-text (FR-CAT-014): tên + mã SKU + thương hiệu + danh mục + mô tả, đã bỏ dấu.
    -- Được tính lại mỗi khi sản phẩm/SKU đổi (FR-CAT-012) và có API tái lập toàn bộ (FR-CAT-018).
    search_vector  TSVECTOR,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_products_slug UNIQUE (slug)
);
CREATE INDEX ix_products_category ON products (category_id);
CREATE INDEX ix_products_brand ON products (brand_id);
CREATE INDEX ix_products_status_created ON products (status, created_at DESC);
CREATE INDEX ix_products_search ON products USING GIN (search_vector);

CREATE TABLE skus (
    id                  UUID PRIMARY KEY,
    product_id          UUID          NOT NULL REFERENCES products (id),
    sku_code            VARCHAR(64)   NOT NULL,
    variant_name        VARCHAR(200),
    attributes          TEXT          NOT NULL DEFAULT '{}',   -- JSON {"color":"red","size":"M"}
    price               NUMERIC(19,2) NOT NULL,                -- FR-CAT-008: decimal, KHÔNG dùng float
    currency            VARCHAR(3)    NOT NULL,
    status              VARCHAR(16)   NOT NULL,                -- ACTIVE | INACTIVE
    max_per_order       INT           NOT NULL DEFAULT 10,     -- FR-CART-004
    -- Bản sao "còn bao nhiêu" do order-service báo qua event StockLevelChanged (read model, eventual consistency)
    available_quantity  BIGINT        NOT NULL DEFAULT 0,
    stock_version       BIGINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,
    -- FR-CAT-006: mã SKU duy nhất toàn hệ thống (đã chuẩn hóa chữ HOA - DR-011)
    CONSTRAINT uq_skus_code UNIQUE (sku_code),
    CONSTRAINT ck_skus_code_upper CHECK (sku_code = upper(sku_code)),
    CONSTRAINT ck_skus_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_skus_max_per_order CHECK (max_per_order BETWEEN 1 AND 1000)
);
CREATE INDEX ix_skus_product ON skus (product_id);

CREATE TABLE product_images (
    id            UUID PRIMARY KEY,
    product_id    UUID         NOT NULL REFERENCES products (id),
    sku_id        UUID REFERENCES skus (id),
    file_name     VARCHAR(100) NOT NULL,     -- tên file do server sinh (UUID), KHÔNG dùng tên client gửi
    original_name VARCHAR(255),
    content_type  VARCHAR(50)  NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order    INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_product_images_file UNIQUE (file_name)
);
CREATE INDEX ix_product_images_product ON product_images (product_id);
CREATE UNIQUE INDEX uq_product_images_one_primary ON product_images (product_id) WHERE is_primary;

# Omnichannel E-commerce - Microservices

Hiện thực đặc tả `SRS_1_Omnichannel_Ecommerce.md` (SRS-OMNI-ECOM-001).

| Tài liệu | Nội dung |
|---|---|
| [docs/ROADMAP.md](docs/ROADMAP.md) | Tiến độ từng phase, việc đã làm / còn lại |
| [docs/API.md](docs/API.md) | Danh sách API theo service + quyền |
| [docs/EVENTS.md](docs/EVENTS.md) | Hợp đồng event, topology RabbitMQ, luồng Saga |
| [docs/TRACEABILITY.md](docs/TRACEABILITY.md) | Ma trận yêu cầu SRS → code → cách kiểm chứng |
| [loadtest/README.md](loadtest/README.md) | Kiểm thử tải k6 (500 req/s, flash sale) |
| Swagger UI | `http://localhost:<GATEWAY_PORT>/swagger-ui.html` (gộp API của mọi service) |

## Kiến trúc

```
                    Web / Mobile
                         │ HTTPS (tùy chọn ở dev)
                 ┌───────▼────────┐  verify JWT, rate limit (Redis), CORS,
                 │  api-gateway   │  correlation ID, access log, giới hạn kích thước
                 └───────┬────────┘
    ┌────────────┬───────┴─────┬───────────────┬────────────────┐
    ▼            ▼             ▼               ▼                ▼
 identity     catalog        order          payment        notification
 :8081        :8082          :8083          :8084          :8085
 JWT RS256    full-text      giỏ, kho,      intent,        email theo event
 RBAC, địa    search (PG),   đơn, Saga      webhook HMAC,  (Mailpit khi dev)
 chỉ, audit   ảnh, giá       orchestrator   hoàn tiền,
                                            đối soát
    │            ▲  REST nội bộ │  ▲             │  ▲              ▲
    │            └──────────────┘  │             │  │              │
    └────────── RabbitMQ: topic exchange ecommerce.events + Outbox + DLQ ──────┘
 Mỗi service một database PostgreSQL riêng (identity_db, catalog_db, order_db, payment_db, notification_db)
```

Luồng đặt hàng (Saga, order-service là nhạc trưởng):

```
Khách ──POST /orders (Idempotency-Key)──> order: giữ kho (all-or-nothing) + đơn PENDING + OrderCreated (Outbox)
        payment <── OrderCreated: tạo payment intent + link thanh toán
Khách ──trả tiền ở cổng thanh toán──> provider ──webhook có chữ ký HMAC──> payment: PaymentSucceeded (Outbox)
        order <── PaymentSucceeded: trừ kho chính thức, đơn CONFIRMED, OrderConfirmed
        notification <── các event trên: gửi email
Thất bại / hết 15 phút: trả kho, đơn CANCELLED. Tiền về muộn cho đơn đã hủy: tự hoàn tiền.
```

| Module | Port | Database | Trách nhiệm |
|---|---|---|---|
| `api-gateway` | 8080 (đổi bằng `GATEWAY_PORT`) | Redis (rate limit) | Điểm vào duy nhất (FR-GW-*) |
| `identity-service` | 8081 | `identity_db` | Tài khoản, JWT + refresh token xoay vòng, hồ sơ, địa chỉ, RBAC, audit |
| `catalog-service` | 8082 | `catalog_db` | Danh mục cây, thương hiệu, sản phẩm, SKU, giá, ảnh, tìm kiếm |
| `order-service` | 8083 | `order_db` | Giỏ hàng, tồn kho + giữ chỗ, đơn hàng, state machine, Saga |
| `payment-service` | 8084 | `payment_db` | Payment intent, mock provider, webhook, hoàn tiền, đối soát |
| `notification-service` | 8085 | `notification_db` | Nghe event, gửi email, chống gửi trùng |
| `common` | - | - | Lỗi/response chuẩn, correlation ID, security JWT, Outbox, consumer idempotent, audit |

## Chạy lần đầu

Yêu cầu: Java 17, Docker, Python 3 + curl (cho script test), OpenSSL.

```bash
cp .env.example .env              # rồi đổi các password "change_me_*"
scripts/gen-secrets.sh            # sinh khóa JWT, khóa mã hóa event, secret webhook, tài khoản admin, chứng chỉ HTTPS
docker compose up -d --wait       # PostgreSQL, Redis, RabbitMQ, Mailpit
scripts/run-all.sh start --build  # build + chạy 6 service
scripts/e2e-smoke.sh --uc07       # kiểm thử end-to-end UC-01..UC-07 (42 bước)
```

Máy đang có app khác chiếm cổng? Đổi trong `.env`: `GATEWAY_PORT`, `DB_PORT`, `REDIS_PORT`, `RABBITMQ_PORT`,
`RABBITMQ_UI_PORT`, `MAIL_PORT`, `MAILPIT_UI_PORT` (máy hiện tại đang dùng 18080 / 5433 / 6380 / 5673 / 15673 / 1026 / 8026).

### Lệnh hay dùng

```bash
scripts/run-all.sh status                       # service nào đang UP
scripts/run-all.sh logs order-service           # xem log
scripts/run-all.sh stop-one notification-service   # tắt 1 service (thử UC-07)
scripts/run-all.sh start-one notification-service
scripts/run-all.sh stop
scripts/reset-db.sh all                         # XÓA dữ liệu dev, Flyway tạo lại schema
./mvnw test                                     # 92 test (test tranh chấp tồn kho cần Docker)
```

IntelliJ: Compound run config `All services` (đừng chạy song song với `run-all.sh`, sẽ trùng cổng).

### Chạy toàn bộ trong Docker

```bash
./mvnw clean package -DskipTests
docker compose --profile apps up -d --build --wait
```

Chỉ gateway mở cổng ra ngoài; các service gọi nhau qua tên trong mạng docker.

### HTTPS (CON-03)

`scripts/gen-secrets.sh` đã tạo chứng chỉ tự ký `secrets/gateway-keystore.p12`. Bật bằng `GATEWAY_SSL_ENABLED=true`
trong `.env`, khởi động lại gateway, gọi `https://localhost:<GATEWAY_PORT>` (TLS 1.3/1.2). Production dùng chứng chỉ thật
hoặc kết thúc TLS ở load balancer.

## Dùng thử bằng tay

| Công cụ | URL | Đăng nhập |
|---|---|---|
| Swagger UI | http://localhost:18080/swagger-ui.html | Bấm Authorize, dán access token |
| Mailpit (xem email) | http://localhost:8026 | - |
| RabbitMQ UI | http://localhost:15673 | `RABBITMQ_USER` / `RABBITMQ_PASSWORD` |
| PostgreSQL | `localhost:5433` | vd `order_user` / `ORDER_DB_PASSWORD` |
| Admin | `ADMIN_EMAIL` / `ADMIN_PASSWORD` trong `.env` | role ADMIN (làm được mọi việc của nhân viên) |

Thanh toán: sau khi đặt đơn, gọi `GET /api/v1/payments/orders/{orderId}` lấy `checkoutUrl`, mở trên trình duyệt
(trang của cổng thanh toán giả lập) và bấm "Thanh toán thành công" hoặc "Giả lập thất bại".
Kết quả đến bằng webhook có chữ ký, không phải bằng redirect (FR-PAY-011).

Cấp quyền nhân viên: admin gọi `POST /api/v1/admin/iam/users/{id}/roles` với `{"role":"CATALOG_STAFF"}`
(CATALOG_STAFF, INVENTORY_STAFF, ORDER_STAFF, FINANCE_STAFF, ADMIN). Role mới có hiệu lực ở access token kế tiếp.

## Quyết định kỹ thuật

| Vấn đề | Chọn | Lý do |
|---|---|---|
| Framework | Java 17, Spring Boot 4.0.9, Spring Cloud 2025.1.4 | Cặp stable mới nhất (Boot 4.1 chưa có Spring Cloud stable) |
| Database | PostgreSQL, mỗi service 1 DB + 1 user | CON-02, NFR-SEC-004 |
| Catalog + search | PostgreSQL `tsvector` + `unaccent` thay MongoDB + Elasticsearch | Đủ cho full-text tiếng Việt không dấu, xếp hạng liên quan, ít hạ tầng. Chỉ mục là dẫn xuất, tái lập được (DR-013) |
| Broker (TBD-01) | RabbitMQ, topic exchange + DLQ | Dễ vận hành, có ack/retry/DLQ sẵn |
| Gửi event | Transactional Outbox + publisher confirm | CON-06: không mất event, không event "ma" |
| Chống oversell | `UPDATE ... WHERE on_hand - reserved >= q` + CHECK constraint | Nguyên tử trong DB, không cần distributed lock; test 100 luồng |
| JWT | RS256, JWKS ở identity, access 15 phút, refresh 7 ngày xoay vòng + phát hiện reuse | Service khác chỉ cần public key |
| Phân quyền | Gateway verify JWT + MỖI service tự verify lại và kiểm role | FR-GW-004, zero trust |
| Thanh toán (TBD-02) | Mock provider (trang checkout + webhook HMAC kiểu Stripe) | Test được toàn luồng; thêm VNPay/Stripe = viết 1 adapter `PaymentProvider` |
| Giữ kho (TBD-03) | 15 phút, `RESERVATION_TTL` | A-08 |
| Hủy đơn (TBD-04) | Khách/nhân viên hủy được khi PENDING hoặc CONFIRMED; SHIPPING thì không | Ma trận 7.1 |
| Thuế/ship (TBD-05) | 0 | FR-ORD-006 cho phép |
| Định danh (TBD-08) | Email; bắt buộc xác minh bật bằng `EMAIL_VERIFICATION_REQUIRED=true` | Mặc định tắt cho dễ demo |
| Kho (TBD-10) | 1 kho `MAIN` | FR-INV-001 |
| Refund (TBD-11) | Toàn phần khi hủy đơn; nhân viên tài chính hoàn một phần được | FR-PAY-014 |
| Token trong event | Mã hóa AES-256-GCM (`EVENT_PAYLOAD_KEY`) | Mục 5.5: event không chứa bí mật dạng rõ |
| Secret | `.env` + `secrets/` (đều gitignored) | CON-09 |

## Quy ước code

- Package chia **theo tính năng** (`auth`, `cart`, `inventory`...), không theo tầng.
- API công khai `/api/v1/...`, quản trị `/api/v1/admin/...`, nội bộ `/internal/v1/...` (gateway không route).
- Response thành công `{code, message, data, correlationId}`, lỗi `{code, message, details?, correlationId}`.
- Tiền: `BigDecimal` + `NUMERIC(19,2)`. Thời gian: `Instant`, lưu UTC. ID: `UUID` tự sinh (`AssignedIdEntity`).
- Bảng lịch sử/sổ cái/audit chỉ được INSERT (trigger `forbid_mutation` chặn UPDATE/DELETE).
- Đổi trạng thái tiền/kho/đơn luôn qua method của entity hoặc câu UPDATE có điều kiện, không set field tùy tiện.

## Giới hạn hiện tại

- Chưa có giao diện Web/Mobile (UI-01..UI-10): hệ thống mới có backend + Swagger UI.
- Chưa tích hợp cổng thanh toán sandbox thật (VNPay/Stripe); kiến trúc adapter đã sẵn.
- SMS/push chỉ có interface `NotificationChannel`, chưa có nhà cung cấp.
- Gọi nội bộ `/internal/**` chưa có xác thực service-to-service (dựa vào việc gateway không route ra ngoài).
  Production nên thêm mTLS hoặc network policy.
- Load test k6 đã có script nhưng chưa chạy đủ 15 phút trên máy này (chưa cài k6).

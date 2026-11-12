# Load test (NFR-PERF-005, NFR-PERF-006, UC-05)

Cần cài [k6](https://k6.io/docs/get-started/installation/).

1. Nới rate limit của gateway khi test tải (mọi request đến từ 1 IP):
   thêm vào `.env` rồi khởi động lại gateway
   ```
   RATE_LIMIT_ENABLED=false
   ```
2. Tạo dữ liệu (khách hàng + sản phẩm + tồn kho), ghi ra `loadtest/data.json`:
   ```bash
   loadtest/seed.sh 200 10      # 200 khách, SKU flash sale tồn 10
   ```
3. Chạy (access token sống 15 phút, chạy ngay sau bước 2):
   ```bash
   # Tải hỗn hợp 500 req/s trong 15 phút (bản thử nhanh: -e RATE=100 -e DURATION=1m)
   k6 run -e BASE=http://localhost:8080 loadtest/k6-mixed.js
   # Flash sale: 200 người cùng lúc mua SKU tồn 10
   k6 run -e BASE=http://localhost:8080 loadtest/k6-flash-sale.js
   ```
4. Ghi kết quả (p95, p99, tỉ lệ lỗi, số đơn thắng) vào báo cáo kiểm thử.

Chứng minh không oversell ở mức code còn có test tự động chạy trên PostgreSQL thật:
`order-service/src/test/java/com/ecommerce/order/inventory/InventoryConcurrencyIntegrationTest.java`
(100 luồng mua SKU tồn 1: đúng 1 thành công, tồn không âm).

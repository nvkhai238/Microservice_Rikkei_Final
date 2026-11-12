#!/usr/bin/env bash
# =============================================================================
# Kiểm thử end-to-end qua API Gateway, theo các kịch bản nghiệm thu SRS mục 8:
#   UC-01 đăng ký/đăng nhập · UC-02 tìm kiếm · UC-03 đặt hàng + thanh toán thành công
#   UC-04 thanh toán thất bại · UC-06 hủy đơn đã thanh toán (hoàn tiền + hoàn kho)
#   Webhook giả mạo / replay · Nhân viên chuyển SHIPPING -> COMPLETED
#   UC-07 (tùy chọn, thêm --uc07): tắt notification, đặt + trả tiền, bật lại => mail vẫn tới đúng 1 lần
#
# Yêu cầu: hạ tầng docker + 6 service đang chạy (scripts/run-all.sh start), python3, curl.
# Chạy:  scripts/e2e-smoke.sh            hoặc   scripts/e2e-smoke.sh --uc07
# =============================================================================
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
envget() { grep -E "^$1=" "$ROOT/.env" | tail -1 | cut -d= -f2-; }
GW="http://localhost:$(envget GATEWAY_PORT || true)"; [[ "$GW" == "http://localhost:" ]] && GW="http://localhost:8080"
API="$GW/api/v1"
MAILPIT="http://localhost:$(envget MAILPIT_UI_PORT || echo 8025)"
ADMIN_EMAIL="$(envget ADMIN_EMAIL)"; ADMIN_PASSWORD="$(envget ADMIN_PASSWORD)"
WEBHOOK_SECRET="$(envget PAYMENT_WEBHOOK_SECRET)"
RUN_UC07=0; [[ "${1:-}" == "--uc07" ]] && RUN_UC07=1

PASS=0; FAIL=0
ok()   { echo "  ✔ $1"; PASS=$((PASS+1)); }
bad()  { echo "  ✘ $1"; FAIL=$((FAIL+1)); }
check() { if [[ "$2" == "$3" ]]; then ok "$1"; else bad "$1 (mong đợi '$3', nhận '$2')"; fi; }
title() { echo; echo "=== $1 ==="; }
# json '<python expr on d>' : đọc JSON từ stdin
json() { python3 -c "import json,sys
try: d=json.load(sys.stdin)
except Exception: print(''); sys.exit()
try: print($1)
except Exception: print('')"; }
req() { # req METHOD PATH [TOKEN] [BODY] [EXTRA_HEADER]
  local m="$1" p="$2" t="${3:-}" b="${4:-}" h="${5:-}"
  local args=(-s -X "$m" "$API$p" -H 'Content-Type: application/json')
  [[ -n "$t" ]] && args+=(-H "Authorization: Bearer $t")
  [[ -n "$h" ]] && args+=(-H "$h")
  [[ -n "$b" ]] && args+=(-d "$b")
  curl "${args[@]}"
}
status() { # status METHOD PATH [TOKEN] [BODY] [EXTRA_HEADER] -> HTTP code
  local m="$1" p="$2" t="${3:-}" b="${4:-}" h="${5:-}"
  local args=(-s -o /dev/null -w '%{http_code}' -X "$m" "$API$p" -H 'Content-Type: application/json')
  [[ -n "$t" ]] && args+=(-H "Authorization: Bearer $t")
  [[ -n "$h" ]] && args+=(-H "$h")
  [[ -n "$b" ]] && args+=(-d "$b")
  curl "${args[@]}"
}
wait_for() { # wait_for <mô tả> <số giây> <lệnh trả về chuỗi> <giá trị mong đợi>
  local desc="$1" secs="$2" cmd="$3" want="$4" got=""
  for _ in $(seq 1 "$secs"); do got="$(eval "$cmd")"; [[ "$got" == "$want" ]] && break; sleep 1; done
  check "$desc" "$got" "$want"
}
uuid() { python3 -c 'import uuid; print(uuid.uuid4())'; }
login() { req POST /auth/login "" "{\"email\":\"$1\",\"password\":\"$2\"}" | json "d['data']['accessToken']"; }
mail_count() { # mail_count <email> <chuỗi trong tiêu đề>: đếm mail trong Mailpit
  local q; q="$(python3 -c 'import sys,urllib.parse; print(urllib.parse.quote(f"to:\"{sys.argv[1]}\" subject:\"{sys.argv[2]}\""))' "$1" "$2")"
  curl -s "$MAILPIT/api/v1/search?query=$q" | json "d['messages_count']"
}

RUN="$(date +%s)"
CUSTOMER="e2e.$RUN@example.com"; PASSWORD="Secret${RUN}x"

title "UC-01 Đăng ký và đăng nhập"
check "Đăng ký trả 201" "$(status POST /auth/register "" "{\"email\":\"$CUSTOMER\",\"password\":\"$PASSWORD\",\"fullName\":\"Khách E2E\",\"phone\":\"0912345678\"}")" "201"
check "Đăng ký trùng email trả 409" "$(status POST /auth/register "" "{\"email\":\"$CUSTOMER\",\"password\":\"$PASSWORD\",\"fullName\":\"Khách E2E\"}")" "409"
check "Mật khẩu yếu trả 400" "$(status POST /auth/register "" "{\"email\":\"weak.$RUN@example.com\",\"password\":\"123\",\"fullName\":\"Weak\"}")" "400"
check "Sai mật khẩu trả 401 (không lộ tài khoản)" "$(req POST /auth/login "" "{\"email\":\"$CUSTOMER\",\"password\":\"wrong-pass-1\"}" | json "d['code']")" "INVALID_CREDENTIALS"
LOGIN="$(req POST /auth/login "" "{\"email\":\"$CUSTOMER\",\"password\":\"$PASSWORD\"}")"
TOKEN="$(echo "$LOGIN" | json "d['data']['accessToken']")"; REFRESH="$(echo "$LOGIN" | json "d['data']['refreshToken']")"
[[ -n "$TOKEN" ]] && ok "Đăng nhập nhận access + refresh token" || bad "Đăng nhập"
NEW_REFRESH="$(req POST /auth/refresh "" "{\"refreshToken\":\"$REFRESH\"}" | json "d['data']['refreshToken']")"
[[ -n "$NEW_REFRESH" ]] && ok "Refresh token xoay vòng" || bad "Refresh token"
check "Dùng lại refresh token cũ bị từ chối (reuse detection)" "$(status POST /auth/refresh "" "{\"refreshToken\":\"$REFRESH\"}")" "401"
check "Không token => 401" "$(status GET /users/me)" "401"
ADDRESS="$(req POST /users/me/addresses "$TOKEN" '{"recipientName":"Khách E2E","phone":"0912345678","line1":"1 Lê Lợi","ward":"Bến Nghé","district":"Quận 1","province":"TP.HCM"}' | json "d['data']['id']")"
[[ -n "$ADDRESS" ]] && ok "Thêm địa chỉ giao hàng" || bad "Thêm địa chỉ"
wait_for "Mail chào mừng gửi tới khách" 15 "mail_count '$CUSTOMER' 'Chào mừng'" "1"

title "Chuẩn bị dữ liệu (admin)"
ADMIN="$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")"
[[ -n "$ADMIN" ]] && ok "Admin đăng nhập" || { bad "Admin đăng nhập (kiểm tra ADMIN_EMAIL/ADMIN_PASSWORD trong .env)"; exit 1; }
check "Khách gọi API admin => 403" "$(status GET /admin/iam/users "$TOKEN")" "403"
CAT="$(req POST /admin/catalog/categories "$ADMIN" "{\"name\":\"Điện thoại $RUN\"}" | json "d['data']['id']")"
BRAND="$(req POST /admin/catalog/brands "$ADMIN" "{\"name\":\"Brand $RUN\"}" | json "d['data']['id']")"
PRODUCT="$(req POST /admin/catalog/products "$ADMIN" "{\"name\":\"Điện thoại Thử Nghiệm $RUN\",\"description\":\"Máy test\",\"categoryId\":\"$CAT\",\"brandId\":\"$BRAND\",\"skus\":[{\"skuCode\":\"PHONE-$RUN-BLK\",\"variantName\":\"Đen\",\"attributes\":{\"color\":\"Đen\"},\"price\":1500000}]}")"
PRODUCT_ID="$(echo "$PRODUCT" | json "d['data']['id']")"; SKU="$(echo "$PRODUCT" | json "d['data']['skus'][0]['id']")"
check "Publish sản phẩm" "$(req PATCH /admin/catalog/products/$PRODUCT_ID/status "$ADMIN" '{"status":"PUBLISHED"}' | json "d['data']['status']")" "PUBLISHED"
check "Nhập kho 5 sản phẩm" "$(req POST /admin/inventory/stocks/$SKU/receipts "$ADMIN" '{"quantity":5,"reason":"E2E nhập hàng"}' | json "d['data']['available']")" "5"

title "UC-02 Tìm kiếm và xem sản phẩm"
check "Tìm không dấu 'dien thoai thu nghiem $RUN'" "$(curl -s "$API/products?q=dien%20thoai%20thu%20nghiem%20$RUN" | json "d['data']['totalElements']")" "1"
wait_for "Chi tiết sản phẩm báo còn hàng (event StockLevelChanged)" 10 "curl -s $API/products/$PRODUCT_ID | json \"d['data']['inStock']\"" "True"
check "size quá lớn bị chặn (400)" "$(curl -s -o /dev/null -w '%{http_code}' "$API/products?size=5000")" "400"

place_order() { # place_order <key> -> order id
  req POST /cart/items "$TOKEN" "{\"skuId\":\"$SKU\",\"quantity\":${2:-1}}" >/dev/null
  req POST /orders "$TOKEN" "{\"addressId\":\"$ADDRESS\",\"paymentMethod\":\"MOCK\"}" "Idempotency-Key: $1" | json "d['data']['id']"
}
order_status() { req GET /orders/$1 "$TOKEN" | json "d['data']['status'] + '/' + d['data']['paymentStatus']"; }
checkout_url() { req GET /payments/orders/$1 "$TOKEN" | json "d['data']['checkoutUrl'] or ''"; }
stock() { req GET /admin/inventory/stocks/$SKU "$ADMIN" | json "str(d['data']['onHand']) + '/' + str(d['data']['reserved'])"; }
pay() { # pay <orderId> <success|fail>
  local url=""; for _ in $(seq 1 20); do url="$(checkout_url "$1")"; [[ -n "$url" ]] && break; sleep 0.5; done
  curl -s -X POST "${url/$GW/$GW}/pay?outcome=$2" >/dev/null
}

title "UC-03 Đặt hàng và thanh toán thành công"
KEY="$(uuid)"
ORDER1="$(place_order "$KEY" 2)"
[[ -n "$ORDER1" ]] && ok "Tạo đơn PENDING" || bad "Tạo đơn"
check "Gửi lặp cùng Idempotency-Key trả 200 cùng đơn" "$(req POST /orders "$TOKEN" "{\"addressId\":\"$ADDRESS\",\"paymentMethod\":\"MOCK\"}" "Idempotency-Key: $KEY" | json "d['data']['id']")" "$ORDER1"
check "Cùng khóa nhưng nội dung khác => 409" "$(status POST /orders "$TOKEN" "{\"addressId\":\"$ADDRESS\",\"paymentMethod\":\"MOCK\",\"note\":\"khac\"}" "Idempotency-Key: $KEY")" "409"
check "Kho: giữ 2 (onHand/reserved)" "$(stock)" "5/2"
pay "$ORDER1" success
wait_for "Webhook => đơn CONFIRMED/PAID" 20 "order_status $ORDER1" "CONFIRMED/PAID"
check "Kho trừ chính thức (onHand/reserved)" "$(stock)" "3/0"
wait_for "Mail xác nhận đơn gửi đúng 1 lần (webhook gửi trùng vẫn 1 mail)" 15 "mail_count '$CUSTOMER' 'xác nhận'" "1"

title "Bảo mật webhook (FR-PAY-007, 008)"
BODY='{"id":"evt_fake","type":"payment.succeeded","data":{"providerRef":"x","orderRef":"00000000-0000-0000-0000-000000000000","amount":"1","currency":"VND"}}'
check "Chữ ký sai => 401" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/webhooks/payments/mock" -H 'Content-Type: application/json' -H 'X-Mock-Signature: t=1,v1=deadbeef' -d "$BODY")" "401"
OLD=$(( $(date +%s) - 3600 ))
SIG="$(printf '%s' "$OLD.$BODY" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -hex | awk '{print $NF}')"
check "Chữ ký đúng nhưng timestamp cũ 1 giờ (replay) => 400" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/webhooks/payments/mock" -H 'Content-Type: application/json' -H "X-Mock-Signature: t=$OLD,v1=$SIG" -d "$BODY")" "400"

title "UC-04 Thanh toán thất bại"
ORDER2="$(place_order "$(uuid)" 1)"
check "Kho giữ thêm 1" "$(stock)" "3/1"
pay "$ORDER2" fail
wait_for "Đơn CANCELLED do thanh toán thất bại" 20 "order_status $ORDER2" "CANCELLED/FAILED"
check "Reservation được trả đúng 1 lần" "$(stock)" "3/0"

title "Vận hành: nhân viên chuyển trạng thái"
ORDER3="$(place_order "$(uuid)" 1)"; pay "$ORDER3" success
wait_for "Đơn thứ 3 CONFIRMED" 20 "order_status $ORDER3" "CONFIRMED/PAID"
check "Không được nhảy CONFIRMED -> COMPLETED" "$(status PATCH /admin/orders/$ORDER3/status "$ADMIN" '{"status":"COMPLETED","reason":"skip"}')" "409"
check "CONFIRMED -> SHIPPING" "$(req PATCH /admin/orders/$ORDER3/status "$ADMIN" '{"status":"SHIPPING","reason":"Bàn giao vận chuyển"}' | json "d['data']['status']")" "SHIPPING"
check "Khách không hủy được đơn đang giao" "$(status POST /orders/$ORDER3/cancel "$TOKEN" '{"reason":"đổi ý"}')" "422"
check "SHIPPING -> COMPLETED" "$(req PATCH /admin/orders/$ORDER3/status "$ADMIN" '{"status":"COMPLETED","reason":"Giao thành công"}' | json "d['data']['status']")" "COMPLETED"

title "UC-06 Hủy đơn đã thanh toán"
check "Khách yêu cầu hủy đơn 1 (đang chờ hoàn tiền)" "$(req POST /orders/$ORDER1/cancel "$TOKEN" '{"reason":"Đổi ý"}' | json "d['data']['paymentStatus']")" "REFUND_PENDING"
check "Hủy lặp lại là idempotent" "$(status POST /orders/$ORDER1/cancel "$TOKEN" '{"reason":"Đổi ý"}')" "200"
wait_for "Hoàn tiền xong => CANCELLED/REFUNDED" 20 "order_status $ORDER1" "CANCELLED/REFUNDED"
check "Hoàn kho 2 sản phẩm" "$(stock)" "4/0"
PAY1="$(req GET /payments/orders/$ORDER1 "$TOKEN" | json "d['data']['id']")"
check "Không hoàn vượt số đã thu (422)" "$(status POST /admin/payments/$PAY1/refunds "$ADMIN" '{"amount":1000,"reason":"thêm"}' "Idempotency-Key: $(uuid)")" "422"

if [[ $RUN_UC07 -eq 1 ]]; then
  title "UC-07 Notification bị dừng"
  "$ROOT/scripts/run-all.sh" stop-one notification-service >/dev/null
  ORDER4="$(place_order "$(uuid)" 1)"; pay "$ORDER4" success
  wait_for "Đơn vẫn CONFIRMED khi notification tắt" 20 "order_status $ORDER4" "CONFIRMED/PAID"
  CODE4="$(req GET /orders/$ORDER4 "$TOKEN" | json "d['data']['orderCode']")"
  check "Chưa có mail của đơn 4" "$(mail_count "$CUSTOMER" "$CODE4 đã được xác nhận")" "0"
  "$ROOT/scripts/run-all.sh" start-one notification-service >/dev/null
  wait_for "Bật lại: mail xác nhận đơn 4 tới đúng 1 lần" 40 "mail_count '$CUSTOMER' '$CODE4 đã được xác nhận'" "1"
fi

echo; echo "Kết quả: $PASS đạt, $FAIL lỗi"
[[ $FAIL -eq 0 ]]

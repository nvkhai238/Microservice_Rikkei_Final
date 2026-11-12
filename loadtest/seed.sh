#!/usr/bin/env bash
# Tạo dữ liệu cho load test: N khách hàng (mỗi người 1 địa chỉ), 1 sản phẩm thường (tồn lớn)
# và 1 sản phẩm flash sale (tồn ít). Ghi kết quả ra loadtest/data.json cho k6 đọc.
#   loadtest/seed.sh 200 10        # 200 khách, SKU flash sale có 10 cái
# LƯU Ý: tắt/nới rate limit trước (vd RATE_LIMIT_ENABLED=false) vì đăng ký bị giới hạn 10 lần/phút/IP.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
envget() { grep -E "^$1=" "$ROOT/.env" | tail -1 | cut -d= -f2-; }
PORT="$(envget GATEWAY_PORT || true)"; API="http://localhost:${PORT:-8080}/api/v1"
USERS="${1:-200}"; FLASH_STOCK="${2:-10}"; RUN="$(date +%s)"
json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)"; }
post() { curl -sf -X POST "$API$1" -H 'Content-Type: application/json' ${3:+-H "Authorization: Bearer $3"} -d "$2"; }

ADMIN="$(post /auth/login "{\"email\":\"$(envget ADMIN_EMAIL)\",\"password\":\"$(envget ADMIN_PASSWORD)\"}" | json "d['data']['accessToken']")"
CAT="$(post /admin/catalog/categories "{\"name\":\"Load $RUN\"}" "$ADMIN" | json "d['data']['id']")"
mk() { # mk <code> <price> <stock>
  local p sku
  p="$(post /admin/catalog/products "{\"name\":\"Load $1 $RUN\",\"categoryId\":\"$CAT\",\"skus\":[{\"skuCode\":\"$1-$RUN\",\"price\":$2,\"maxPerOrder\":5}]}" "$ADMIN")"
  sku="$(echo "$p" | json "d['data']['skus'][0]['id']")"
  curl -sf -X PATCH "$API/admin/catalog/products/$(echo "$p" | json "d['data']['id']")/status" -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d '{"status":"PUBLISHED"}' >/dev/null
  post "/admin/inventory/stocks/$sku/receipts" "{\"quantity\":$3,\"reason\":\"load test\"}" "$ADMIN" >/dev/null
  echo "$sku"
}
NORMAL_SKU="$(mk NORMAL 100000 1000000)"
FLASH_SKU="$(mk FLASH 9000 "$FLASH_STOCK")"

echo "[" > "$ROOT/loadtest/users.tmp"
for i in $(seq 1 "$USERS"); do
  email="load.$RUN.$i@example.com"
  post /auth/register "{\"email\":\"$email\",\"password\":\"LoadTest${RUN}x\",\"fullName\":\"Load $i\"}" >/dev/null
  token="$(post /auth/login "{\"email\":\"$email\",\"password\":\"LoadTest${RUN}x\"}" | json "d['data']['accessToken']")"
  addr="$(post /users/me/addresses '{"recipientName":"Load","phone":"0912345678","line1":"1 A","ward":"W","district":"D","province":"P"}' "$token" | json "d['data']['id']")"
  [[ $i -gt 1 ]] && echo "," >> "$ROOT/loadtest/users.tmp"
  printf '{"email":"%s","token":"%s","addressId":"%s"}' "$email" "$token" "$addr" >> "$ROOT/loadtest/users.tmp"
done
echo "]" >> "$ROOT/loadtest/users.tmp"
python3 - "$ROOT/loadtest" "$NORMAL_SKU" "$FLASH_SKU" "$FLASH_STOCK" <<'PY'
import json,sys,os
d,normal,flash,stock=sys.argv[1],sys.argv[2],sys.argv[3],int(sys.argv[4])
users=json.load(open(os.path.join(d,'users.tmp')))
json.dump({"normalSku":normal,"flashSku":flash,"flashStock":stock,"users":users},open(os.path.join(d,'data.json'),'w'))
os.remove(os.path.join(d,'users.tmp'))
PY
echo "Đã ghi loadtest/data.json: $USERS khách, SKU thường $NORMAL_SKU, SKU flash $FLASH_SKU (tồn $FLASH_STOCK)"
echo "Access token sống 15 phút: chạy k6 ngay sau khi seed."

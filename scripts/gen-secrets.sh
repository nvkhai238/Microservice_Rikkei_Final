#!/usr/bin/env bash
# Sinh các secret cho môi trường dev (chạy 1 lần, chạy lại cũng an toàn: cái nào có rồi thì giữ nguyên).
#   - secrets/jwt-private.pem, secrets/jwt-public.pem : cặp khóa RSA ký JWT (identity-service)
#   - EVENT_PAYLOAD_KEY trong .env                    : khóa AES-256 mã hóa token trong event
#   - PAYMENT_WEBHOOK_SECRET trong .env               : khóa HMAC ký webhook của mock payment provider
#   - ADMIN_EMAIL / ADMIN_PASSWORD trong .env          : tài khoản admin đầu tiên
#   - secrets/gateway-keystore.p12                     : chứng chỉ tự ký cho HTTPS ở gateway (CON-03)
# Thư mục secrets/ và file .env đã nằm trong .gitignore.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
mkdir -p secrets
chmod 700 secrets

[[ -f .env ]] || cp .env.example .env

if [[ ! -f secrets/jwt-private.pem ]]; then
  echo "==> Sinh cặp khóa RSA 2048 cho JWT"
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/jwt-private.pem 2>/dev/null
  openssl pkey -in secrets/jwt-private.pem -pubout -out secrets/jwt-public.pem
  chmod 600 secrets/jwt-private.pem
fi

set_env() { # thêm KEY=VALUE vào .env nếu KEY chưa có giá trị
  local key="$1" value="$2"
  if grep -qE "^${key}=.+" .env; then return; fi
  if grep -qE "^#? ?${key}=" .env; then
    sed -i -E "s|^#? ?${key}=.*|${key}=${value}|" .env
  else
    printf '%s=%s\n' "$key" "$value" >> .env
  fi
  echo "==> Đã đặt ${key} trong .env"
}

set_env EVENT_PAYLOAD_KEY "$(openssl rand -base64 32)"
set_env PAYMENT_WEBHOOK_SECRET "$(openssl rand -hex 32)"
set_env ADMIN_EMAIL "admin@ecommerce.local"
set_env ADMIN_PASSWORD "Pw$(openssl rand -hex 5)9"

if [[ ! -f secrets/gateway-keystore.p12 ]]; then
  echo "==> Sinh chứng chỉ tự ký cho HTTPS gateway (dev)"
  pass="$(openssl rand -hex 16)"
  keytool -genkeypair -alias gateway -keyalg RSA -keysize 2048 -validity 825 \
    -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1" \
    -storetype PKCS12 -keystore secrets/gateway-keystore.p12 -storepass "$pass" >/dev/null 2>&1
  set_env GATEWAY_KEYSTORE_PASSWORD "$pass"
fi

echo "Xong. Tài khoản admin: $(grep '^ADMIN_EMAIL=' .env | cut -d= -f2) / mật khẩu xem ADMIN_PASSWORD trong .env"

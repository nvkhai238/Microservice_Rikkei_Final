#!/usr/bin/env bash
# XÓA SẠCH dữ liệu của một (hoặc tất cả) service trong môi trường DEV, để Flyway tạo lại schema từ đầu.
#   scripts/reset-db.sh identity      # chỉ identity_db
#   scripts/reset-db.sh all           # cả 5 database
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
reset() {
  echo "==> Reset ${1}_db"
  docker exec ecom-postgres psql -q -U postgres -d "${1}_db" \
    -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION ${1}_user;"
}
case "${1:-}" in
  identity|catalog|order|payment|notification) reset "$1" ;;
  all) for s in identity catalog order payment notification; do reset "$s"; done ;;
  *) sed -n '2,5p' "$0"; exit 1 ;;
esac

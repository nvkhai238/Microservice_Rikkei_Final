#!/bin/sh
# Chạy MỘT LẦN khi volume postgres còn trống (lần đầu `docker compose up`).
# Tạo database + user riêng cho từng service => service A không có quyền đọc DB của service B (CON-02).
# Muốn chạy lại: docker compose down -v && docker compose up -d
set -e

create_db() {
  db="$1"; user="$2"; pwd="$3"
  echo ">> Creating database $db owned by $user"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-SQL
    CREATE USER $user WITH PASSWORD '$pwd';
    CREATE DATABASE $db OWNER $user;
    REVOKE ALL ON DATABASE $db FROM PUBLIC;
SQL
}

create_db identity_db     identity_user     "$IDENTITY_DB_PASSWORD"
create_db catalog_db      catalog_user      "$CATALOG_DB_PASSWORD"
create_db order_db        order_user        "$ORDER_DB_PASSWORD"
create_db payment_db      payment_user      "$PAYMENT_DB_PASSWORD"
create_db notification_db notification_user "$NOTIFICATION_DB_PASSWORD"

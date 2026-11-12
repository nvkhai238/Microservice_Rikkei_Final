#!/usr/bin/env bash
# Chạy toàn bộ microservice từ terminal (thay cho IntelliJ).
#
#   scripts/run-all.sh start            bật hạ tầng docker, build nếu thiếu jar, chạy 6 service
#   scripts/run-all.sh start --build    build lại từ đầu rồi chạy
#   scripts/run-all.sh stop             dừng 6 service (hạ tầng docker giữ nguyên)
#   scripts/run-all.sh status           xem service nào đang UP
#   scripts/run-all.sh logs <service>   theo dõi log 1 service, ví dụ: logs order-service
#   scripts/run-all.sh stop-one <service>   dừng 1 service (vd: thử UC-07 tắt notification-service)
#   scripts/run-all.sh start-one <service>  bật lại 1 service
#
# Cổng gateway mặc định 8080; máy đang có app khác giữ 8080 thì đặt GATEWAY_PORT trong .env (vd: 18080).
#
# Log và PID lưu trong thư mục .run/ (đã gitignore).

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT/.run"
LOG_DIR="$RUN_DIR/logs"
mkdir -p "$LOG_DIR"
cd "$ROOT"

SERVICES=(api-gateway identity-service catalog-service order-service payment-service notification-service)

# Đọc GATEWAY_PORT từ môi trường hoặc .env
if [[ -z "${GATEWAY_PORT:-}" && -f "$ROOT/.env" ]]; then
  GATEWAY_PORT="$(grep -E '^GATEWAY_PORT=' "$ROOT/.env" | tail -1 | cut -d= -f2 || true)"
fi
GATEWAY_PORT="${GATEWAY_PORT:-8080}"

port_of() {
  case "$1" in
    api-gateway)          echo "$GATEWAY_PORT" ;;
    identity-service)     echo 8081 ;;
    catalog-service)      echo 8082 ;;
    order-service)        echo 8083 ;;
    payment-service)      echo 8084 ;;
    notification-service) echo 8085 ;;
    *) echo "Service không tồn tại: $1" >&2; exit 1 ;;
  esac
}

jar_of()     { echo "$ROOT/$1/target/$1-0.0.1-SNAPSHOT.jar"; }
pid_file()   { echo "$RUN_DIR/$1.pid"; }

is_running() {
  local pf
  pf="$(pid_file "$1")"
  [[ -f "$pf" ]] && kill -0 "$(cat "$pf")" 2>/dev/null
}

is_up() { # $1 = service: trả về 0 nếu /actuator/health báo UP
  local body
  body="$(curl -s --max-time 2 "http://localhost:$(port_of "$1")/actuator/health" || true)"
  [[ "$body" == *'"status":"UP"'* ]]
}

port_in_use() { ss -ltnH "sport = :$(port_of "$1")" | grep -q .; }

wait_up() { # chờ tối đa 120 giây; thất bại ngay nếu tiến trình của script đã chết
  for _ in $(seq 1 120); do
    if ! is_running "$1"; then return 1; fi
    if is_up "$1"; then return 0; fi
    sleep 1
  done
  return 1
}

cmd_start() {
  local build=0
  [[ "${1:-}" == "--build" ]] && build=1

  echo "==> Bật hạ tầng (Postgres, Redis, RabbitMQ, Mailpit)"
  docker compose -f "$ROOT/docker-compose.yml" up -d --wait >/dev/null

  local need_build=$build
  for s in "${SERVICES[@]}"; do
    [[ -f "$(jar_of "$s")" ]] || need_build=1
  done
  if [[ $need_build -eq 1 ]]; then
    echo "==> Build (mvnw clean package -DskipTests)"
    ./mvnw -q -B clean package -DskipTests
  fi

  local started=()
  # identity trước: các service khác lấy public key JWT từ nó
  for s in "${SERVICES[@]}"; do
    if is_running "$s"; then
      echo "[$s] đang chạy (do script này bật), bỏ qua"
      continue
    fi
    rm -f "$(pid_file "$s")"
    if port_in_use "$s"; then
      echo "[$s] cổng $(port_of "$s") đang bị chiếm bởi tiến trình khác (IntelliJ?), bỏ qua" >&2
      continue
    fi
    echo "[$s] khởi động..."
    setsid java -jar "$(jar_of "$s")" >"$LOG_DIR/$s.log" 2>&1 </dev/null &
    echo $! > "$(pid_file "$s")"
    started+=("$s")
  done

  for s in "${started[@]}"; do
    if wait_up "$s"; then
      echo "[$s] UP  → http://localhost:$(port_of "$s")"
    else
      echo "[$s] KHÔNG LÊN ĐƯỢC. Xem log: scripts/run-all.sh logs $s" >&2
    fi
  done
}

start_one() { # $1 = service
  local s="$1"
  port_of "$s" >/dev/null
  if is_running "$s"; then echo "[$s] đang chạy"; return; fi
  if port_in_use "$s"; then echo "[$s] cổng $(port_of "$s") đang bị chiếm" >&2; return 1; fi
  [[ -f "$(jar_of "$s")" ]] || ./mvnw -q -B -pl "$s" -am package -DskipTests
  setsid java -jar "$(jar_of "$s")" >>"$LOG_DIR/$s.log" 2>&1 </dev/null &
  echo $! > "$(pid_file "$s")"
  if wait_up "$s"; then echo "[$s] UP"; else echo "[$s] KHÔNG LÊN ĐƯỢC" >&2; return 1; fi
}

stop_one() { # $1 = service
  local s="$1" pf pid
  pf="$(pid_file "$s")"
  if ! is_running "$s"; then rm -f "$pf"; echo "[$s] không chạy"; return; fi
  pid="$(cat "$pf")"
  echo "[$s] dừng (pid $pid)..."
  kill "$pid"
  for _ in $(seq 1 30); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
  kill -0 "$pid" 2>/dev/null && kill -9 "$pid"
  rm -f "$pf"
}

cmd_stop() {
  for s in "${SERVICES[@]}"; do
    local pf pid
    pf="$(pid_file "$s")"
    if ! is_running "$s"; then
      rm -f "$pf"
      echo "[$s] không chạy"
      continue
    fi
    pid="$(cat "$pf")"
    echo "[$s] dừng (pid $pid)..."
    kill "$pid"
    for _ in $(seq 1 30); do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    if kill -0 "$pid" 2>/dev/null; then
      echo "[$s] chưa tắt sau 30 giây, buộc dừng"
      kill -9 "$pid"
    fi
    rm -f "$pf"
  done
}

cmd_status() {
  local state
  for s in "${SERVICES[@]}"; do
    if is_running "$s" && is_up "$s"; then
      state="UP"
    elif is_running "$s"; then
      state="STARTING"
    elif is_up "$s"; then
      state="UP(ngoài)"     # đang chạy nhưng không do script bật, ví dụ IntelliJ
    else
      state="STOPPED"
    fi
    printf '%-22s %-12s http://localhost:%s\n' "$s" "$state" "$(port_of "$s")"
  done
}

cmd_logs() {
  if [[ -z "${1:-}" ]]; then
    echo "Dùng: scripts/run-all.sh logs <service>" >&2
    exit 1
  fi
  port_of "$1" >/dev/null
  tail -n 100 -f "$LOG_DIR/$1.log"
}

case "${1:-}" in
  start)  shift; cmd_start "${1:-}" ;;
  stop)   cmd_stop ;;
  status) cmd_status ;;
  logs)   cmd_logs "${2:-}" ;;
  start-one) start_one "${2:?Thiếu tên service}" ;;
  stop-one)  stop_one "${2:?Thiếu tên service}" ;;
  *)
    sed -n '2,14p' "$0"
    exit 1
    ;;
esac

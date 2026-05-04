#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/docker/docker-compose.e2e.yml"
LOG_DIR="${ROOT_DIR}/build/ci-e2e-logs"
VALID_JWT="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxMDAwMSJ9.gO06k1moG99ujial-2CkEtGQtkE-vKf59mgOKA9RScU"
AUTH_HEADER="Authorization: Bearer ${VALID_JWT}"
JSON_HEADER="Content-Type: application/json"

declare -a APP_PIDS=()

mkdir -p "${LOG_DIR}"

cleanup() {
  for pid in "${APP_PIDS[@]:-}"; do
    kill "${pid}" >/dev/null 2>&1 || true
  done
  docker compose -f "${COMPOSE_FILE}" down -v --remove-orphans >/dev/null 2>&1 || true
}

dump_diagnostics() {
  docker compose -f "${COMPOSE_FILE}" ps -a > "${LOG_DIR}/docker-compose-ps.log" 2>&1 || true
  docker compose -f "${COMPOSE_FILE}" logs > "${LOG_DIR}/docker-compose.log" 2>&1 || true
  for log_file in "${LOG_DIR}"/*.log; do
    [[ -f "${log_file}" ]] || continue
    echo "========== ${log_file} =========="
    tail -n 240 "${log_file}" || true
  done
}

on_exit() {
  local status=$?
  if [[ ${status} -ne 0 ]]; then
    dump_diagnostics
  fi
  cleanup
  exit "${status}"
}

trap on_exit EXIT

wait_for_port() {
  local host=$1
  local port=$2
  local name=$3
  local timeout_seconds=${4:-180}

  for ((i = 1; i <= timeout_seconds; i++)); do
    if (echo > "/dev/tcp/${host}/${port}") >/dev/null 2>&1; then
      echo "${name} is ready on ${host}:${port}"
      return 0
    fi
    sleep 1
  done

  echo "Timed out waiting for ${name} on ${host}:${port}"
  return 1
}

wait_for_http() {
  local url=$1
  local name=$2
  local timeout_seconds=${3:-180}

  for ((i = 1; i <= timeout_seconds; i++)); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "${name} is ready: ${url}"
      return 0
    fi
    sleep 1
  done

  echo "Timed out waiting for ${name}: ${url}"
  return 1
}

wait_for_nacos_service() {
  local service_name=$1
  local timeout_seconds=${2:-120}
  local url="http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=${service_name}"

  for ((i = 1; i <= timeout_seconds; i++)); do
    if curl -fsS "${url}" | python -c 'import json,sys; payload=json.load(sys.stdin); hosts=payload.get("hosts") or []; raise SystemExit(0 if any(host.get("healthy") for host in hosts) else 1)' >/dev/null 2>&1; then
      echo "Nacos service is healthy: ${service_name}"
      return 0
    fi
    sleep 1
  done

  echo "Timed out waiting for Nacos service: ${service_name}"
  return 1
}

wait_for_mysql() {
  for ((i = 1; i <= 180; i++)); do
    if docker exec velocity-e2e-mysql mysqladmin ping -uroot -proot --silent >/dev/null 2>&1; then
      echo "MySQL is ready"
      return 0
    fi
    sleep 1
  done

  echo "Timed out waiting for MySQL"
  return 1
}

mysql_exec() {
  docker exec -i velocity-e2e-mysql mysql -uroot -proot --default-character-set=utf8mb4 "$@"
}

mysql_scalar() {
  local query=$1
  docker exec velocity-e2e-mysql mysql -uroot -proot --default-character-set=utf8mb4 -D velocity_mall \
    -N -B -e "${query}" 2>/dev/null | head -n 1 | tr -d '\r'
}

wait_mysql_equals() {
  local query=$1
  local expected=$2
  local name=$3
  local timeout_seconds=${4:-90}
  local actual=""

  for ((i = 1; i <= timeout_seconds; i++)); do
    actual="$(mysql_scalar "${query}" || true)"
    if [[ "${actual}" == "${expected}" ]]; then
      echo "${name}: ${actual}"
      return 0
    fi
    sleep 1
  done

  echo "Expected ${name} to be '${expected}', got '${actual}'"
  return 1
}

wait_mysql_non_empty() {
  local query=$1
  local name=$2
  local timeout_seconds=${3:-90}
  local actual=""

  for ((i = 1; i <= timeout_seconds; i++)); do
    actual="$(mysql_scalar "${query}" || true)"
    if [[ -n "${actual}" && "${actual}" != "NULL" ]]; then
      echo "${name}: ${actual}" >&2
      printf '%s' "${actual}"
      return 0
    fi
    sleep 1
  done

  echo "Expected non-empty MySQL value for ${name}"
  return 1
}

redis_cli() {
  docker exec velocity-e2e-redis redis-cli "$@"
}

wait_redis_equals() {
  local name=$1
  local expected=$2
  shift 2
  local actual=""

  for ((i = 1; i <= 60; i++)); do
    actual="$(redis_cli "$@" | tr -d '\r' || true)"
    if [[ "${actual}" == "${expected}" ]]; then
      echo "${name}: ${actual}"
      return 0
    fi
    sleep 1
  done

  echo "Expected Redis ${name} to be '${expected}', got '${actual}'"
  return 1
}

request_status() {
  local method=$1
  local url=$2
  local output_file=$3
  shift 3

  curl -sS -X "${method}" -o "${output_file}" -w "%{http_code}" "$@" "${url}"
}

json_code() {
  local file=$1
  python - "${file}" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as f:
    print(json.load(f).get("code"))
PY
}

assert_json_code() {
  local file=$1
  local expected_code=$2

  python - "${file}" "${expected_code}" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as f:
    payload = json.load(f)
expected = int(sys.argv[2])
actual = payload.get("code")
if actual != expected:
    raise SystemExit(f"Expected JSON code {expected}, got {actual}. Payload: {payload}")
PY
}

assert_json_data_non_empty_list() {
  local file=$1
  local path=$2

  python - "${file}" "${path}" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as f:
    value = json.load(f)
for part in sys.argv[2].split("."):
    value = value[part]
if not isinstance(value, list) or not value:
    raise SystemExit(f"Expected non-empty list at {sys.argv[2]}, got: {value}")
PY
}

json_path() {
  local file=$1
  local path=$2

  python - "${file}" "${path}" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as f:
    value = json.load(f)
for part in sys.argv[2].split("."):
    value = value[part]
print(value)
PY
}

assert_success_response() {
  local file=$1
  assert_json_code "${file}" 20000
}

call_until_success() {
  local name=$1
  local method=$2
  local url=$3
  local output_file=$4
  shift 4
  local status=""
  local code=""

  for ((i = 1; i <= 60; i++)); do
    status="$(request_status "${method}" "${url}" "${output_file}" "$@" || true)"
    code="$(json_code "${output_file}" 2>/dev/null || true)"
    if [[ "${status}" == "200" && "${code}" == "20000" ]]; then
      echo "${name} passed"
      return 0
    fi
    echo "${name} not ready. attempt=${i}, http_status=${status}, code=${code}"
    cat "${output_file}" || true
    sleep 2
  done

  echo "${name} did not return HTTP 200 + Result code 20000"
  return 1
}

call_once_success() {
  local name=$1
  local method=$2
  local url=$3
  local output_file=$4
  shift 4
  local status

  status="$(request_status "${method}" "${url}" "${output_file}" "$@")"
  if [[ "${status}" != "200" ]]; then
    echo "${name} expected HTTP 200, got ${status}"
    cat "${output_file}" || true
    return 1
  fi
  assert_success_response "${output_file}"
  echo "${name} passed"
}

find_jar() {
  local module=$1
  find "${ROOT_DIR}/${module}/target" -maxdepth 1 -type f -name "${module}-*.jar" \
    ! -name "*sources*" ! -name "*javadoc*" | head -n 1
}

start_app() {
  local module=$1
  local name=$2
  local port=$3
  local jar
  jar="$(find_jar "${module}")"
  if [[ -z "${jar}" ]]; then
    echo "Cannot find jar for ${module}"
    return 1
  fi

  echo "Starting ${name} from ${jar}"
  java -jar "${jar}" > "${LOG_DIR}/${name}.log" 2>&1 &
  APP_PIDS+=("$!")
  wait_for_port 127.0.0.1 "${port}" "${name}" 240
}

seed_database() {
  mysql_exec <<'SQL'
DROP DATABASE IF EXISTS velocity_mall;
CREATE DATABASE velocity_mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE velocity_mall;

CREATE TABLE pms_category (
    id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(64) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    icon VARCHAR(255) DEFAULT NULL,
    level TINYINT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_status_sort (status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pms_spu (
    id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    publish_status TINYINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_publish_status (publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pms_sku (
    id BIGINT NOT NULL,
    spu_id BIGINT NOT NULL,
    sku_name VARCHAR(128) NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    price DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    stock INT NOT NULL DEFAULT 0,
    lock_stock INT NOT NULL DEFAULT 0,
    sale_count INT NOT NULL DEFAULT 0,
    cover_img VARCHAR(255) DEFAULT NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_spu_id (spu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pms_stock_lock_log (
    id BIGINT NOT NULL,
    order_sn VARCHAR(64) NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_sku (order_sn, sku_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mq_consume_log (
    id BIGINT NOT NULL,
    topic VARCHAR(128) NOT NULL,
    consumer_group VARCHAR(128) NOT NULL,
    order_sn VARCHAR(64) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_topic_group_order (topic, consumer_group, order_sn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE oms_order (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_sn VARCHAR(64) NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    pay_type INT DEFAULT NULL,
    pay_time DATETIME DEFAULT NULL,
    order_type INT NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 0,
    remark VARCHAR(500) DEFAULT NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_sn (order_sn),
    KEY idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE oms_order_item (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_sn VARCHAR(64) NOT NULL,
    spu_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    sku_name VARCHAR(128) NOT NULL,
    sku_pic VARCHAR(255) DEFAULT NULL,
    sku_price DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    sku_quantity INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order_sn (order_sn),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sms_coupon (
    id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    min_point DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    stock INT NOT NULL DEFAULT 0,
    limit_per_user INT NOT NULL DEFAULT 1,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sms_coupon_history (
    id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    claim_time DATETIME NOT NULL,
    use_status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_coupon_user (coupon_id, user_id),
    KEY idx_user_status (user_id, use_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO pms_category
(id, parent_id, name, sort, icon, level, status, create_time, update_time, is_deleted)
VALUES
    (1, 0, 'Digital', 1, 'https://static.velocitymall.local/category/digital.png', 1, 1, NOW(), NOW(), 0),
    (2, 1, 'Smart Phone', 1, 'https://static.velocitymall.local/category/phone.png', 2, 1, NOW(), NOW(), 0),
    (3, 2, 'Flagship Phone', 1, 'https://static.velocitymall.local/category/flagship.png', 3, 1, NOW(), NOW(), 0);

INSERT INTO pms_spu
(id, category_id, name, description, publish_status, version, create_time, update_time, is_deleted)
VALUES
    (1001, 3, 'Velocity Phone Pro', 'CI E2E flagship phone', 1, 0, NOW(), NOW(), 0);

INSERT INTO pms_sku
(id, spu_id, sku_name, sku_code, price, stock, lock_stock, sale_count, cover_img, version, create_time, update_time, is_deleted)
VALUES
    (2001, 1001, 'Velocity Phone Pro 512G', 'VM-PHONE-512G', 7999.00, 100, 0, 0, 'https://static.velocitymall.local/sku/2001.png', 0, NOW(), NOW(), 0);

INSERT INTO sms_coupon
(id, name, amount, min_point, stock, limit_per_user, start_time, end_time, status, version, create_time, update_time, is_deleted)
VALUES
    (3001, 'E2E Coupon', 100.00, 1000.00, 10, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), 1, 0, NOW(), NOW(), 0);
SQL

  redis_cli FLUSHALL >/dev/null
}

echo "Starting full-chain E2E infrastructure..."
docker compose -f "${COMPOSE_FILE}" up -d

wait_for_mysql
wait_for_port 127.0.0.1 6379 Redis 60
wait_for_port 127.0.0.1 8848 Nacos 180
wait_for_port 127.0.0.1 9876 RocketMQ-NameServer 120
wait_for_port 127.0.0.1 10911 RocketMQ-Broker 180
wait_for_http "http://127.0.0.1:9201" Elasticsearch 240
wait_for_port 127.0.0.1 9848 Nacos-gRPC 240
wait_for_http "http://127.0.0.1:8848/nacos/v1/console/health/readiness" Nacos-readiness 240

seed_database

echo "Starting VelocityMall applications..."
start_app velocity-mall-product product 8081
start_app velocity-mall-order order 8082
start_app velocity-mall-seckill seckill 8083
start_app velocity-mall-search search 8085
start_app velocity-mall-coupon coupon 8086
start_app velocity-mall-gateway gateway 8080

echo "Waiting for Nacos registrations to propagate..."
wait_for_nacos_service velocity-mall-product
wait_for_nacos_service velocity-mall-order
wait_for_nacos_service velocity-mall-seckill
wait_for_nacos_service velocity-mall-search
wait_for_nacos_service velocity-mall-coupon
sleep 5

category_response="${LOG_DIR}/category-tree.json"
call_until_success "category tree" GET "http://127.0.0.1:8080/api/v1/categories/tree" "${category_response}"
assert_json_data_non_empty_list "${category_response}" "data"

spu_response="${LOG_DIR}/spu-detail.json"
call_until_success "spu detail" GET "http://127.0.0.1:8080/api/v1/products/spus/1001" "${spu_response}"

sku_response="${LOG_DIR}/sku-detail.json"
call_until_success "sku detail" GET "http://127.0.0.1:8080/api/v1/products/skus/2001" "${sku_response}"

coupon_unauthorized="${LOG_DIR}/coupon-unauthorized.json"
coupon_unauthorized_status="$(request_status POST "http://127.0.0.1:8080/api/v1/coupons/3001/claim" "${coupon_unauthorized}")"
if [[ "${coupon_unauthorized_status}" != "401" ]]; then
  echo "Expected coupon claim without token HTTP 401, got ${coupon_unauthorized_status}"
  cat "${coupon_unauthorized}" || true
  exit 1
fi
assert_json_code "${coupon_unauthorized}" 40100

inner_response="${LOG_DIR}/product-inner-forbidden.json"
inner_status="$(request_status PUT "http://127.0.0.1:8080/api/v1/products/inner/skus/lock-batch" "${inner_response}" \
  -H "${AUTH_HEADER}" \
  -H "${JSON_HEADER}" \
  -d '{"orderSn":"E2E_FORBIDDEN","items":[{"skuId":2001,"quantity":1}]}')"
if [[ "${inner_status}" != "403" ]]; then
  echo "Expected product inner API HTTP 403, got ${inner_status}"
  cat "${inner_response}" || true
  exit 1
fi
assert_json_code "${inner_response}" 40300

coupon_claim="${LOG_DIR}/coupon-claim.json"
call_once_success "coupon claim" POST "http://127.0.0.1:8080/api/v1/coupons/3001/claim" "${coupon_claim}" \
  -H "${AUTH_HEADER}"
wait_mysql_equals "SELECT stock FROM sms_coupon WHERE id = 3001" "9" "coupon stock after first claim"
wait_mysql_equals "SELECT COUNT(1) FROM sms_coupon_history WHERE coupon_id = 3001 AND user_id = 10001" "1" "coupon history count"

coupon_repeat="${LOG_DIR}/coupon-repeat.json"
coupon_repeat_status="$(request_status POST "http://127.0.0.1:8080/api/v1/coupons/3001/claim" "${coupon_repeat}" \
  -H "${AUTH_HEADER}")"
if [[ "${coupon_repeat_status}" != "200" ]]; then
  echo "Expected repeat coupon claim HTTP 200 business warning, got ${coupon_repeat_status}"
  cat "${coupon_repeat}" || true
  exit 1
fi
assert_json_code "${coupon_repeat}" 50001
wait_mysql_equals "SELECT stock FROM sms_coupon WHERE id = 3001" "9" "coupon stock after repeat claim"

cart_add="${LOG_DIR}/cart-add.json"
call_once_success "cart add" POST "http://127.0.0.1:8080/api/v1/carts/items" "${cart_add}" \
  -H "${AUTH_HEADER}" \
  -H "${JSON_HEADER}" \
  -d '{"skuId":2001,"quantity":2}'

cart_list="${LOG_DIR}/cart-list.json"
call_until_success "cart list" GET "http://127.0.0.1:8080/api/v1/carts/items" "${cart_list}" \
  -H "${AUTH_HEADER}"
assert_json_data_non_empty_list "${cart_list}" "data"

order_create="${LOG_DIR}/normal-order-create.json"
call_once_success "normal order create" POST "http://127.0.0.1:8080/api/v1/orders" "${order_create}" \
  -H "${AUTH_HEADER}" \
  -H "${JSON_HEADER}" \
  -d '{"skuIds":[2001]}'
order_sn="$(json_path "${order_create}" "data.orderSn")"
echo "Normal order created: ${order_sn}"

wait_mysql_equals "SELECT lock_stock FROM pms_sku WHERE id = 2001" "2" "locked stock after normal order"
wait_mysql_equals "SELECT status FROM pms_stock_lock_log WHERE order_sn = '${order_sn}' AND sku_id = 2001" "0" "stock lock log after normal order"
wait_mysql_equals "SELECT COUNT(1) FROM oms_order_item WHERE order_sn = '${order_sn}' AND sku_name = 'Velocity Phone Pro 512G'" "1" "normal order item snapshot"

pay_response="${LOG_DIR}/normal-order-pay.json"
call_once_success "normal order pay" POST "http://127.0.0.1:8080/api/v1/orders/pay/mock?orderSn=${order_sn}&payType=1" "${pay_response}" \
  -H "${AUTH_HEADER}"
wait_mysql_equals "SELECT status FROM oms_order WHERE order_sn = '${order_sn}'" "1" "normal order paid status"
wait_mysql_equals "SELECT stock FROM pms_sku WHERE id = 2001" "98" "stock after payment"
wait_mysql_equals "SELECT lock_stock FROM pms_sku WHERE id = 2001" "0" "locked stock after payment"
wait_mysql_equals "SELECT sale_count FROM pms_sku WHERE id = 2001" "2" "sale count after payment"
wait_mysql_equals "SELECT status FROM pms_stock_lock_log WHERE order_sn = '${order_sn}' AND sku_id = 2001" "2" "stock lock log after payment"
wait_mysql_equals "SELECT COUNT(1) FROM mq_consume_log WHERE topic = 'payment-success-topic' AND consumer_group = 'payment-success-consumer-group' AND order_sn = '${order_sn}'" "1" "payment consume log"

refund_response="${LOG_DIR}/normal-order-refund.json"
call_once_success "normal order refund" POST "http://127.0.0.1:8080/api/v1/orders/${order_sn}/refund/mock" "${refund_response}" \
  -H "${AUTH_HEADER}"
wait_mysql_equals "SELECT status FROM oms_order WHERE order_sn = '${order_sn}'" "5" "normal order refunded status"
wait_mysql_equals "SELECT stock FROM pms_sku WHERE id = 2001" "100" "stock after refund"
wait_mysql_equals "SELECT sale_count FROM pms_sku WHERE id = 2001" "0" "sale count after refund"
wait_mysql_equals "SELECT COUNT(1) FROM mq_consume_log WHERE topic = 'order-refund-topic' AND consumer_group = 'order-refund-consumer-group' AND order_sn = '${order_sn}'" "1" "refund consume log"

redis_cli DEL velocitymall:seckill:stock:2001 velocitymall:seckill:bought:2001 >/dev/null
redis_cli SET velocitymall:seckill:stock:2001 5 >/dev/null
seckill_response="${LOG_DIR}/seckill-execute.json"
call_once_success "seckill execute" POST "http://127.0.0.1:8080/api/v1/seckill/execute/2001" "${seckill_response}" \
  -H "${AUTH_HEADER}"
wait_redis_equals "seckill stock after execute" "4" GET velocitymall:seckill:stock:2001
wait_redis_equals "seckill bought marker" "1" SISMEMBER velocitymall:seckill:bought:2001 10001
seckill_order_sn="$(wait_mysql_non_empty "SELECT order_sn FROM oms_order WHERE order_sn LIKE 'SEC_%' AND user_id = 10001 AND order_type = 1 LIMIT 1" "seckill order persisted" 120)"
wait_mysql_equals "SELECT status FROM oms_order WHERE order_sn = '${seckill_order_sn}'" "0" "seckill order wait-pay status"

rebuild_response="${LOG_DIR}/search-rebuild.json"
call_until_success "search index rebuild" POST "http://127.0.0.1:8085/api/v1/search/inner/skus/rebuild-index" "${rebuild_response}"

search_response="${LOG_DIR}/search-skus.json"
call_until_success "search skus" GET "http://127.0.0.1:8080/api/v1/search/skus?keyword=Phone&page=1&size=10" "${search_response}"
assert_json_data_non_empty_list "${search_response}" "data.records"

search_inner_forbidden="${LOG_DIR}/search-inner-forbidden.json"
search_inner_status="$(request_status POST "http://127.0.0.1:8080/api/v1/search/inner/skus/rebuild-index" "${search_inner_forbidden}" \
  -H "${AUTH_HEADER}")"
if [[ "${search_inner_status}" != "403" ]]; then
  echo "Expected search inner API HTTP 403, got ${search_inner_status}"
  cat "${search_inner_forbidden}" || true
  exit 1
fi
assert_json_code "${search_inner_forbidden}" 40300

echo "Full-chain E2E checks passed."

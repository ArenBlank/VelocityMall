#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/docker/docker-compose.e2e.yml"
LOG_DIR="${ROOT_DIR}/build/ci-e2e-logs"
JSON_HEADER="Content-Type: application/json"

# Will be populated after user registration and admin login
USER_TOKEN=""
USER_ID=""
USER_AUTH_HEADER=""
ADMIN_TOKEN=""
ADMIN_AUTH_HEADER=""

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
    if docker exec velocity-e2e-mysql mysqladmin ping -h 127.0.0.1 -uroot -proot --silent >/dev/null 2>&1; then
      echo "MySQL is ready"
      return 0
    fi
    sleep 1
  done

  echo "Timed out waiting for MySQL"
  return 1
}

mysql_exec() {
  docker exec -i velocity-e2e-mysql mysql -h 127.0.0.1 -uroot -proot --default-character-set=utf8mb4 "$@"
}

mysql_scalar() {
  local query=$1
  docker exec velocity-e2e-mysql mysql -h 127.0.0.1 -uroot -proot --default-character-set=utf8mb4 -D velocity_mall \
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
    try:
        value = value[int(part)]
    except (ValueError, TypeError):
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
    try:
        value = value[int(part)]
    except (ValueError, TypeError):
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

CREATE TABLE sms_seckill_activity (
    id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    spu_id BIGINT NOT NULL,
    activity_name VARCHAR(128) NOT NULL,
    seckill_price DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    original_price DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    seckill_stock INT NOT NULL DEFAULT 0,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_sku_status_time (sku_id, status, start_time, end_time),
    KEY idx_status_time (status, start_time, end_time)
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
    receiver_name VARCHAR(32) DEFAULT NULL,
    receiver_phone VARCHAR(20) DEFAULT NULL,
    receiver_province VARCHAR(32) DEFAULT NULL,
    receiver_city VARCHAR(32) DEFAULT NULL,
    receiver_region VARCHAR(32) DEFAULT NULL,
    receiver_detail_address VARCHAR(255) DEFAULT NULL,
    delivery_company VARCHAR(64) DEFAULT NULL,
    delivery_sn VARCHAR(64) DEFAULT NULL,
    delivery_time DATETIME DEFAULT NULL,
    receive_time DATETIME DEFAULT NULL,
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

INSERT INTO sms_seckill_activity
(id, sku_id, spu_id, activity_name, seckill_price, original_price, seckill_stock, start_time, end_time, status, version, create_time, update_time, is_deleted)
VALUES
    (23001, 2001, 1001, 'Velocity Phone Pro E2E Flash Sale', 4999.00, 7999.00, 100, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 YEAR), 1, 0, NOW(), NOW(), 0);

INSERT INTO sms_coupon
(id, name, amount, min_point, stock, limit_per_user, start_time, end_time, status, version, create_time, update_time, is_deleted)
VALUES
    (3001, 'E2E Coupon', 100.00, 1000.00, 10, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), 1, 0, NOW(), NOW(), 0);
CREATE TABLE ums_user (
    id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ums_user_address (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(32) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    province VARCHAR(32) NOT NULL,
    city VARCHAR(32) NOT NULL,
    region VARCHAR(32) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    is_default TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ums_admin (
    id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(64) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ums_admin (id, username, password, real_name, status)
VALUES (1, 'admin', '$2a$10$j/jaxAC8fXLIrZH361eoye3cvkCoPDPcDcCTcDJ7uphwG8h0.L0bS', 'E2E Admin', 1);

CREATE TABLE oms_product_review (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_sn VARCHAR(64) NOT NULL,
    sku_id BIGINT NOT NULL,
    spu_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    has_pictures TINYINT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    dislike_count INT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_spu_id (spu_id),
    UNIQUE KEY uk_user_order_sku (user_id, order_sn, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE oms_review_interaction (
    id BIGINT NOT NULL,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    interaction_type TINYINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_user (review_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
start_app velocity-mall-user user 8088
start_app velocity-mall-admin admin 8089
start_app velocity-mall-review review 8087

echo "Waiting for Nacos registrations to propagate..."
wait_for_nacos_service velocity-mall-product
wait_for_nacos_service velocity-mall-order
wait_for_nacos_service velocity-mall-seckill
wait_for_nacos_service velocity-mall-search
wait_for_nacos_service velocity-mall-coupon
wait_for_nacos_service velocity-mall-user
wait_for_nacos_service velocity-mall-admin
wait_for_nacos_service velocity-mall-review
sleep 5

echo "=== Setting up test accounts ==="

# Register C-end test user
USER_REGISTER="${LOG_DIR}/user-register.json"
call_once_success "user register" POST "http://127.0.0.1:8080/api/v1/users/register" "${USER_REGISTER}"   -H "${JSON_HEADER}"   -d '{"username":"e2euser","password":"e2epass123"}'

# Login to get user JWT
USER_LOGIN="${LOG_DIR}/user-login.json"
call_once_success "user login" POST "http://127.0.0.1:8080/api/v1/users/login" "${USER_LOGIN}"   -H "${JSON_HEADER}"   -d '{"username":"e2euser","password":"e2epass123"}'
USER_TOKEN="$(json_path "${USER_LOGIN}" "data.token")"
USER_ID="$(json_path "${USER_LOGIN}" "data.user.id")"
USER_AUTH_HEADER="Authorization: Bearer ${USER_TOKEN}"
echo "C-end user registered: id=${USER_ID}"

# Create shipping address
ADDRESS_RESP="${LOG_DIR}/user-address.json"
call_once_success "create address" POST "http://127.0.0.1:8080/api/v1/users/addresses" "${ADDRESS_RESP}"   -H "${USER_AUTH_HEADER}"   -H "${JSON_HEADER}"   -d '{"receiverName":"E2E Tester","receiverPhone":"13800138000","province":"Shanghai","city":"Shanghai","region":"Pudong","detailAddress":"No.1 Test Road","isDefault":1}'
ADDRESS_ID="$(json_path "${ADDRESS_RESP}" "data.id")"
echo "Shipping address created: id=${ADDRESS_ID}"

# Login admin to get admin JWT
ADMIN_LOGIN="${LOG_DIR}/admin-login.json"
call_once_success "admin login" POST "http://127.0.0.1:8080/api/v1/admin/login" "${ADMIN_LOGIN}"   -H "${JSON_HEADER}"   -d '{"username":"admin","password":"123456"}'
ADMIN_TOKEN="$(json_path "${ADMIN_LOGIN}" "data.token")"
ADMIN_AUTH_HEADER="Authorization: Bearer ${ADMIN_TOKEN}"
echo "Admin logged in"

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
  -H "${USER_AUTH_HEADER}" \
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
  -H "${USER_AUTH_HEADER}"
wait_mysql_equals "SELECT stock FROM sms_coupon WHERE id = 3001" "9" "coupon stock after first claim"
wait_mysql_equals "SELECT COUNT(1) FROM sms_coupon_history WHERE coupon_id = 3001 AND user_id = ${USER_ID}" "1" "coupon history count"

coupon_repeat="${LOG_DIR}/coupon-repeat.json"
coupon_repeat_status="$(request_status POST "http://127.0.0.1:8080/api/v1/coupons/3001/claim" "${coupon_repeat}" \
  -H "${USER_AUTH_HEADER}")"
if [[ "${coupon_repeat_status}" != "200" ]]; then
  echo "Expected repeat coupon claim HTTP 200 business warning, got ${coupon_repeat_status}"
  cat "${coupon_repeat}" || true
  exit 1
fi
assert_json_code "${coupon_repeat}" 50001
wait_mysql_equals "SELECT stock FROM sms_coupon WHERE id = 3001" "9" "coupon stock after repeat claim"

cart_add="${LOG_DIR}/cart-add.json"
call_once_success "cart add" POST "http://127.0.0.1:8080/api/v1/carts/items" "${cart_add}" \
  -H "${USER_AUTH_HEADER}" \
  -H "${JSON_HEADER}" \
  -d '{"skuId":2001,"quantity":1}'

cart_list="${LOG_DIR}/cart-list.json"
call_until_success "cart list" GET "http://127.0.0.1:8080/api/v1/carts/items" "${cart_list}" \
  -H "${USER_AUTH_HEADER}"
assert_json_data_non_empty_list "${cart_list}" "data"

# ============================================
# Main positive flow: order -> pay -> deliver -> confirm -> review
# ============================================
order_create="${LOG_DIR}/normal-order-create.json"
call_once_success "order create" POST "http://127.0.0.1:8080/api/v1/orders" "${order_create}" \
  -H "${USER_AUTH_HEADER}" \
  -H "${JSON_HEADER}" \
  -d "{\"skuIds\":[2001],\"addressId\":${ADDRESS_ID}}"
order_sn="$(json_path "${order_create}" "data.orderSn")"
echo "Order created: ${order_sn}"

wait_mysql_equals "SELECT lock_stock FROM pms_sku WHERE id = 2001" "1" "locked stock after order"
wait_mysql_equals "SELECT status FROM pms_stock_lock_log WHERE order_sn = '${order_sn}' AND sku_id = 2001" "0" "stock lock log after order"
wait_mysql_equals "SELECT COUNT(1) FROM oms_order_item WHERE order_sn = '${order_sn}' AND sku_name = 'Velocity Phone Pro 512G'" "1" "order item snapshot"

# Mock payment
pay_response="${LOG_DIR}/order-pay.json"
call_once_success "order pay" POST "http://127.0.0.1:8080/api/v1/orders/pay/mock?orderSn=${order_sn}&payType=1" "${pay_response}" \
  -H "${USER_AUTH_HEADER}"
wait_mysql_equals "SELECT status FROM oms_order WHERE order_sn = '${order_sn}'" "1" "order paid status"
wait_mysql_equals "SELECT stock FROM pms_sku WHERE id = 2001" "99" "stock after payment"
wait_mysql_equals "SELECT lock_stock FROM pms_sku WHERE id = 2001" "0" "locked stock after payment"
wait_mysql_equals "SELECT sale_count FROM pms_sku WHERE id = 2001" "1" "sale count after payment"
wait_mysql_equals "SELECT status FROM pms_stock_lock_log WHERE order_sn = '${order_sn}' AND sku_id = 2001" "2" "stock lock log after payment"
wait_mysql_equals "SELECT COUNT(1) FROM mq_consume_log WHERE topic = 'payment-success-topic' AND consumer_group = 'payment-success-consumer-group' AND order_sn = '${order_sn}'" "1" "payment consume log"

# Admin deliver (B-end operates on C-end order)
deliver_resp="${LOG_DIR}/admin-deliver.json"
call_once_success "admin deliver" POST "http://127.0.0.1:8080/api/v1/admin/orders/${order_sn}/deliver?deliveryCompany=SFExpress&deliverySn=SF9988776655" "${deliver_resp}" \
  -H "${ADMIN_AUTH_HEADER}"
wait_mysql_equals "SELECT status FROM oms_order WHERE order_sn = '${order_sn}'" "2" "order delivered status"
wait_mysql_equals "SELECT delivery_company FROM oms_order WHERE order_sn = '${order_sn}'" "SFExpress" "delivery company persisted"
wait_mysql_equals "SELECT delivery_sn FROM oms_order WHERE order_sn = '${order_sn}'" "SF9988776655" "delivery sn persisted"

# C-end confirm receipt
confirm_resp="${LOG_DIR}/confirm-receipt.json"
call_once_success "confirm receipt" PUT "http://127.0.0.1:8080/api/v1/orders/${order_sn}/confirm-receipt" "${confirm_resp}" \
  -H "${USER_AUTH_HEADER}"
wait_mysql_equals "SELECT status FROM oms_order WHERE order_sn = '${order_sn}'" "3" "order completed status"

# Allow Nacos routing cache to warm up before first Feign call from review -> order
echo "Waiting for Nacos routing propagation (Feign load-balancer cache)..."
sleep 10

# Create review (purchase verification via Feign -- order is completed, should pass)
review_create="${LOG_DIR}/review-create.json"
call_once_success "review create" POST "http://127.0.0.1:8080/api/v1/reviews" "${review_create}" \
  -H "${USER_AUTH_HEADER}" \
  -H "${JSON_HEADER}" \
  -d "{\"orderSn\":\"${order_sn}\",\"skuId\":2001,\"spuId\":1001,\"rating\":5,\"content\":\"Excellent product! Full-chain E2E verified.\"}"
echo "Review created for order: ${order_sn}"

# Verify review appears in product listing
review_list="${LOG_DIR}/review-list.json"
call_once_success "review list" GET "http://127.0.0.1:8080/api/v1/reviews/products/1001?page=1&size=10" "${review_list}"
assert_json_data_non_empty_list "${review_list}" "data.records"
review_id="$(json_path "${review_list}" "data.records.0.id")"
echo "Review ID: ${review_id}"

# Like the review
like_resp="${LOG_DIR}/review-like.json"
call_once_success "review like" POST "http://127.0.0.1:8080/api/v1/reviews/${review_id}/interaction" "${like_resp}" \
  -H "${USER_AUTH_HEADER}" \
  -H "${JSON_HEADER}" \
  -d '{"interactionType":1}'
wait_mysql_equals "SELECT like_count FROM oms_product_review WHERE id = ${review_id}" "1" "review like_count after like"

# Switch like to dislike (verify toggle works)
dislike_resp="${LOG_DIR}/review-dislike.json"
call_once_success "review dislike (toggle)" POST "http://127.0.0.1:8080/api/v1/reviews/${review_id}/interaction" "${dislike_resp}" \
  -H "${USER_AUTH_HEADER}" \
  -H "${JSON_HEADER}" \
  -d '{"interactionType":2}'
wait_mysql_equals "SELECT like_count FROM oms_product_review WHERE id = ${review_id}" "0" "review like_count after toggle to dislike"
wait_mysql_equals "SELECT dislike_count FROM oms_product_review WHERE id = ${review_id}" "1" "review dislike_count after toggle"

# Anonymous review stats (no auth header)
stats_resp="${LOG_DIR}/review-stats-anon.json"
stats_status="$(request_status GET "http://127.0.0.1:8080/api/v1/reviews/products/1001/stats" "${stats_resp}")"
if [[ "${stats_status}" != "200" ]]; then
  echo "Expected review stats HTTP 200, got ${stats_status}"
  cat "${stats_resp}" || true
  exit 1
fi
assert_success_response "${stats_resp}"

# Delete the review
call_once_success "review delete" DELETE "http://127.0.0.1:8080/api/v1/reviews/${review_id}" "${LOG_DIR}/review-delete.json" \
  -H "${USER_AUTH_HEADER}"

# ============================================
# Refund test: separate order for reverse flow
# ============================================
cart_add2="${LOG_DIR}/cart-add-refund.json"
call_once_success "cart add (refund test)" POST "http://127.0.0.1:8080/api/v1/carts/items" "${cart_add2}" \
  -H "${USER_AUTH_HEADER}" \
  -H "${JSON_HEADER}" \
  -d '{"skuId":2001,"quantity":1}'

order_create2="${LOG_DIR}/order-refund-test.json"
call_once_success "order create (refund test)" POST "http://127.0.0.1:8080/api/v1/orders" "${order_create2}" \
  -H "${USER_AUTH_HEADER}" \
  -H "${JSON_HEADER}" \
  -d "{\"skuIds\":[2001],\"addressId\":${ADDRESS_ID}}"
order_sn_rf="$(json_path "${order_create2}" "data.orderSn")"
echo "Refund test order: ${order_sn_rf}"

pay_rf="${LOG_DIR}/order-refund-pay.json"
call_once_success "refund order pay" POST "http://127.0.0.1:8080/api/v1/orders/pay/mock?orderSn=${order_sn_rf}&payType=1" "${pay_rf}" \
  -H "${USER_AUTH_HEADER}"
wait_mysql_equals "SELECT status FROM oms_order WHERE order_sn = '${order_sn_rf}'" "1" "refund order paid status"

refund_response="${LOG_DIR}/order-refund.json"
call_once_success "order refund" POST "http://127.0.0.1:8080/api/v1/orders/${order_sn_rf}/refund/mock" "${refund_response}" \
  -H "${USER_AUTH_HEADER}"
wait_mysql_equals "SELECT status FROM oms_order WHERE order_sn = '${order_sn_rf}'" "5" "order refunded status"
wait_mysql_equals "SELECT stock FROM pms_sku WHERE id = 2001" "99" "stock after refund"
wait_mysql_equals "SELECT sale_count FROM pms_sku WHERE id = 2001" "1" "sale count after refund"
wait_mysql_equals "SELECT COUNT(1) FROM mq_consume_log WHERE topic = 'order-refund-topic' AND consumer_group = 'order-refund-consumer-group' AND order_sn = '${order_sn_rf}'" "1" "refund consume log"

# ============================================
# Seckill
# ============================================
redis_cli DEL velocitymall:seckill:stock:2001 velocitymall:seckill:bought:2001 >/dev/null
redis_cli SET velocitymall:seckill:stock:2001 5 >/dev/null
seckill_response="${LOG_DIR}/seckill-execute.json"
call_once_success "seckill execute" POST "http://127.0.0.1:8080/api/v1/seckill/execute/2001" "${seckill_response}" \
  -H "${USER_AUTH_HEADER}"
wait_redis_equals "seckill stock after execute" "4" GET velocitymall:seckill:stock:2001
wait_redis_equals "seckill bought marker" "1" SISMEMBER velocitymall:seckill:bought:2001 ${USER_ID}
seckill_order_sn="$(wait_mysql_non_empty "SELECT order_sn FROM oms_order WHERE order_sn LIKE 'SEC_%' AND user_id = ${USER_ID} AND order_type = 1 LIMIT 1" "seckill order persisted" 120)"
wait_mysql_equals "SELECT status FROM oms_order WHERE order_sn = '${seckill_order_sn}'" "0" "seckill order wait-pay status"

# ============================================
# Search
# ============================================
rebuild_response="${LOG_DIR}/search-rebuild.json"
call_until_success "search index rebuild" POST "http://127.0.0.1:8085/api/v1/search/inner/skus/rebuild-index" "${rebuild_response}"

search_response="${LOG_DIR}/search-skus.json"
call_until_success "search skus" GET "http://127.0.0.1:8080/api/v1/search/skus?keyword=Phone&page=1&size=10" "${search_response}"
assert_json_data_non_empty_list "${search_response}" "data.records"

search_inner_forbidden="${LOG_DIR}/search-inner-forbidden.json"
search_inner_status="$(request_status POST "http://127.0.0.1:8080/api/v1/search/inner/skus/rebuild-index" "${search_inner_forbidden}" \
  -H "${USER_AUTH_HEADER}")"
if [[ "${search_inner_status}" != "403" ]]; then
  echo "Expected search inner API HTTP 403, got ${search_inner_status}"
  cat "${search_inner_forbidden}" || true
  exit 1
fi
assert_json_code "${search_inner_forbidden}" 40300

echo "Full-chain E2E checks passed."

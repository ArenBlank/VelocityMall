"""
极限压测 初始化与清理服务

端口: 8099
依赖: pip install flask pymysql redis

接口:
  POST /setup   — 重置 SKU 2001 库存为 1000，清理历史订单
  POST /cleanup — 清空 lt_u 开头用户，清空订单，清理 Redis 秒杀 Key
  GET  /health  — 健康检查
"""
import redis
import pymysql
from flask import Flask, jsonify

app = Flask(__name__)

MYSQL_HOST = "127.0.0.1"
MYSQL_PORT = 3306
MYSQL_USER = "root"
MYSQL_PASS = "root"
MYSQL_DB = "velocity_mall"

REDIS_HOST = "127.0.0.1"
REDIS_PORT = 6379

SKU_ID = 2001
SECKILL_STOCK = 1000
USER_PREFIX = "lt_u"


def get_mysql_conn():
    return pymysql.connect(
        host=MYSQL_HOST, port=MYSQL_PORT,
        user=MYSQL_USER, password=MYSQL_PASS,
        database=MYSQL_DB, charset="utf8mb4",
        autocommit=True,
    )


def get_redis_client():
    return redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)


@app.route("/setup", methods=["POST"])
def do_setup():
    result = {"steps": []}

    # 1. 重置 SKU 库存
    try:
        conn = get_mysql_conn()
        cur = conn.cursor()
        cur.execute(
            "UPDATE pms_sku SET stock = %s, lock_stock = 0 WHERE id = %s",
            (SECKILL_STOCK, SKU_ID),
        )
        cur.close()
        conn.close()
        result["steps"].append({"step": "reset_sku_stock", "ok": True, "msg": f"stock={SECKILL_STOCK}"})
    except Exception as e:
        result["steps"].append({"step": "reset_sku_stock", "ok": False, "msg": str(e)})

    # 2. 清理历史订单
    try:
        conn = get_mysql_conn()
        cur = conn.cursor()
        cur.execute("DELETE FROM oms_order_item")
        cur.execute("DELETE FROM oms_order")
        cur.execute("DELETE FROM pms_stock_lock_log")
        cur.execute("DELETE FROM mq_consume_log")
        cur.execute("DELETE FROM oms_product_review")
        cur.execute("DELETE FROM oms_review_interaction")
        cur.close()
        conn.close()
        result["steps"].append({"step": "clean_orders", "ok": True, "msg": "orders cleared"})
    except Exception as e:
        result["steps"].append({"step": "clean_orders", "ok": False, "msg": str(e)})

    # 3. 预热 Redis 秒杀库存
    try:
        r = get_redis_client()
        r.set(f"velocitymall:seckill:stock:{SKU_ID}", SECKILL_STOCK)
        r.delete(f"velocitymall:seckill:bought:{SKU_ID}")
        result["steps"].append({"step": "warmup_redis", "ok": True, "msg": f"stock_key={SECKILL_STOCK}"})
    except Exception as e:
        result["steps"].append({"step": "warmup_redis", "ok": False, "msg": str(e)})

    return jsonify({"code": 20000, "message": "setup done", "data": result})


@app.route("/cleanup", methods=["POST"])
def do_cleanup():
    result = {"steps": []}

    # 1. 清空测试用户
    try:
        conn = get_mysql_conn()
        cur = conn.cursor()
        cur.execute(f"DELETE FROM ums_user WHERE username LIKE '{USER_PREFIX}%%'")
        n = cur.rowcount
        cur.close()
        conn.close()
        result["steps"].append({"step": "clean_users", "ok": True, "msg": f"deleted {n}"})
    except Exception as e:
        result["steps"].append({"step": "clean_users", "ok": False, "msg": str(e)})

    # 2. 清空订单
    try:
        conn = get_mysql_conn()
        cur = conn.cursor()
        cur.execute("DELETE FROM oms_order_item")
        cur.execute("DELETE FROM oms_order")
        cur.execute("DELETE FROM pms_stock_lock_log")
        cur.execute("DELETE FROM mq_consume_log")
        cur.close()
        conn.close()
        result["steps"].append({"step": "clean_orders", "ok": True, "msg": "cleared"})
    except Exception as e:
        result["steps"].append({"step": "clean_orders", "ok": False, "msg": str(e)})

    # 3. 清理 Redis 秒杀 Key
    try:
        r = get_redis_client()
        for key in r.scan_iter("velocitymall:seckill:*"):
            r.delete(key)
        for key in r.scan_iter("velocitymall:order:submit-token:*"):
            r.delete(key)
        result["steps"].append({"step": "clean_redis", "ok": True, "msg": "cleared"})
    except Exception as e:
        result["steps"].append({"step": "clean_redis", "ok": False, "msg": str(e)})

    return jsonify({"code": 20000, "message": "cleanup done", "data": result})


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    print("VelocityMall cleanup server — port 8099")
    app.run(host="0.0.0.0", port=8099, debug=False)

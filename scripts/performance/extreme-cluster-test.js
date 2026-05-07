import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// ============================================================
// VelocityMall 3 节点集群 极限压测 — 1500 VU
// 目标：Nginx:80 → Gateway×3 → (Nacos lb) → Seckill×3
// 前置：python generate_users.py (生成 extreme_users.json, 2000+ tokens)
// ============================================================

// 读取预生成的用户 token（需先执行 generate_users.py 生成 extreme_users.json）
const users = new SharedArray('users', function () {
  return JSON.parse(open('./extreme_users.json'));
});

const seckillSuccess = new Counter('seckill_success');
const seckillSoldOut = new Counter('seckill_sold_out');
const seckillDuplicate = new Counter('seckill_duplicate');
const seckillRateLimited = new Counter('seckill_rate_limited');
const seckillFailed = new Counter('seckill_failed');
const responseTime = new Trend('seckill_response_time_ms', true);

const isCI = __ENV.CI === 'true';
const CLEANUP_URL = 'http://127.0.0.1:8099';
const SKU_ID = '2001';
// 绕过 Nginx（高并发下 host.docker.internal 失效），直连 3 个 Gateway 轮询
const GATEWAYS = ['http://127.0.0.1:8080', 'http://127.0.0.1:8090', 'http://127.0.0.1:8091'];
function getTarget() {
  return GATEWAYS[Math.floor(Math.random() * GATEWAYS.length)] + '/api/v1/seckill/execute/' + SKU_ID;
}

export const options = {
  stages: isCI
    ? [
        { duration: '2s',  target: 30 },
        { duration: '5s',  target: 30 },
        { duration: '2s',  target: 0  },
      ]
    : [
        { duration: '10s', target: 1500 },
        { duration: '30s', target: 1500 },
        { duration: '10s', target: 0    },
      ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    http_req_failed:   ['rate<0.50'],
  },
  batch: 50,
  batchPerHost: 50,
};

// ---- setup：初始化库存 + 清理数据 ----
export function setup() {
  console.log('[setup] 调用 /setup 初始化极限压测数据...');
  const res = http.post(CLEANUP_URL + '/setup', '{}', {
    headers: { 'Content-Type': 'application/json' },
    timeout: '30s',
  });
  const data = JSON.parse(res.body);
  console.log('[setup] code=' + data.code);
  for (const s of (data.data && data.data.steps) || []) {
    console.log(`[setup]   ${s.step}: ${s.ok ? 'OK' : 'FAIL'} — ${s.msg}`);
  }
  return { userCount: users.length };
}

// ---- default：核心压测 ----
export default function (data) {
  if (!users || users.length === 0) {
    seckillFailed.add(1);
    return;
  }

  const user = users[Math.floor(Math.random() * users.length)];
  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${user.token}`,
    },
    timeout: '10s',
  };

  const start = Date.now();
  let res;
  try {
    res = http.post(getTarget(), null, params);
  } catch (_) {
    seckillFailed.add(1);
    responseTime.add(Date.now() - start);
    return;
  }
  responseTime.add(Date.now() - start);

  if (res.status === 200) {
    let body;
    try { body = JSON.parse(res.body); } catch (_) { seckillFailed.add(1); return; }

    const code = body.code;
    const msg = body.message || '';

    if (code === 20000) {
      seckillSuccess.add(1);
    } else if (msg.includes('库存不足') || msg.includes('已抢光') || msg.includes('售罄')) {
      seckillSoldOut.add(1);
    } else if (msg.includes('已抢过') || msg.includes('请勿重复')) {
      seckillDuplicate.add(1);
    } else {
      seckillFailed.add(1);
    }
  } else if (res.status === 429) {
    seckillRateLimited.add(1);
  } else {
    seckillFailed.add(1);
  }

  sleep(0.05);
}

// ---- teardown：清理 ----
export function teardown(data) {
  console.log('[teardown] 调用 /cleanup 清理...');
  const res = http.post(CLEANUP_URL + '/cleanup', '{}', {
    headers: { 'Content-Type': 'application/json' },
    timeout: '30s',
  });
  const d = JSON.parse(res.body);
  console.log('[teardown] code=' + d.code);
  for (const s of (d.data && d.data.steps) || []) {
    console.log(`[teardown]   ${s.step}: ${s.ok ? 'OK' : 'FAIL'} — ${s.msg}`);
  }
  console.log(`[result] success=${seckillSuccess.Value} soldOut=${seckillSoldOut.Value} dup=${seckillDuplicate.Value} limited=${seckillRateLimited.Value} failed=${seckillFailed.Value}`);
}

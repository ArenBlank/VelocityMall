import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// Custom metrics
const seckillSuccess = new Counter('seckill_success');
const seckillDuplicate = new Counter('seckill_duplicate');
const seckillRateLimited = new Counter('seckill_rate_limited');
const seckillFailed = new Counter('seckill_failed');
const responseTime = new Trend('seckill_response_time_ms');

const isCI = __ENV.CI === 'true';

export const options = {
  insecureSkipTLSVerify: true,
  stages: isCI
    ? [
        { duration: '1s', target: 20 },
        { duration: '3s', target: 20 },
        { duration: '1s', target: 0  },
      ]
    : [
        { duration: '5s',  target: 500  },
        { duration: '10s', target: 1000 },
        { duration: '5s',  target: 0    },
      ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed:   ['rate<0.50'],
  },
};

export function setup() {
  const loginUrl = 'http://127.0.0.1:8080/api/v1/users/login';
  const payload = JSON.stringify({
    username: 'e2euser',
    password: 'e2epass123',
  });
  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const res = http.post(loginUrl, payload, params);
  check(res, {
    'setup login HTTP 200': (r) => r.status === 200,
  });

  const body = JSON.parse(res.body);
  check(body, {
    'setup login code 20000': (b) => b.code === 20000,
  });

  const token = body.data.token;
  console.log(`Token acquired: ${token.substring(0, 20)}...`);
  return { token };
}

export default function (data) {
  const url = 'http://127.0.0.1:8080/api/v1/seckill/execute/2001';
  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.token}`,
    },
  };

  const start = Date.now();
  const res = http.post(url, null, params);
  responseTime.add(Date.now() - start);

  const status200 = res.status === 200;
  const status429 = res.status === 429;

  check(res, {
    'HTTP 状态是 200 (请求到达后台)':   () => status200,
    'HTTP 状态是 429 (被 Sentinel 限流拦截)': () => status429,
  });

  if (status200) {
    let body;
    try {
      body = JSON.parse(res.body);
    } catch (_) {
      seckillFailed.add(1);
      return;
    }

    const isSuccess = body.code === 20000;
    const isDuplicate = typeof body.message === 'string' && body.message.includes('已抢过');

    check(body, {
      '业务 code 为 20000 (抢购成功)': () => isSuccess,
      'message 包含 "已抢过" (防重拦截)': () => isDuplicate,
    });

    if (isSuccess) {
      seckillSuccess.add(1);
    } else if (isDuplicate) {
      seckillDuplicate.add(1);
    } else {
      seckillFailed.add(1);
      console.warn(`Unexpected response: ${res.body}`);
    }
  } else if (status429) {
    seckillRateLimited.add(1);
  } else {
    seckillFailed.add(1);
    console.warn(`Unexpected HTTP status ${res.status}: ${res.body}`);
  }

  sleep(0.1);
}

export function teardown() {
  console.log('Load test completed.');
}

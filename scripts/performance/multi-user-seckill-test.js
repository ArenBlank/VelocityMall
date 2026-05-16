import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 200 }, 429));

// Load 200 user tokens
const users = new SharedArray('users', function () {
  return JSON.parse(open('./users.json'));
});

// Custom metrics
const seckillSuccess = new Counter('seckill_success');
const seckillSoldOut = new Counter('seckill_sold_out');
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
        { duration: '5s',  target: 200 },
        { duration: '15s', target: 200 },
        { duration: '5s',  target: 0   },
      ],
  thresholds: isCI
    ? {
        http_req_duration: ['p(95)<5000'],
        http_req_failed:   ['rate<0.05'],
      }
    : {
        http_req_duration: ['p(95)<2000'],
        http_req_failed:   ['rate<0.50'],
      },
};

export default function () {
  // Randomly pick a user token — each VU iteration uses a different user
  const user = users[Math.floor(Math.random() * users.length)];

  const url = 'http://127.0.0.1:8080/api/v1/seckill/execute/2001';
  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${user.token}`,
    },
  };

  const start = Date.now();
  const res = http.post(url, null, params);
  responseTime.add(Date.now() - start);

  const status200 = res.status === 200;
  const status429 = res.status === 429;

  check(res, {
    'HTTP 200 or expected Sentinel 429': () => status200 || status429,
  });

  if (status200) {
    let body;
    try {
      body = JSON.parse(res.body);
    } catch (_) {
      seckillFailed.add(1);
      return;
    }

    const code = body.code;
    const msg = body.message || '';

    const isSuccess = code === 20000;
    const isSoldOut = code === 50002 || msg.includes('库存不足') || msg.includes('已抢光') || msg.includes('售罄');
    const isDuplicate = msg.includes('已抢过') || msg.includes('请勿重复');

    check(body, {
      'recognized seckill business result': () => isSuccess || isSoldOut || isDuplicate,
    });

    if (isSuccess) {
      seckillSuccess.add(1);
    } else if (isSoldOut) {
      seckillSoldOut.add(1);
    } else if (isDuplicate) {
      seckillDuplicate.add(1);
    } else {
      seckillFailed.add(1);
    }
  } else if (status429) {
    seckillRateLimited.add(1);
  } else {
    seckillFailed.add(1);
  }

  sleep(0.1);
}

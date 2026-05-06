import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// Custom metrics
const normalCount = new Counter('sku_normal');
const fallbackCount = new Counter('sku_fallback');
const errorCount = new Counter('sku_error');
const responseTime = new Trend('sku_response_time_ms');

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
        { duration: '2s',  target: 500 },
        { duration: '10s', target: 500 },
        { duration: '2s',  target: 0   },
      ],
  thresholds: {
    http_req_duration: ['p(95)<10000'],
    http_req_failed:   ['rate<0.30'],
  },
};

export default function () {
  const url = 'http://127.0.0.1:8080/api/v1/products/skus/2001';
  const params = {
    headers: { 'Content-Type': 'application/json' },
    timeout: '8s',
  };

  const start = Date.now();
  let res;
  try {
    res = http.get(url, params);
  } catch (e) {
    errorCount.add(1);
    sleep(0.05);
    return;
  }
  responseTime.add(Date.now() - start);

  check(res, {
    'HTTP 200': (r) => r.status === 200,
  });

  if (res.status === 200) {
    let body;
    try {
      body = JSON.parse(res.body);
    } catch (_) {
      errorCount.add(1);
      sleep(0.05);
      return;
    }

    const msg = body.message || '';
    const isFallback = msg.includes('系统降级保护生效');

    check(body, {
      '正常返回商品数据': () => body.code === 20000 && body.data != null,
      'Sentinel 降级保护生效': () => isFallback,
    });

    if (body.code === 20000 && body.data != null) {
      normalCount.add(1);
    } else if (isFallback) {
      fallbackCount.add(1);
      console.log(`FALLBACK: ${msg}`);
    } else {
      errorCount.add(1);
    }
  } else {
    errorCount.add(1);
    console.warn(`HTTP ${res.status}: ${res.body.substring(0, 200)}`);
  }

  sleep(0.05);
}

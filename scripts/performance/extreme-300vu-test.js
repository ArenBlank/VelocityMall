import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const users = new SharedArray('users', function () {
  return JSON.parse(open('./extreme_users.json'));
});

const seckillSuccess = new Counter('seckill_success');
const seckillSoldOut = new Counter('seckill_sold_out');
const seckillDuplicate = new Counter('seckill_duplicate');
const seckillRateLimited = new Counter('seckill_rate_limited');
const seckillFailed = new Counter('seckill_failed');
const responseTime = new Trend('seckill_response_time_ms', true);

export const options = {
  stages: [
    { duration: '5s',  target: 300 },
    { duration: '20s', target: 300 },
    { duration: '5s',  target: 0   },
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed:   ['rate<0.20'],
  },
};

export default function () {
  const user = users[Math.floor(Math.random() * users.length)];
  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${user.token}`,
    },
    timeout: '8s',
  };

  const start = Date.now();
  const res = http.post('http://127.0.0.1:8080/api/v1/seckill/execute/2001', null, params);
  responseTime.add(Date.now() - start);

  if (res.status === 200) {
    let body;
    try { body = JSON.parse(res.body); } catch (_) { seckillFailed.add(1); return; }
    const code = body.code;
    const msg = body.message || '';
    if (code === 20000) seckillSuccess.add(1);
    else if (msg.includes('库存不足') || msg.includes('已抢光') || msg.includes('售罄')) seckillSoldOut.add(1);
    else if (msg.includes('已抢过') || msg.includes('请勿重复')) seckillDuplicate.add(1);
    else seckillFailed.add(1);
  } else if (res.status === 429) {
    seckillRateLimited.add(1);
  } else {
    seckillFailed.add(1);
  }
  sleep(0.05);
}

export function handleSummary(data) {
  console.log('========================================');
  console.log('  300 VU 极限压测报告');
  console.log('========================================');
  console.log(`总请求数:       ${data.metrics.http_reqs.values.count}`);
  console.log(`吞吐量:         ${data.metrics.http_reqs.values.rate.toFixed(0)} req/s`);
  console.log(`秒杀成功:       ${data.metrics.seckill_success?.values?.count || 0}`);
  console.log(`已售罄:         ${data.metrics.seckill_sold_out?.values?.count || 0}`);
  console.log(`重复抢购:       ${data.metrics.seckill_duplicate?.values?.count || 0}`);
  console.log(`限流(429):      ${data.metrics.seckill_rate_limited?.values?.count || 0}`);
  console.log(`失败:           ${data.metrics.seckill_failed?.values?.count || 0}`);
  console.log(`HTTP错误率:     ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`);
  console.log(`p50延迟:        ${data.metrics.http_req_duration.values.p(50).toFixed(1)}ms`);
  console.log(`p95延迟:        ${data.metrics.http_req_duration.values.p(95).toFixed(1)}ms`);
  console.log(`p99延迟:        ${data.metrics.http_req_duration.values.p(99).toFixed(1)}ms`);
  console.log(`最大延迟:       ${data.metrics.http_req_duration.values.max.toFixed(1)}ms`);
  console.log('========================================');
  return { stdout: [] };
}

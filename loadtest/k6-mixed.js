// NFR-PERF-005: 500 request/giây trong 15 phút, workload mix:
//   55% tìm kiếm/duyệt catalog · 25% xem chi tiết · 15% xem giỏ hàng · 5% thêm vào giỏ
// Ngưỡng theo NFR-PERF-001/002: p95 đọc <= 500ms, p99 <= 1000ms; lỗi < 1%.
// Chạy:  k6 run -e BASE=http://localhost:8080 loadtest/k6-mixed.js
//        (rút gọn để thử: -e RATE=100 -e DURATION=1m)
import http from 'k6/http';
import { check } from 'k6';

const data = JSON.parse(open('./data.json'));
const BASE = (__ENV.BASE || 'http://localhost:8080') + '/api/v1';
const RATE = Number(__ENV.RATE || 500);

export const options = {
  scenarios: {
    mixed: {
      executor: 'constant-arrival-rate',
      rate: RATE, timeUnit: '1s',
      duration: __ENV.DURATION || '15m',
      preAllocatedVUs: 200, maxVUs: 1000,
    },
  },
  thresholds: {
    'http_req_duration{kind:read}': ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{kind:write}': ['p(95)<1000', 'p(99)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

const terms = ['load', 'ao', 'dien thoai', 'normal', 'flash', 'giay'];

export default function () {
  const user = data.users[Math.floor(Math.random() * data.users.length)];
  const auth = { headers: { Authorization: `Bearer ${user.token}`, 'Content-Type': 'application/json' } };
  const r = Math.random();
  let res;
  if (r < 0.55) {
    const q = terms[Math.floor(Math.random() * terms.length)];
    res = http.get(`${BASE}/products?q=${encodeURIComponent(q)}&page=0&size=20`, { tags: { kind: 'read' } });
  } else if (r < 0.80) {
    res = http.get(`${BASE}/categories`, { tags: { kind: 'read' } });
  } else if (r < 0.95) {
    res = http.get(`${BASE}/cart`, Object.assign({ tags: { kind: 'read' } }, auth));
  } else {
    res = http.put(`${BASE}/cart/items/${data.normalSku}`, JSON.stringify({ quantity: 1 }),
      Object.assign({ tags: { kind: 'write' } }, auth));
    if (res.status === 404) {   // chưa có trong giỏ => thêm mới
      res = http.post(`${BASE}/cart/items`, JSON.stringify({ skuId: data.normalSku, quantity: 1 }),
        Object.assign({ tags: { kind: 'write' } }, auth));
    }
  }
  check(res, { 'status < 400': (x) => x.status < 400 });
}

// UC-05 / NFR-PERF-006: mọi khách CÙNG LÚC mua 1 SKU tồn ít.
// Mong đợi: số đơn tạo thành công <= tồn ban đầu, phần còn lại nhận 409 INVENTORY_INSUFFICIENT, không lỗi 5xx.
// Chạy:  k6 run -e BASE=http://localhost:8080 loadtest/k6-flash-sale.js
// Sau đó kiểm tra kho: GET /api/v1/admin/inventory/stocks/<flashSku> => available >= 0, reserved = số đơn thắng.
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import exec from 'k6/execution';

const data = JSON.parse(open('./data.json'));
const BASE = (__ENV.BASE || 'http://localhost:8080') + '/api/v1';
const won = new Counter('orders_won');
const soldOut = new Counter('orders_sold_out');
const unexpected = new Counter('orders_unexpected');

export const options = {
  scenarios: {
    rush: { executor: 'per-vu-iterations', vus: data.users.length, iterations: 1, maxDuration: '2m' },
  },
  thresholds: {
    orders_unexpected: ['count==0'],
    [`orders_won`]: [`count<=${data.flashStock}`],
  },
};

export default function () {
  const user = data.users[exec.vu.idInTest - 1];
  const headers = { Authorization: `Bearer ${user.token}`, 'Content-Type': 'application/json' };
  http.del(`${BASE}/cart`, null, { headers });
  http.post(`${BASE}/cart/items`, JSON.stringify({ skuId: data.flashSku, quantity: 1 }), { headers });
  const res = http.post(`${BASE}/orders`, JSON.stringify({ addressId: user.addressId, paymentMethod: 'MOCK' }),
    { headers: Object.assign({ 'Idempotency-Key': `flash-${exec.vu.idInTest}-${Date.now()}` }, headers) });
  if (res.status === 201) won.add(1);
  else if (res.status === 409) soldOut.add(1);
  else unexpected.add(1);
  check(res, { 'thắng (201) hoặc hết hàng (409)': (r) => r.status === 201 || r.status === 409 });
}

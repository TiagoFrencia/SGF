import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const saleLatency = new Trend('sale_latency_ms');

export const options = {
  scenarios: {
    // Scenario 1: Steady-state POS load (100 tx/s target)
    pos_checkout: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 200,
      maxVUs: 400,
      exec: 'posCheckout',
    },
    // Scenario 2: Inventory queries (concurrent reads)
    inventory_queries: {
      executor: 'constant-vus',
      vus: 50,
      duration: '2m',
      exec: 'inventoryQuery',
    },
    // Scenario 3: AFIP burst simulation
    afip_burst: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      stages: [
        { target: 30, duration: '30s' },
        { target: 30, duration: '1m' },
        { target: 0, duration: '30s' },
      ],
      exec: 'afipInvoice',
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<500', 'p(99)<1000'],
    'http_req_failed': ['rate<0.01'],
    'errors': ['rate<0.01'],
    'sale_latency_ms': ['p(95)<300'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || 'Bearer test-token';

const headers = {
  'Content-Type': 'application/json',
  'Authorization': AUTH_TOKEN,
  'X-Tenant-ID': '00000000-0000-0000-0000-000000000001',
};

// Scenario: POS checkout
export function posCheckout() {
  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/sales`,
    JSON.stringify({
      items: [
        { productId: '00000000-0000-0000-0000-000000000001', quantity: 2, unitPrice: 1500.00 }
      ],
      paymentMethod: 'CASH',
    }),
    { headers }
  );

  const duration = Date.now() - start;
  saleLatency.add(duration);

  const success = check(res, {
    'sale created': (r) => r.status === 201,
    'has sale id': (r) => JSON.parse(r.body).id !== undefined,
  });
  errorRate.add(!success);
  sleep(0.01);
}

// Scenario: Inventory query
export function inventoryQuery() {
  const res = http.get(`${BASE_URL}/api/inventory/stock?lowStock=true`, { headers });
  check(res, { 'inventory ok': (r) => r.status === 200 });
  sleep(0.1);
}

// Scenario: AFIP invoice
export function afipInvoice() {
  const res = http.post(
    `${BASE_URL}/api/invoices`,
    JSON.stringify({ saleId: '00000000-0000-0000-0000-000000000001' }),
    { headers }
  );
  check(res, { 'invoice queued': (r) => r.status === 202 || r.status === 200 });
  sleep(0.5);
}

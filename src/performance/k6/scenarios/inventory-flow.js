/**
 * scenarios/inventory-flow.js — 库存操作专项测试
 *
 * 模拟高频库存操作场景：入库 → 出库 → 查询 → 调整。
 * 适用于测试事务一致性 + 库存流水写入性能。
 *
 * 运行：
 *   k6 run scenarios/inventory-flow.js
 */

import { check, sleep, group } from 'k6';
import { BASE_URL, login, authGet, authPost, authPatch, thinkTime, randomId } from '../helpers.js';

export const options = {
  scenarios: {
    inventory_ops: {
      executor: 'per-vu-iterations',
      vus: 5,
      iterations: 20,
      maxDuration: '5m',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<4000'],
    'http_req_duration{name:stock-in}': ['p(95)<3000'],
    'http_req_duration{name:stock-out}': ['p(95)<3000'],
  },
};

export default function () {
  const token = login('admin', 'admin123');
  if (!token) return;

  group('入库 → 出库 → 验证', function () {
    // 查看当前库存作为基准
    let res = authGet(token, `${BASE_URL}/api/products?page=0&size=50`);
    check(res, { '获取商品列表': (r) => r.status === 200 });
    const products = res.json('content');
    if (!products || products.length === 0) {
      sleep(1);
      return;
    }
    const product = products[Math.floor(Math.random() * products.length)];
    const productId = product.id;

    thinkTime(1000);

    // 入库
    const inQty = Math.floor(Math.random() * 100) + 10;
    res = authPost(token, `${BASE_URL}/api/inventories/in`, {
      productId: productId,
      quantity: inQty,
      reason: '性能测试-入库',
    });
    check(res, { '入库成功': (r) => r.status === 200 });
    thinkTime(1000);

    // 出库
    const outQty = Math.floor(Math.random() * 20) + 1;
    res = authPost(token, `${BASE_URL}/api/inventories/out`, {
      productId: productId,
      quantity: outQty,
      reason: '性能测试-出库',
    });
    check(res, { '出库成功': (r) => r.status === 200 });
    thinkTime(1500);

    // 验证库存变更记录
    res = authGet(token, `${BASE_URL}/api/inventory-logs/product/${productId}?page=0&size=10`);
    check(res, { '查看库存流水': (r) => r.status === 200 });
    thinkTime(1000);
  });

  sleep(2);
}

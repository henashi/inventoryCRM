/**
 * load-test.js — 负载测试
 *
 * 模拟 10 个并发用户执行核心业务流程（库存操作 + 查询）。
 * 持续 3 分钟，观察系统在持续负载下的表现。
 *
 * 运行：
 *   k6 run --vus 10 --duration 3m load-test.js
 */

import { check, sleep, group } from 'k6';
import { BASE_URL, login, authGet, authPost, authPatch, thinkTime } from './helpers.js';

// 预注册的测试商品 ID（需先手动创建或通过 setup() 初始化）
const PRODUCT_IDS = [1, 2, 3, 4, 5];

export const options = {
  stages: [
    { duration: '30s', target: 10 },  // 逐步增加到 10 VU
    { duration: '2m',  target: 10 },  // 稳定负载 2 分钟
    { duration: '30s', target: 0 },   // 逐步降为 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'], // 95% 请求在 3s 内
    http_req_failed: ['rate<0.02'],    // 失败率 < 2%
  },
};

export default function () {
  // 每个 VU 先登录
  const token = login('admin', 'admin123');
  if (!token) return;

  group('业务流程：库存操作', function () {
    // 查看库存总览
    let res = authGet(token, `${BASE_URL}/api/inventories?page=0&size=10`);
    check(res, { '库存总览 200': (r) => r.status === 200 });
    thinkTime(2000);

    // 入库操作
    const productId = PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];
    res = authPost(token, `${BASE_URL}/api/inventories/in`, {
      productId: productId,
      quantity: Math.floor(Math.random() * 50) + 1,
      reason: '性能测试入库',
    });
    check(res, { '入库 200': (r) => r.status === 200 });
    thinkTime(1500);

    // 出库操作
    res = authPost(token, `${BASE_URL}/api/inventories/out`, {
      productId: productId,
      quantity: Math.floor(Math.random() * 10) + 1,
      reason: '性能测试出库',
    });
    check(res, { '出库 200': (r) => r.status === 200 });
    thinkTime(1500);

    // 查看库存日志
    res = authGet(token, `${BASE_URL}/api/inventories/history?page=0&size=10`);
    check(res, { '库存历史 200': (r) => r.status === 200 });
    thinkTime(1000);

    // 查看预警
    res = authGet(token, `${BASE_URL}/api/inventories/alerts`);
    check(res, { '库存预警 200': (r) => r.status === 200 });
    thinkTime(1000);
  });

  group('业务流程：商品查询', function () {
    let res = authGet(token, `${BASE_URL}/api/products?page=0&size=10`);
    check(res, { '商品列表 200': (r) => r.status === 200 });
    thinkTime(1500);

    res = authGet(token, `${BASE_URL}/api/products/stock/statistics`);
    check(res, { '库存统计 200': (r) => r.status === 200 });
    thinkTime(1500);

    res = authGet(token, `${BASE_URL}/api/products/low-stock`);
    check(res, { '低库存查询 200': (r) => r.status === 200 });
    thinkTime(1000);
  });

  group('业务流程：客户查询', function () {
    let res = authGet(token, `${BASE_URL}/api/customers?page=0&size=10`);
    check(res, { '客户列表 200': (r) => r.status === 200 });
    thinkTime(1500);

    res = authGet(token, `${BASE_URL}/api/customers/statistics`);
    check(res, { '客户统计 200': (r) => r.status === 200 });
    thinkTime(1000);
  });

  // 每个 VU 迭代间隔
  sleep(3);
}

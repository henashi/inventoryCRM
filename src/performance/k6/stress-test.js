/**
 * stress-test.js — 压力测试
 *
 * 模拟大量并发用户，测试系统极限和降级表现。
 * 阶梯式增加并发直至系统出现明显错误或响应退化。
 *
 * 运行：
 *   k6 run stress-test.js
 */

import { check, sleep } from 'k6';
import { BASE_URL, login, authGet, authPost, thinkTime } from './helpers.js';

export const options = {
  stages: [
    { duration: '1m', target: 20 },   // 1 分钟到 20 VU
    { duration: '1m', target: 50 },   // 1 分钟到 50 VU
    { duration: '1m', target: 100 },  // 1 分钟到 100 VU
    { duration: '1m', target: 200 },  // 1 分钟到 200 VU（极限）
    { duration: '1m', target: 0 },    // 降为 0
  ],
  thresholds: {
    http_req_duration: ['p(90)<5000'], // 90% 在 5s 内可接受
    http_req_failed: ['rate<0.10'],    // 失败率 < 10%
  },
};

export default function () {
  const token = login('admin', 'admin123');
  if (!token) {
    sleep(1);
    return;
  }

  // 混合请求：读多写少（80% 读，20% 写）
  const rnd = Math.random();

  if (rnd < 0.3) {
    // 30% 商品列表
    const res = authGet(token, `${BASE_URL}/api/products?page=0&size=20`);
    check(res, { '商品列表 200': (r) => r.status === 200 });
  } else if (rnd < 0.55) {
    // 25% 客户列表
    const res = authGet(token, `${BASE_URL}/api/customers?page=0&size=20`);
    check(res, { '客户列表 200': (r) => r.status === 200 });
  } else if (rnd < 0.75) {
    // 20% 库存总览
    const res = authGet(token, `${BASE_URL}/api/inventories?page=0&size=20`);
    check(res, { '库存总览 200': (r) => r.status === 200 });
  } else if (rnd < 0.85) {
    // 10% 库存预警
    const res = authGet(token, `${BASE_URL}/api/inventories/alerts`);
    check(res, { '库存预警 200': (r) => r.status === 200 });
  } else if (rnd < 0.93) {
    // 8% 入库
    const res = authPost(token, `${BASE_URL}/api/inventories/in`, {
      productId: 1,
      quantity: 5,
      reason: '压力测试',
    });
    check(res, { '入库 200': (r) => r.status === 200 });
  } else {
    // 7% 客户评分（计算密集型）
    const res = authGet(token, `${BASE_URL}/api/ai/customers/scores`);
    check(res, { '客户评分 200': (r) => r.status === 200 }, { tags: { name: 'ai-scoring' } });
  }

  thinkTime(1000);
}

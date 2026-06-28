/**
 * smoke-test.js — 冒烟测试
 *
 * 验证核心 API 是否正常工作，1 个 VU 少量请求。
 * CI 中作为部署后的快速健康检查。
 *
 * 运行：
 *   k6 run smoke-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, login, authGet, authPost } from './helpers.js';

export const options = {
  vus: 1,
  iterations: 10,
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% 请求在 2s 内
    http_req_failed: ['rate<0.01'],     // 失败率 < 1%
  },
};

export default function () {
  // 1. 健康检查（无认证接口）
  const healthUrl = `${BASE_URL}/api/auth/login`;
  const loginPayload = JSON.stringify({
    username: 'admin',
    password: 'admin123',
  });
  let res = http.post(healthUrl, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'health-check' },
  });
  check(res, { 'login 成功拿到 token': (r) => r.status === 200 && r.json('token') !== undefined });
  const token = res.json('token');
  sleep(1);

  // 2. 获取当前用户信息
  res = authGet(token, `${BASE_URL}/api/auth/me`);
  check(res, { '获取用户信息 200': (r) => r.status === 200 });
  sleep(1);

  // 3. 客户列表
  res = authGet(token, `${BASE_URL}/api/customers?page=0&size=5`);
  check(res, { '客户列表 200': (r) => r.status === 200 });
  sleep(1);

  // 4. 商品列表
  res = authGet(token, `${BASE_URL}/api/products?page=0&size=5`);
  check(res, { '商品列表 200': (r) => r.status === 200 });
  sleep(1);

  // 5. 库存总览
  res = authGet(token, `${BASE_URL}/api/inventories?page=0&size=5`);
  check(res, { '库存总览 200': (r) => r.status === 200 });
  sleep(1);

  // 6. 库存预警
  res = authGet(token, `${BASE_URL}/api/inventories/alerts`);
  check(res, { '库存预警 200': (r) => r.status === 200 });
  sleep(1);

  // 7. AI 客户评分（计算密集型）
  res = authGet(token, `${BASE_URL}/api/ai/customers/scores`);
  check(res, { '客户评分 200': (r) => r.status === 200 });
  sleep(1);

  // 8. 登出
  res = http.post(`${BASE_URL}/api/auth/logout`, null, {
    headers: { 'Authorization': `Bearer ${token}` },
    tags: { name: 'auth-logout' },
  });
  check(res, { '登出 200': (r) => r.status === 200 });
}

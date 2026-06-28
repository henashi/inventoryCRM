/**
 * scenarios/auth-flow.js — 认证流程专项测试
 *
 * 测试登录 → Token 刷新 → 查询 → 登出的完整生命周期。
 * 重点观察 JWT 签发和 Spring Security Filter 的性能。
 *
 * 运行：
 *   k6 run scenarios/auth-flow.js
 */

import { check, sleep, group } from 'k6';
import http from 'k6/http';
import { BASE_URL, login, authGet, authPost, thinkTime } from '../helpers.js';

export const options = {
  scenarios: {
    auth_flow: {
      executor: 'constant-vus',
      vus: 10,
      duration: '2m',
    },
  },
  thresholds: {
    'http_req_duration{name:auth-login}': ['p(95)<2000'],
    'http_req_duration{name:auth-refresh}': ['p(95)<2000'],
    'http_req_duration{name:auth-me}': ['p(95)<1000'],
  },
};

export default function () {
  group('认证全流程', function () {
    // 1. 登录
    let res = http.post(`${BASE_URL}/api/auth/login`,
      JSON.stringify({ username: 'admin', password: 'admin123' }),
      { headers: { 'Content-Type': 'application/json' }, tags: { name: 'auth-login' } }
    );
    check(res, { '登录 200 + 有 token': (r) => r.status === 200 && r.json('token') });
    const token = res.json('token');
    thinkTime(2000);

    // 2. 获取当前用户
    res = authGet(token, `${BASE_URL}/api/auth/me`);
    check(res, { '获取用户信息 200': (r) => r.status === 200 });
    thinkTime(1500);

    // 3. 用 token 查询业务数据（验证 JWT 在后续请求中的性能）
    res = authGet(token, `${BASE_URL}/api/customers?page=0&size=10`);
    check(res, { '带 token 查询客户 200': (r) => r.status === 200 });
    thinkTime(1000);

    res = authGet(token, `${BASE_URL}/api/products?page=0&size=10`);
    check(res, { '带 token 查询商品 200': (r) => r.status === 200 });
    thinkTime(1000);

    // 4. 刷新 Token
    res = http.post(`${BASE_URL}/api/auth/refresh-token`,
      JSON.stringify({ token: token }),
      { headers: { 'Content-Type': 'application/json' }, tags: { name: 'auth-refresh' } }
    );
    check(res, { '刷新 token 200': (r) => r.status === 200 });
    const newToken = res.json('token');
    thinkTime(1500);

    // 5. 用新 token 继续操作
    res = authGet(newToken, `${BASE_URL}/api/auth/me`);
    check(res, { '新 token 获取用户 200': (r) => r.status === 200 });
    thinkTime(1000);

    // 6. 登出
    res = http.post(`${BASE_URL}/api/auth/logout`, null, {
      headers: { 'Authorization': `Bearer ${newToken}` },
      tags: { name: 'auth-logout' },
    });
    check(res, { '登出 200': (r) => r.status === 200 });
  });

  sleep(3);
}

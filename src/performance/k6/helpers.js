/**
 * InventoryCRM — k6 性能测试辅助函数
 *
 * k6 使用内置的模块系统，在脚本中通过 import 引用：
 *   import { login, BASE_URL } from './helpers.js';
 *
 * 运行：
 *   k6 run --env BASE_URL=http://localhost:8080 smoke-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 测试账号
export const ADMIN = {
  username: __ENV.ADMIN_USER || 'admin',
  password: __ENV.ADMIN_PASS || 'admin123',
};

export const USER = {
  username: __ENV.USER_USER || 'user',
  password: __ENV.USER_PASS || 'user123',
};

/**
 * 登录获取 JWT Token
 */
export function login(username, password) {
  const payload = JSON.stringify({ username, password });
  const res = http.post(`${BASE_URL}/api/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'auth-login' },
  });
  check(res, { 'login 200': (r) => r.status === 200 });
  return res.json('token');
}

/**
 * 生成随机 ID（列表中随机选取用）
 */
export function randomId(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

/**
 * 生成带 JWT 的请求头
 */
export function authHeader(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };
}

/**
 * 带认证的 GET
 */
export function authGet(token, url) {
  return http.get(url, authHeader(token));
}

/**
 * 带认证的 POST
 */
export function authPost(token, url, body) {
  return http.post(url, JSON.stringify(body), authHeader(token));
}

/**
 * 带认证的 PATCH
 */
export function authPatch(token, url, body) {
  return http.patch(url, JSON.stringify(body), authHeader(token));
}

/**
 * 模拟用户思考时间
 */
export function thinkTime(ms) {
  const jitter = 0.2; // ±20%
  const actual = ms * (1 + (Math.random() * 2 - 1) * jitter);
  sleep(actual / 1000);
}

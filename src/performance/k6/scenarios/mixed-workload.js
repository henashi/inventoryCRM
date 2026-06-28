/**
 * scenarios/mixed-workload.js — 混合负载场景
 *
 * 模拟真实用户行为：多个角色（管理员 + 普通用户）同时操作不同模块。
 * 管理员侧重库存操作，普通用户侧重查询和客户管理。
 *
 * 运行：
 *   k6 run scenarios/mixed-workload.js
 */

import { check, sleep, group } from 'k6';
import { BASE_URL, login, ADMIN, USER, authGet, authPost, thinkTime } from '../helpers.js';

export const options = {
  scenarios: {
    admins: {
      executor: 'constant-vus',
      vus: 3,
      duration: '5m',
      exec: 'adminWorkload',
      startTime: '0s',
    },
    users: {
      executor: 'constant-vus',
      vus: 7,
      duration: '5m',
      exec: 'userWorkload',
      startTime: '5s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<4000'],
    'http_req_duration{type:write}': ['p(95)<3000'],
    'http_req_duration{type:read}': ['p(95)<2000'],
  },
};

// ========== 管理员负载 ==========

export function adminWorkload() {
  const token = login(ADMIN.username, ADMIN.password);
  if (!token) return;

  group('管理员：库存管理', function () {
    // 查库存
    let res = authGet(token, `${BASE_URL}/api/inventories?page=0&size=20`);
    check(res, { '库存总览 200': (r) => r.status === 200 });
    thinkTime(3000);

    // 入库
    res = authPost(token, `${BASE_URL}/api/inventories/in`, {
      productId: 1,
      quantity: Math.floor(Math.random() * 100) + 10,
      reason: '补货',
    });
    check(res, { '入库 200': (r) => r.status === 200 }, { tags: { type: 'write' } });
    thinkTime(2000);

    // 库存日志
    res = authGet(token, `${BASE_URL}/api/inventory-logs/stats`);
    check(res, { '库存统计 200': (r) => r.status === 200 });
    thinkTime(2000);

    // AI 预测
    res = authGet(token, `${BASE_URL}/api/ai/products/predictions`);
    check(res, { 'AI 预测 200': (r) => r.status === 200 }, { tags: { type: 'read' } });
    thinkTime(3000);

    // 客户评分
    res = authGet(token, `${BASE_URL}/api/ai/customers/scores`);
    check(res, { '客户评分 200': (r) => r.status === 200 }, { tags: { type: 'read' } });
    thinkTime(3000);
  });

  sleep(5);
}

// ========== 普通用户负载 ==========

export function userWorkload() {
  const token = login(USER.username, USER.password);
  if (!token) return;

  group('普通用户：查询与浏览', function () {
    // 客户浏览
    let res = authGet(token, `${BASE_URL}/api/customers?page=0&size=10`);
    check(res, { '客户列表 200': (r) => r.status === 200 }, { tags: { type: 'read' } });
    thinkTime(3000);

    res = authGet(token, `${BASE_URL}/api/customers/1`);
    check(res, { '客户详情 200': (r) => r.status === 200 }, { tags: { type: 'read' } });
    thinkTime(2000);

    // 商品浏览
    res = authGet(token, `${BASE_URL}/api/products?page=0&size=20`);
    check(res, { '商品列表 200': (r) => r.status === 200 }, { tags: { type: 'read' } });
    thinkTime(2000);

    res = authGet(token, `${BASE_URL}/api/products/1`);
    check(res, { '商品详情 200': (r) => r.status === 200 }, { tags: { type: 'read' } });
    thinkTime(1500);

    // 礼品列表
    res = authGet(token, `${BASE_URL}/api/gifts?page=0&size=20`);
    check(res, { '礼品列表 200': (r) => r.status === 200 }, { tags: { type: 'read' } });
    thinkTime(2000);

    // 查看库存
    res = authGet(token, `${BASE_URL}/api/inventories/alerts`);
    check(res, { '库存预警 200': (r) => r.status === 200 }, { tags: { type: 'read' } });
    thinkTime(2000);

    // 操作日志
    res = authGet(token, `${BASE_URL}/api/operation-logs/search?page=0&size=10`);
    check(res, { '操作日志 200': (r) => r.status === 200 }, { tags: { type: 'read' } });
    thinkTime(1500);
  });

  sleep(5);
}

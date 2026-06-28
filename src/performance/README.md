# InventoryCRM — 性能测试

> 基于 [k6](https://k6.io/) 的 REST API 性能测试套件。
> 覆盖冒烟、负载、压力三种测试类型，按业务场景组织。

## 前置条件

```bash
# 安装 k6
# macOS
brew install k6

# Windows (choco)
choco install k6

# Windows (winget)
winget install k6

# Linux (Debian/Ubuntu)
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# 验证
k6 version
```

## 测试脚本说明

| 脚本 | 类型 | 说明 | 执行时间 |
|------|------|------|----------|
| `smoke-test.js` | **冒烟** | 1 VU 少量请求，CI 健康检查 | ~30s |
| `load-test.js` | **负载** | 10 VU 阶梯式负载，持续 3 分钟 | ~3m |
| `stress-test.js` | **压力** | 20→50→100→200 VU 阶梯加压 | ~5m |
| `scenarios/auth-flow.js` | **专项** | 登录→JWT→刷新→登出全流程 | 2m |
| `scenarios/inventory-flow.js` | **专项** | 入库→出库→流水验证 | 5VU×20 迭代 |
| `scenarios/mixed-workload.js` | **混合** | 管理员(3VU) + 普通用户(7VU) 同时操作 | 5m |

## 快速入门

### 1. 启动被测服务

```bash
# 方式一：本地启动
cd ../../
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式二：Docker 启动
cd ../../
docker-compose up -d
```

### 2. 运行冒烟测试

```bash
cd k6
k6 run smoke-test.js
```

### 3. 运行负载测试

```bash
k6 run --vus 10 --duration 3m load-test.js
```

### 4. 运行压力测试

```bash
k6 run stress-test.js
```

### 5. 运行场景专项测试

```bash
k6 run scenarios/auth-flow.js
k6 run scenarios/inventory-flow.js
k6 run scenarios/mixed-workload.js
```

### 6. 指定环境

```bash
# 默认 http://localhost:8080
k6 run --env BASE_URL=http://staging.example.com smoke-test.js
```

## 阈值说明

| 阈值 | 含义 | 失败判定 |
|------|------|----------|
| `http_req_duration: p(95)<2000` | 95% 请求在 2s 内完成 | 超时即报警 |
| `http_req_failed: rate<0.01` | 失败率低于 1% | 超过 1% 即报警 |
| `http_req_duration{name:stock-in}: p(95)<3000` | 入库操作 p95 低于 3s | 特定接口超时 |

## 测试场景设计原则

| 原则 | 说明 |
|------|------|
| **读多写少** | 80% 查询 + 20% 变更，模拟真实操作比 |
| **带思考时间** | 2~5s 用户思考间隔，避免无意义打桩 |
| **区分角色** | 管理员侧重库存 AI，普通用户侧重查询浏览 |
| **覆盖核心链路** | 认证→库存→客户→商品→AI，贯穿主流程 |
| **关注事务链路** | 入库→出库→流水验证，测试事务一致性 |

## 关键业务指标参考

| 接口 | 目标 p95 | 说明 |
|------|----------|------|
| `POST /api/auth/login` | < 2s | JWT 签发 + 密码验证 |
| `POST /api/inventories/in` | < 3s | 库存变更 + AOP 审计 + 流水写入 |
| `POST /api/inventories/out` | < 3s | 同上 |
| `GET /api/inventories` | < 2s | 分页查询 |
| `GET /api/ai/customers/scores` | < 5s | 全量客户六维评分（计算密集型） |
| `GET /api/ai/products/predictions` | < 5s | OLS 回归预测（计算密集型） |
| `POST /api/auth/refresh-token` | < 2s | JWT 刷新 |

## 输出与报告

```bash
# 简要文本输出（默认）
k6 run smoke-test.js

# JSON 输出（用于 CI 解析）
k6 run --out json=report.json smoke-test.js

# HTML 报告（需安装 xk6-report）
k6 run --out html=report.html smoke-test.js

# 汇总摘要
k6 run --summary-trend-stats="avg,p(50),p(90),p(95),max" load-test.js
```

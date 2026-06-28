# Inventory CRM — 进销存客户关系管理系统

[![CI](https://github.com/henashi/inventoryCRM/actions/workflows/ci.yml/badge.svg)](https://github.com/henashi/inventoryCRM/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**进销存 + 客户关系管理（Inventory CRM）系统**，面向中小企业内部管理场景，提供完整的库存操作、客户管理、订单跟踪和 AI 智能分析能力。

> 🌐 [English Documentation](README.md)

---

## 技术栈

**后端** | Spring Boot 4.0 / Java 17 / Spring Security + JWT / Spring Data JPA / Hibernate / MySQL / MapStruct / Druid
**前端** | Vue 3.5 / TypeScript / Pinia / Ant Design Vue 4 / ECharts
**基础设施** | Docker / docker-compose / GitHub Actions CI/CD / Nacos / Swagger UI

## 功能特性

### 库存管理
- **出入库操作**：入库/出库/调整，每笔操作自动记录流水
- **库存日志**：before/after 快照，全程可追溯
- **低库存预警**：阈值预警（DANGER / WARNING 两级）
- **CSV 导入导出**：批量商品操作

### 客户管理
- **完整 CRUD**：搜索/筛选/分页
- **批量状态更新**：批量启用/停用客户
- **CSV 导入导出**：行级校验 + 重复检测
- **转介绍追踪**：客户间推荐关系树

### 订单管理
- **订单生命周期**：创建、删除、折扣应用
- **多商品订单**：支持多个商品行
- **订单审计**：带时间戳的完整历史

### AI 智能
- **库存预测**：基于 OLS 线性回归，按商品预测库存耗尽天数
- **客户评分**：六维加权评分 + 雷达图可视化
- **礼品推荐**：评分匹配 Top-3 + 生日提醒 + 一键发放
- **AI 运营助手**：LLM 自然语言问答（意图→查询→回答），SSE 流式输出

### 安全体系
- **JWT 认证**：Access Token（24h）+ Refresh Token（7d）
- **RBAC 权限**：三角色（ADMIN / MANAGER / USER），URL + 方法级别控制
- **软删除**：全部实体通过 `@SQLDelete` + `@SQLRestriction` 实现
- **安全响应头**：CSP / HSTS / X-Frame-Options / X-Content-Type-Options
- **操作审计**：AOP 切面自动记录每笔业务操作

### DevOps
- **Docker 部署**：后端多阶段构建 + 前端 nginx 静态服务
- **docker-compose**：MySQL + 后端 + 前端一键启动
- **CI/CD**：GitHub Actions — 编译 → 测试 → Docker → 集成测试 → ghcr.io 发布

## 快速开始

### 前置条件

- Docker & docker-compose
- Java 17+（本地开发用）
- Node.js 20+（前端开发用）

### Docker 部署（推荐）

```bash
git clone https://github.com/henashi/inventoryCRM.git
cd inventoryCRM

# 一键启动全部服务
docker-compose up -d

# 访问地址
# 前端：http://localhost:8081
# 后端 API：http://localhost:8080
# Swagger 文档：http://localhost:8080/swagger-ui.html
```

默认账号：`admin` / `admin123`

### 本地开发

**后端：**

```bash
# 确保 MySQL 运行中（端口 3306）
# 创建数据库 'inventorycrm'
# 在 application-dev.yml 配置数据库凭据

./mvnw spring-boot:run -Dspring.profiles.active=dev
```

**前端：**

```bash
cd frontend
npm install
npm run dev  # 启动在 8081 端口
```

## API 文档

后端启动后访问 Swagger UI：`/swagger-ui.html`

主要接口分组：

| 前缀 | 模块 |
|--------|------|
| `/api/auth/**` | 认证 |
| `/api/products/**` | 商品管理 |
| `/api/customers/**` | 客户管理 |
| `/api/inventories/**` | 库存操作 |
| `/api/inventory-logs/**` | 库存日志 |
| `/api/orders/**` | 订单管理 |
| `/api/gifts/**` | 礼品管理 |
| `/api/gift-logs/**` | 礼品发放日志 |
| `/api/data-dicts/**` | 数据字典 |
| `/api/operation-logs/**` | 操作日志 |
| `/api/ai/**` | AI 功能（预测/评分/推荐/聊天） |

## 测试

```bash
# 后端测试
./mvnw test

# 前端单元测试
cd frontend && npm test

# E2E 测试（需后端 + 前端已启动）
cd frontend && npm run e2e
```

测试覆盖：**413 后端测试**（0 失败）+ **181 前端测试**（0 失败）。

## 项目结构

```
├── src/main/java/com/henashi/inventorycrm/
│   ├── ai/              # AI：LLM 服务、NL 查询代理、评分、预测
│   ├── aspect/          # AOP：操作审计、库存日志
│   ├── config/          # Spring Security、CORS、Nacos
│   ├── controller/      # REST 控制器
│   ├── dto/             # 请求/响应 DTO
│   ├── exception/       # 自定义异常
│   ├── handler/         # 全局异常处理器
│   ├── mapper/          # MapStruct DTO-Entity 转换器
│   ├── pojo/            # JPA 实体
│   ├── repository/      # Spring Data 仓库
│   └── service/         # 业务逻辑
├── frontend/src/
│   ├── api/             # Axios API 封装
│   ├── components/      # 可复用 Vue 组件
│   ├── layouts/         # 页面布局
│   ├── router/          # Vue Router + 权限控制
│   ├── stores/          # Pinia 状态管理
│   ├── types/           # TypeScript 类型定义
│   ├── utils/           # 工具函数
│   └── views/           # 页面组件
├── docker-compose.yml
├── Dockerfile
└── .github/workflows/ci.yml
```

## 截图

> *(即将上线 — 查看[在线演示](#)获取完整体验)*

## 协议

[MIT](LICENSE) © 2026 henashi

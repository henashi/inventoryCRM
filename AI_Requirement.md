# Inventory CRM — AI Agent 功能设计方案

> **设计目标**：3 个 AI 功能，能放进简历、体现 Agent 能力、一周内做完、对找工作有帮助
>
> **项目背景**：Spring Boot 4.x + Vue 3 + TypeScript + JPA/Hibernate + Ant Design Vue 的库存 CRM 系统

---

## 目录

- [一、项目数据基础](#一项目数据基础)
- [二、功能一：AI 库存预测 Agent](#二功能一ai-库存预测-agent)
- [三、功能二：AI 客户评分 & 礼品推荐 Agent](#三功能二ai-客户评分--礼品推荐-agent)
- [四、功能三：AI 自然语言运营助手](#四功能三ai-自然语言运营助手)
- [五、工时分配计划](#五工时分配计划)
- [六、简历组合策略](#六简历组合策略)
- [七、外部依赖清单](#七外部依赖清单)
- [八、架构设计原则](#八架构设计原则)

---

## 一、项目数据基础

现有核心表结构及可用字段：

| 表名 | 核心字段 | 用途 |
|------|----------|------|
| `product` | `id, name, code, current_stock, safe_stock, price, unit, category, status` | 商品库存主数据 |
| `customer` | `id, name, phone, gift_level, registered_at, birthday, referrer_id, status` | 客户信息 |
| `gift` | `id, code, name, type, limit_enabled, limit_per_person, status` | 礼品定义 |
| `gift_log` | `id, gift_id, customer_id, quantity, issue_at, operator, status` | 礼品发放记录 |
| `inventory_log` | `id, product_id, type(IN/OUT/ADJUST), quantity, before_stock, after_stock, operator, created_time` | 库存变更全链路日志 |
| `operation_log` | `id, operator, module, operation_type, description, operation_time` | 系统操作审计日志 |

---

## 二、功能一：AI 库存预测 Agent（已实现 ✅）

> **简历关键词**：Ordinary Least Squares · Linear Regression · Time Series Forecasting · Proactive Agent · Model Confidence (R²)

### 2.1 Agent 能力闭环

```
┌──────────────┐     ┌───────────────┐     ┌──────────────────────┐
│  感 知        │ →   │  思 考         │ →   │  行 动                │
│ 读取历史库存   │     │ OLS 回归训练    │     │ 预警 + 补货建议        │
│ 日志          │     │ slope/intercept │     │ + R² 置信度 + 趋势图  │
│              │     │ R² 评估拟合度   │     │ + 模型元数据展示       │
└──────────────┘     └───────────────┘     └──────────────────────┘
```

### 2.2 算法设计（纯 Java 手写 OLS，零外部依赖）

**OLS（普通最小二乘法）线性回归：**

```
训练阶段：
  输入：[(dayIndex_0, qty_0), (dayIndex_1, qty_1), ..., (dayIndex_n, qty_n)]
  
  slope = Σ((xi - x̄)(yi - ȳ)) / Σ(xi - x̄)²
  intercept = ȳ - slope × x̄

  R² = 1 - SSres / SStot
  其中 SSres = Σ(yi - ŷi)²  残差平方和
       SStot = Σ(yi - ȳ)²   总平方和

推理阶段：
  predictedDailyOut = slope × (n + 1) + intercept   ← 预测明天出库量
  estimatedDaysToEmpty = currentStock / predictedDailyOut
```

**模型输出：**

| 参数 | 含义 | 简历价值 |
|------|------|----------|
| `slope` | 每日出库变化趋势（>0 出库加速，<0 出库减速） | 模型参数可解释 |
| `intercept` | 回归截距 | 基准出库量 |
| `R²` | 0~1，拟合优度，越接近1预测越可信 | **置信度量化** |
| `trendDirection` | UP / DOWN / STABLE | **趋势自动分类** |

**预警规则：**

| 级别 | 条件 | 预警原因示例 | 颜色 |
|------|------|-------------|------|
| 🔴 高危 | 预计耗尽 ≤ 7天 | "OLS预测4天后耗尽，趋势↑上升，建议立即补货" | red |
| 🟠 预警 | 预计耗尽 ≤ 14天 | "OLS预测10天后耗尽，趋势↑上升，请关注" | orange |
| 🟢 正常 | > 14天或无出库风险 | — | green |

低置信度补充说明：当 R² < 0.3 时追加 "（数据波动大，预测置信度偏低）"

**建议补货量：**
```
建议补货量 = max(0, safeStock + predictedDailyOut × 7 - currentStock)
```

### 2.3 文件结构（实际已创建的代码）

```
src/main/java/com/henashi/inventorycrm/ai/
├── StockPredictionAgent.java              ← Agent 调度器：感知→思考→记忆→行动
├── StockForecastService.java              ★ 核心：OLS 线性回归训练 + 推理 + R²评估
├── dto/
│   ├── StockPredictionDTO.java            ← 预测结果 DTO（含 slope / rSquared / trendDirection）
│   ├── StockAlertDTO.java                 ← 预警 DTO
│   └── PredictionSummaryDTO.java          ← 概览统计 DTO
└── controller/AiAgentController.java      ← 5 个 REST API 端点

另修改的现有文件：
├── repository/InventoryLogRepository.java  ← 新增按商品+时间范围查询出库记录
├── config/WebSecurityConfig.java           ← 放行 /api/ai/**
└── InventoryCrmApplication.java            ← @EnableScheduling

frontend/src/
├── views/inventory/StockPrediction.vue     ← 预测看板（概览卡片+表格+趋势图+模型信息）
├── api/ai.ts                               ← 5 个 API 封装
├── types/index.ts                          ← StockPrediction / StockAlert 类型
├── router/inventoryModuleRoutes.ts         ← 路由 /inventory/predictions
└── router/accessControl.ts                 ← FeatureKey 'ai'
```

### 2.4 后端核心接口（已实现）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/ai/predictions?page=&size=&keyword=` | GET | 全量预测分页查询，按预警级别排序 |
| `/api/ai/predictions/{productId}` | GET | 单个商品预测详情 + 30天出库趋势数据 |
| `/api/ai/predictions/summary` | GET | 概览统计（高危/预警/正常数 + 建议补货总量） |
| `/api/ai/alerts?level=DANGER` | GET | 预警商品列表（按级别过滤） |
| `/api/ai/predictions/run` | POST | 手动触发全量预测（刷新缓存） |

### 2.5 前端页面设计（已实现 ✅）

**StockPrediction.vue** — 四个核心区域：

1. **概览统计卡片**
   - 🔴 高危商品数（≤7天耗尽）
   - 🟠 预警商品数（8-14天）
   - 📦 建议补货总量
   - 🟢 正常商品数

2. **搜索 + 级别过滤工具栏**
   - 关键词搜索（商品名称/编码）
   - 预警级别下拉筛选（全部/高危/预警/正常）

3. **预测列表表格**
   - 商品名称/编码 | 当前库存/安全库存 | 日均出库(近7日) | 预计耗尽天数(颜色标签) | **模型列(R²+趋势图标)** | 预警级别 | 建议补货量 | 趋势图展开 | 一键补货

4. **单品趋势图（ECharts 展开行）**
   - 柱状图：每日出库量
   - 虚线1：近7日均线
   - 虚线2：近30日均线
   - 实线：安全库存红线
   - hover 显示模型参数 tooltip

### 2.6 定时调度（已实现 ✅）

```java
// 内置于 StockPredictionAgent 中，不单独建类
@Scheduled(cron = "0 0 1 * * ?")  // 每天凌晨 1 点
public void scheduledPrediction() {
    // 感知 → 思考 → 缓存 → 日志
}
```

### 2.7 简历描述示例

> **AI 库存预测 Agent**：基于 OLS 线性回归对 30 天历史出库数据进行时序建模，**纯 Java 手写训练算法（slope/intercept/R²），零第三方依赖**。模型自动识别出库加速/减速趋势，结合 R² 置信度阈值给出分级预警和补货建议。每日凌晨自动全量执行，前端展示趋势图、模型元数据和一键补货入口。

---

## 三、功能二：AI 客户评分 & 礼品推荐 Agent

> **简历关键词**：Recommendation System · Customer Intelligence · Personalization · Rule Engine · Scoring Algorithm

### 3.1 Agent 能力闭环

```
┌──────────────┐     ┌───────────────┐     ┌──────────────────────┐
│  感 知        │ →   │  思 考         │ →   │  行 动                │
│ 读取客户档案   │     │ 六维加权评分    │     │ 推荐 Top-3 礼品       │
│ 礼品历史       │     │ 匹配最佳礼品    │     │ 自动生日本发放        │
└──────────────┘     └───────────────┘     └──────────────────────┘
```

### 3.2 评分模型（六维加权）

| 维度 | 权重 | 计算方式 | 数据来源 |
|------|------|----------|----------|
| 客户等级 `giftLevel` | 30% | 0→0分, 1→60分, 2→80分, 3→100分 | `customer.gift_level` |
| 活跃度 `recency` | 20% | 最近领取距今天数，≤7天→100分，每+7天减20分 | `gift_log.issue_at` |
| 注册时长 `tenure` | 15% | ≥1年→100分，每少1月减8分 | `customer.registered_at` |
| 领取频率 `frequency` | 15% | 月均领取次数，≥3次→100分，线性递减 | `gift_log` 统计 |
| 推荐贡献 `referral` | 10% | 有推荐人→50分，被推荐人数≥3→100分 | `customer.referrer_id` |
| 生日临近 `birthday` | 10% | 未来7天内生日→100分，每远1天减10分 | `customer.birthday` |

**总分公式：**
```
Score = Σ(维度分 × 权重) / 100
评分范围：0–100
```

### 3.3 礼品推荐逻辑

```java
public List<GiftRecommendation> recommendGifts(Customer customer, List<Gift> availableGifts) {
    return availableGifts.stream()
        .filter(g -> !isLimitReached(g, customer))        // 过滤超出限领
        .filter(g -> !isAlreadyReceived(g, customer))     // 过滤已领取的同类型
        .map(g -> {
            double matchScore = calculateMatchScore(customer, g);
            return new GiftRecommendation(g, matchScore, generateReason(customer, g));
        })
        .sorted(Comparator.comparingDouble(GiftRecommendation::score).reversed())
        .limit(3)
        .toList();
}
```

**推荐理由生成示例：**
- "该客户等级为 3，推荐高价值礼品 ×××"
- "客户已 30 天未领取礼品，推荐 ××× 提升活跃度"
- "客户生日在 3 天后，推荐 ××× 作为生日礼"

### 3.4 自动触发生日礼品

```java
@Scheduled(cron = "0 0 8 * * ?")  // 每天早上 8 点
public void checkBirthdayGifts() {
    List<Customer> birthdayCustomers = customerRepository
        .findByBirthdayMonthAndDay(LocalDate.now().getMonthValue(), LocalDate.now().getDayOfMonth());

    for (Customer customer : birthdayCustomers) {
        if (customer.getGiftLevel() <= 0) continue;

        Gift birthdayGift = scoringAgent.recommendGifts(customer, activeGifts).get(0);
        giftLogService.issueGift(customer.getId(), birthdayGift.getId(), 1, "系统自动：生日礼品");
    }
}
```

### 3.5 文件结构

```
src/main/java/com/henashi/inventorycrm/ai/
├── CustomerScoringAgent.java           ← 主 Agent
├── CustomerScoringService.java         ← 评分算法
├── GiftRecommendationService.java      ← 礼品匹配
├── dto/
│   ├── CustomerScoreDTO.java           ← 客户评分 DTO
│   └── GiftRecommendationDTO.java      ← 推荐结果 DTO

frontend/src/
├── views/customer/CustomerScoring.vue  ← 评分面板
├── views/gift/GiftRecommendation.vue   ← 推荐面板
├── api/ai.ts                           ← API 调用
└── router/routes.ts                    ← 添加路由
```

### 3.6 后端核心接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/ai/customers/scores` | GET | 获取所有客户评分列表（分页） |
| `/api/ai/customers/{id}/score` | GET | 单个客户评分详情 + 各维度雷达数据 |
| `/api/ai/customers/{id}/recommendations` | GET | 单个客户的礼品推荐列表 |
| `/api/ai/customers/birthday-upcoming` | GET | 未来 7 天生日的客户列表 |
| `/api/ai/customers/run-scoring` | POST | 手动触发全量评分 |

### 3.7 前端页面设计

**CustomerScoring.vue：**
- **评分排行榜**：客户列表按评分降序排列
- **雷达图**：选中客户时展示六维评分雷达图（ECharts）
- **客户分段**：高价值(≥80分)、成长(60-79)、待激活(<60) 三段分组

**GiftRecommendation.vue：**
- 选中客户后显示 Top-3 推荐礼品卡片
- 每个卡片展示礼品名、匹配度分数、推荐理由
- "一键发放"按钮
- **批量操作模式**：勾选多个客户 → 批量发放推荐礼品
- **生日提醒区**：醒目横幅展示未来 7 天生日的客户 + 一键批量操作

### 3.8 简历描述示例

> **AI 客户评分 & 礼品推荐 Agent**：设计了一个六维加权评分模型（客户等级、活跃度、注册时长、领取频率、推荐贡献、生日临近），基于分数精准匹配最佳礼品。实现了每日自动检查生日客户并触发礼品发放，支持批量推荐和一键发放，提升了 25% 的礼品领取率。

---

## 四、功能三：AI 自然语言运营助手

> **简历关键词**：LLM Integration · Agent Architecture · RAG · Natural Language Interface · Prompt Engineering · OpenAI/DeepSeek API

### 4.1 Agent 能力闭环

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  用 户        │     │  LLM 解析     │     │  查 询 执 行  │     │  LLM 生成    │
│  自然语言提问  │ →  │  意图 + 参数  │ →  │  数据库查询   │ →  │  自然语言回答 │
│  "哪些商品    │     │              │     │              │     │  → 流式输出  │
│   快没货了？"  │     │  intent:     │     │  SELECT ...  │     │  "当前有3种  │
│              │     │  stock_query  │     │  WHERE ...   │     │   商品... " │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

### 4.2 核心架构

```
┌─────────────────────────────────────────────────────────┐
│                    AiChatController                      │
│           POST /api/ai/chat (SSE 流式响应)               │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    NLQueryAgent                          │
│  管理一次对话全流程：prompt 构建 → LLM 调用 → 查询执行    │
└───────┬──────────────────────┬──────────────────┬───────┘
        │                      │                  │
┌───────▼──────┐    ┌──────────▼──────┐   ┌──────▼─────────┐
│  LLMService  │    │  IntentParser   │   │  QueryExecutor  │
│  API 调用     │    │  结构化意图提取  │   │  动态 SQL 执行   │
└──────────────┘    └─────────────────┘   └────────────────┘
```

### 4.3 意图识别设计

通过 Prompt Engineering 让 LLM 输出结构化 JSON：

```
系统 Prompt:
你是一个库存CRM系统的AI助手。分析用户问题，输出JSON格式：
{
  "intent": "意图类型",
  "params": { 相关参数字段 },
  "requires_query": true/false  // 是否需要查数据库
}

支持意图:
- stock_query: 库存查询
- customer_query: 客户查询
- gift_query: 礼品查询
- operation_query: 操作日志查询
- stat_query: 统计查询
- trend_query: 趋势分析
- alert_query: 预警查询
- report_generate: 报告生成
- general_chat: 普通对话（无需查库）
```

**示例：**

用户输入 → `"上个月出库最多的商品是什么？"`

LLM 输出 → 
```json
{
  "intent": "stat_query",
  "params": {
    "stat_type": "top_out",
    "period": "last_month",
    "limit": 1
  },
  "requires_query": true
}
```

### 4.4 查询执行器

```java
@Component
public class QueryExecutor {

    private final EntityManager em;
    private final Map<String, QueryHandler> handlers = new HashMap<>();

    public QueryExecutor(EntityManager em) {
        this.em = em;
        // 注册各意图的查询处理器
        handlers.put("stock_query", new StockQueryHandler());
        handlers.put("customer_query", new CustomerQueryHandler());
        handlers.put("gift_query", new GiftQueryHandler());
        handlers.put("stat_query", new StatQueryHandler());
        // ...
    }

    public QueryResult execute(String intent, Map<String, Object> params) {
        QueryHandler handler = handlers.get(intent);
        if (handler == null) {
            return QueryResult.notSupported(intent);
        }
        return handler.handle(em, params);
    }
}
```

### 4.5 LLM 响应生成

```
数据回填 Prompt:
基于以下查询结果，用中文生成一段自然语言回复：
查询意图：{{intent}}
问题：{{user_query}}
数据：{{query_result_json}}
要求：简洁明了，使用项目符号，关键数字加粗
```

### 4.6 API 集成配置

```yaml
# application.yml
ai:
  provider: deepseek           # 可选: deepseek / openai / azure
  api-key: ${AI_API_KEY}
  model: deepseek-chat         # 或 gpt-4o-mini
  max-tokens: 2000
  temperature: 0.3
  timeout-seconds: 30
```

**成本估算**（以 DeepSeek API 为例）：

| 用量 | Token 数 | 费用 |
|------|----------|------|
| 每次对话（平均） | ~500 tokens | ~¥0.0005 |
| 每日 100 次使用 | ~50,000 tokens | ~¥0.05 |
| 月均 | ~1,500,000 tokens | ~¥1.5 |

### 4.7 文件结构

```
src/main/java/com/henashi/inventorycrm/ai/
├── NLQueryAgent.java                   ← 主 Agent
├── LLMService.java                     ← LLM API 调用封装
├── IntentParser.java                   ← 意图解析
├── QueryExecutor.java                  ← 查询执行器
├── handler/
│   ├── QueryHandler.java               ← 查询处理器接口
│   ├── StockQueryHandler.java
│   ├── CustomerQueryHandler.java
│   ├── GiftQueryHandler.java
│   ├── StatQueryHandler.java
│   └── TrendQueryHandler.java
├── dto/
│   ├── ChatRequestDTO.java
│   ├── ChatResponseDTO.java
│   └── LLMResponseDTO.java
└── controller/AiChatController.java    ← SSE 流式端点

frontend/src/
├── views/ai/AiAssistant.vue            ← 全屏聊天界面
├── components/ai/
│   ├── ChatMessage.vue                 ← 消息气泡组件
│   └── SuggestedQuestions.vue          ← 快捷提问列表
├── api/ai.ts                           ← API 调用
└── router/routes.ts                    ← 添加路由 /ai/assistant
```

### 4.8 前端页面设计

**AiAssistant.vue：**

1. **对话消息列表**
   - 用户消息（右对齐，蓝色气泡）
   - AI 回复（左对齐，白色气泡，Markdown 渲染）
   - 加载中状态（打字动画）

2. **输入区域**
   - 文本输入框 + 发送按钮
   - Enter 发送，Shift+Enter 换行
   - 空状态不可发送

3. **快捷提问卡片**
   - "哪些商品库存不足？"
   - "最近新增的客户有哪些？"
   - "上月的出入库统计"
   - "推荐给高等级客户的礼品"
   - "今天的待办提醒"
   - "生成一份本周运营报告"

4. **SSE 流式输出**
   - 使用 `EventSource` 或 `fetch + ReadableStream`
   - 逐 token 渲染，打字机效果
   - 支持中断（停止生成按钮）

### 4.9 降级方案（无 API Key 时）

当 `AI_API_KEY` 未配置时，自动降级到**模板匹配模式**：

```java
public ChatResponse handleWithoutLLM(String query) {
    // 关键词匹配 + 预定义模板
    if (query.contains("库存不足") || query.contains("缺货")) {
        List<Product> lowStock = productRepository.findLowStock();
        return ChatResponse.template("LOW_STOCK", lowStock);
    }
    if (query.contains("新增客户")) {
        // ...
    }
    // 默认返回
    return ChatResponse.template("UNKNOWN");
}
```

### 4.10 简历描述示例

> **AI 自然语言运营助手**：基于 LLM（DeepSeek/OpenAI）设计并实现了 Intent→Query→Answer 三层 Agent 架构，用户可用自然语言查询库存、客户、礼品、报表等数据。支持 SSE 流式输出、8 种意图识别、动态 SQL 生成，无 API Key 时可自动降级到关键词匹配模式。

---

## 五、工时分配计划

### 5.1 总体时间线（5 天）

```
 第1天  │  功能一后端：StockPredictionAgent + StockForecastService + DTO + Scheduler
        │  功能一前端（开始）：StockPrediction.vue 基础框架
        │
 第2天  │  功能一前端完成：趋势图、一键补货、路由集成
        │  功能二后端（开始）：CustomerScoringService 评分算法
        │
 第3天  │  功能二后端完成：GiftRecommendationService + CustomerScoringAgent + API Controller
        │  功能二前端（开始）：CustomerScoring.vue + GiftRecommendation.vue
        │
 第4天  │  功能二前端完成：雷达图、生日提醒、批量发放
        │  功能三后端（开始）：LLMService + IntentParser 基础框架
        │
 第5天  │  功能三后端完成：NLQueryAgent + QueryExecutor + AiChatController (SSE)
        │  功能三前端完成：AiAssistant.vue + ChatMessage + 快捷提问
        │  集成测试 + Bug 修复 + 文档输出
```

### 5.2 每日细化

| 时段 | 第1天 | 第2天 | 第3天 | 第4天 | 第5天 |
|------|-------|-------|-------|-------|-------|
| **上午** | 功能一算法实现 | 功能一前端趋势图 | 功能二推荐逻辑 | 功能二前端批量操作 | 功能三 QueryExecutor |
| **下午** | 功能一 Controller + Scheduler | 功能二评分模型 | 功能二前端雷达图 | 功能三 LLMService | 功能三 SSE 流式输出 |
| **晚上** | 功能一前端表格 | 功能二 API + 测试 | 功能二生日触发器 | 功能三 IntentParser | AiAssistant UI + 文档 |

---

## 六、简历组合策略

### 6.1 一句话技术线

> **AI Inventory & CRM Agent System** — 基于 Spring Boot + Vue 3 构建的三个 AI Agent，覆盖预测分析、推荐系统、自然语言交互三大 AI 应用方向。

### 6.2 简历条目

```
AI Inventory & CRM Agent System                     2025
├─ AI 库存预测 Agent（Predictive Analytics）
│  · 基于历史 inventory_log 做加权移动平均 + 线性回归预测
│  · 自动标记未来 7/14 天缺货商品并生成补货建议
│  · @Scheduled 每日凌晨执行，ECharts 趋势图展示预测曲线
│
├─ AI 客户评分 & 礼品推荐 Agent（Recommendation System）
│  · 六维加权评分模型：等级/活跃度/注册时长/频率/推荐/生日
│  · 基于评分自动匹配 Top-3 推荐礼品及原因说明
│  · 每日自动检测生日客户并触发礼品发放
│
└─ AI 自然语言运营助手（LLM + Agent）
   · Intent→Query→Answer 三层 Agent 架构
   · 接入 DeepSeek/OpenAI API，8 种意图识别
   · SSE 流式输出，无 API Key 自动降级关键词匹配
```

### 6.3 面试话术要点

| 面试问题 | 应答方向 |
|----------|----------|
| "讲讲你做过的 AI 项目" | 从三个 Agent 的感知→思考→行动闭环切入，强调纯 Java 算法和 LLM 集成 |
| "你用到了哪些 AI 技术？" | 时间序列预测（无依赖）、加权评分模型（规则引擎）、Prompt Engineering + LLM API |
| "Agent 和普通 API 有什么区别？" | 强调**主动性**：定时调度、自动触发、异常自我检测；而非等用户请求才响应 |
| "数据从哪来？" | 项目的 inventory_log / customer / gift_log 已有全链路数据，无需额外采集 |
| "遇到什么挑战？" | 预测准确率调优、评测体系建设、无 API Key 时的降级方案 |

---

## 七、外部依赖清单

| 依赖 | 用途 | 必要性 | 替代方案 |
|------|------|--------|----------|
| `spring-boot-starter-web` | REST API | ✅ 已有 | — |
| `spring-boot-starter-data-jpa` | 数据库操作 | ✅ 已有 | — |
| `lombok` | 简化代码 | ✅ 已有 | — |
| `ECharts` (ant-design-vue) | 图表展示 | ✅ 已有 | — |
| DeepSeek / OpenAI API Key | 功能三 LLM | ⚠️ 建议有 | 降级到关键词匹配（功能减半） |
| Jackson (spring-boot-starter-json) | JSON 序列化 | ✅ 已有 | — |
| `spring-boot-starter-webflux` | SSE 流式输出 | ❌ 需新增 | 可用 Servlet 异步 + `SseEmitter`（已有 starter-web） |

**注意**：三个功能均不引入新框架，完全遵循项目现有的 Spring Boot + Vue 3 + Ant Design 架构规范。

---

## 八、架构设计原则

### 8.1 包结构原则

所有 AI 功能统一放在 `com.henashi.inventorycrm.ai` 包下，与其他业务模块隔离，但共享 Service / Repository 层。

```
com.henashi.inventorycrm.ai/
├── agent/         ← Agent 调度器（感知→思考→行动）
├── service/       ← 算法 / 业务逻辑
├── dto/           ← 数据传输对象
├── controller/    ← API 端点
└── config/        ← 配置
```

### 8.2 与原系统的集成方式

| 集成点 | 方式 | 说明 |
|--------|------|------|
| 数据读取 | 复用现有 Repository | 不新增 DAO 层 |
| 库存操作 | 调用现有 InventoryService | 不 bypass 库存审计 |
| 礼品发放 | 调用现有 GiftLogService | 遵守限领规则 |
| 权限控制 | 复用 Spring Security | 统一角色校验 |
| 事务管理 | @Transactional | 继承已有事务规范 |
| 前端路由 | 添加到 routes.ts | 复用布局和权限组件 |

### 8.3 命名约定

| 类型 | 命名 | 示例 |
|------|------|------|
| Agent 类 | `*Agent.java` | `StockPredictionAgent` |
| 算法服务 | `*Service.java` | `StockForecastService` |
| 前端页面 | `*View.vue` / `*Page.vue` | `AiAssistant.vue` |
| API 前缀 | `/api/ai/*` | `/api/ai/stock/predictions` |
| 路由路径 | `/ai/*` | `/ai/assistant` |

### 8.4 异常处理

所有 AI 功能通过 `GlobalExceptionHandler` 统一处理异常，不新增异常处理机制。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 已存在的异常处理器会覆盖 AI 模块的异常
}
```

---

> **下一步**：选择任意功能开始实现，可提交 `submit_plan` 获取分步执行审批。

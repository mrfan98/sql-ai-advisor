# 智能SQL优化顾问 - 系统架构设计文档

## 1. 架构概述

智能SQL优化顾问采用分层模块化架构设计，包括核心分析引擎、多模型AI接入层、规则引擎、缓存管理和MyBatis拦截层。系统通过模块化设计实现高内聚低耦合，便于后续扩展和维护。

**核心模块**：
- `sql-optimizer-core` - 核心分析引擎
- `sql-optimizer-starter` - **智能优化Starter（推荐）**，MyBatis拦截+审核+自动替换
- `sql-optimizer-spring-boot-starter` - Spring Boot自动配置
- `sql-optimizer-mybatis-plugin` - 独立MyBatis插件
- `sql-optimizer-console-api` - 控制台REST API

## 2. 技术栈选择

| 类别 | 技术/框架 | 版本 | 用途 |
|------|-----------|------|------|
| 基础语言 | Java | 17 | 核心开发语言 |
| SQL解析 | JSqlParser | 4.6+ | SQL语法解析和分析 |
| 数据库连接 | JDBC | - | 连接数据库获取元数据 |
| Spring Boot | Spring Boot | 3.2+ | 自动配置和依赖管理 |
| AI集成 | LangChain4j + HTTP直调 | 0.24.0 | 多模型统一接入 |
| 缓存 | Caffeine + Redis | - | 本地缓存+分布式缓存 |
| HTTP客户端 | Apache HttpClient5 | - | AI API调用 |
| JSON处理 | Gson | - | API响应解析 |
| MyBatis | MyBatis | 3.5+ | SQL拦截与改写 |
| 构建工具 | Maven | 3.8+ | 项目构建和依赖管理 |
| 测试框架 | JUnit | 5+ | 单元测试 |

## 3. 模块划分

### 3.1 核心模块 sql-optimizer-core

```
sql-optimizer-core/
├── ai/
│   ├── AiAdvisor.java              # AI优化顾问核心类
│   ├── model/
│   │   ├── AiProviderType.java   # AI提供商类型枚举
│   │   └── AiProviderConfig.java # Provider配置
│   └── provider/
│       ├── AiProvider.java        # Provider统一接口
│       ├── AbstractAiProvider.java # Provider抽象基类
│       ├── OpenAiProvider.java    # OpenAI实现
│       ├── ClaudeProvider.java    # Claude实现
│       ├── MiniMaxProvider.java   # MiniMax实现
│       ├── AiProviderFactory.java  # Provider工厂
│       └── AiProviderManager.java # Provider管理器
├── analyzer/
│   └── PerformanceAnalyzer.java    # 性能分析器
├── cache/
│   ├── SqlAnalysisCache.java      # 缓存接口
│   └── CaffeineCache.java        # Caffeine实现
├── database/
│   ├── DatabaseAdapter.java       # 数据库适配器接口
│   ├── DatabaseAdapterManager.java # 适配器管理器
│   ├── DatabaseType.java         # 数据库类型枚举
│   ├── MySqlAdapter.java         # MySQL适配器
│   ├── PostgreSqlAdapter.java    # PostgreSQL适配器
│   └── DmAdapter.java             # 达梦数据库适配器
├── model/
│   ├── OptimizationAdvice.java    # 优化建议
│   ├── OptimizationIssue.java     # 优化问题
│   └── OptimizationReport.java    # 优化报告
├── parser/
│   └── SqlParser.java             # SQL解析器
├── rule/
│   ├── SqlRule.java              # 规则接口
│   ├── SqlRuleEngine.java         # 规则引擎
│   ├── SelectAllColumnsRule.java # SELECT * 规则
│   ├── LimitMissingRule.java     # LIMIT缺失规则
│   └── ImplicitConversionRule.java # 隐式转换规则
└── service/
    ├── SqlOptimizerService.java   # 服务接口
    └── impl/
        └── SqlOptimizerServiceImpl.java  # 服务实现
```

### 3.2 sql-optimizer-starter（智能优化Starter）

**推荐生产环境使用**，集成MyBatis拦截、审核工作流和自动替换。

```
sql-optimizer-starter/
├── config/
│   ├── SqlOptimizerStarterProperties.java  # 配置属性类
│   └── SqlOptimizerAutoConfiguration.java  # Spring Boot自动配置
├── interceptor/
│   └── SqlOptimizerInterceptor.java         # MyBatis SQL拦截器
├── service/
│   └── SqlOptimizationService.java          # 核心优化服务
├── controller/
│   └── SqlReviewController.java            # 审核REST API
├── entity/
│   ├── SqlOptimizationRequest.java         # 优化请求实体
│   ├── SqlIssue.java                      # SQL问题实体
│   └── SqlAdvice.java                      # 优化建议实体
└── workflow/
    └── SqlReviewStatus.java               # 审核状态枚举
```

**核心流程**：
```
MyBatis SQL执行
    │
    ▼
SqlOptimizerInterceptor 拦截
    │
    ├── 判断是否慢查询 ──► 记录到Redis/DB
    │
    └── 异步提交AI分析
            │
            ▼
    SqlOptimizationService 分析
            │
            ├── 规则引擎预检
            ├── AI生成优化SQL
            │
            ▼
        根据审核模式
            │
            ├── MANUAL_REVIEW ──► 进入待审核队列
            ├── AUTO_APPROVE ──► 自动加入替换缓存
            │
            ▼
    审核API (人工确认)
            │
            ▼
    替换缓存更新 ──► 下次执行时自动替换SQL
```

### 3.3 sql-optimizer-spring-boot-starter
- **功能**：Spring Boot自动配置模块，提供自动装配和依赖注入

### 3.4 sql-optimizer-mybatis-plugin
- **功能**：独立MyBatis插件，可单独引入

### 3.5 sql-optimizer-console-api
- **功能**：控制台REST API，提供审核管理和趋势统计

## 4. AI多模型接入架构

### 4.1 核心接口

```java
public interface AiProvider {
    AiProviderConfig getConfig();
    String getProviderName();
    boolean validateApiKey();
    String chat(String... messages);
    String chat(String systemPrompt, String userMessage);
    int estimateTokens(String text);
    boolean supportsStreaming();
    boolean isAvailable();
    List<String> getSupportedModels();
}
```

### 4.2 支持的AI模型类型

| 类型 | 配置值 | Base URL | 支持模型 |
|------|--------|----------|----------|
| OpenAI | `OPENAI` | https://api.openai.com/v1 | GPT-4o, GPT-4o-mini, GPT-4, GPT-3.5-turbo |
| Claude | `CLAUDE` | https://api.anthropic.com/v1 | Claude-3.5-Sonnet, Claude-3-Opus |
| MiniMax | `MINIMAX` | https://api.minimax.chat/v1 | MiniMax-M2.7, abab6.5s-chat |
| Ollama | `OLLAMA` | http://localhost:11434/v1 | llama3.2, qwen2.5, mistral (本地模型) |
| Azure | `AZURE_OPENAI` | (用户配置) | Azure OpenAI部署的模型 |
| 自定义 | `CUSTOM` | (用户配置) | 任何OpenAI兼容API |

## 5. 配置项设计

### 5.1 sql-optimizer-starter 完整配置

```yaml
sql:
  optimizer:
    enabled: true                    # 是否启用
    mybatis:
      enabled: true                 # 是否启用MyBatis拦截
      intercept-mode: MANUAL_REVIEW  # ADVISORY_ONLY / MANUAL_REVIEW / AUTO_REPLACE
      slow-query-threshold-ms: 1000 # 慢查询阈值
      async-record: true            # 异步记录
      exclude-prefixes:              # 排除的SQL前缀
        - SELECT 1
        - SHOW
        - EXPLAIN
    workflow:
      review-mode: MANUAL           # MANUAL / AUTO_APPROVE_RULE_BASED / AUTO_APPROVE_ALL
      auto-approve-rule-based: true # 规则引擎问题自动通过
      expire-days: 30               # 审核过期天数
      max-pending-count: 1000      # 最大待审核数
    server:
      enabled: true                # 是否启用内嵌审核API
      port: 8088                   # API端口
```

## 6. 核心流程

### 6.1 SQL分析流程

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   SQL输入   │───▶│  SQL解析    │───▶│ 规则预检    │
└─────────────┘    └─────────────┘    └──────┬──────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    ▼                        ▼                        ▼
            ┌───────────┐            ┌───────────┐            ┌───────────┐
            │ 规则命中  │            │ 规则未命  │            │  无问题   │
            │ (直接返回)│            │ 中(调AI) │            │ (直接返回)│
            └───────────┘            └─────┬─────┘            └───────────┘
                                           │
                                           ▼
                                   ┌───────────────┐
                                   │ AI生成优化建议 │
                                   └───────────────┘
```

### 6.2 MyBatis拦截与审核流程

```
MyBatis Executor
      │
      ▼
SqlOptimizerInterceptor
      │
      ├── 记录执行时间
      │
      ├── 是否慢查询？ ──► 记录到SlowQuery表
      │
      ├── 异步提交分析
      │       │
      │       ▼
      │   SqlOptimizationService
      │       │
      │       ├── 规则引擎预检
      │       ├── AI生成优化SQL
      │       │
      │       ▼
      │   根据 review-mode
      │       │
      │       ├── MANUAL ──► PENDING (待审核队列)
      │       ├── AUTO_APPROVE_RULE_BASED ──► 规则问题自动批准
      │       └── AUTO_APPROVE_ALL ──► 全部自动批准
      │
      └── 检查是否有批准优化
              │
              ▼
      有批准优化 ──► 替换SQL执行
              │
              ▼
      SqlReviewController (REST API)
              │
              ├── GET /pending      # 获取待审核
              ├── POST /review      # 审核决策
              └── POST /execute     # 执行优化
```

## 7. 审核状态机

```
                    ┌──────────────┐
                    │   PENDING    │
                    │   待审核      │
                    └──────┬───────┘
                           │
          ┌────────────────┼────────────────┐
          │ approve        │ reject         │ timeout
          ▼                ▼                ▼
   ┌────────────┐   ┌────────────┐   ┌────────────┐
   │  APPROVED  │   │  REJECTED  │   │   EXPIRED  │
   │   已批准   │   │   已拒绝   │   │   已过期   │
   └──────┬─────┘   └────────────┘   └────────────┘
          │ apply
          ▼
   ┌────────────┐
   │  EXECUTED  │
   │   已执行   │
   └────────────┘
```

## 8. 数据库表设计

### 8.1 SQL优化记录表 (sql_optimization_request)
```sql
CREATE TABLE SQL_OPTIMIZATION_REQUEST (
    ID BIGINT PRIMARY KEY,
    ORIGINAL_SQL TEXT NOT NULL,
    OPTIMIZED_SQL TEXT,
    MAPPER_ID VARCHAR(256),
    DATABASE_TYPE VARCHAR(32),
    STATUS VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    ISSUES_JSON TEXT,
    ADVICES_JSON TEXT,
    ANALYSIS_TIME_MS BIGINT,
    REVIEWER VARCHAR(128),
    REVIEW_COMMENT TEXT,
    SUBMITTED_AT TIMESTAMP,
    REVIEWED_AT TIMESTAMP,
    EXECUTED_AT TIMESTAMP,
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 8.2 慢查询记录表 (slow_query_record)
```sql
CREATE TABLE SLOW_QUERY_RECORD (
    ID BIGINT PRIMARY KEY,
    SQL_TEXT TEXT NOT NULL,
    SQL_HASH VARCHAR(64) NOT NULL,
    EXECUTION_TIME_MS BIGINT NOT NULL,
    DATABASE_TYPE VARCHAR(32),
    SQL_TYPE VARCHAR(16),
    TABLE_NAME VARCHAR(128),
    FREQUENCY INT DEFAULT 1,
    ISSUES_JSON TEXT,
    EXECUTED_AT TIMESTAMP,
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 9. REST API

### 9.1 审核API (sql-optimizer-starter)

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/v1/sql-review/submit` | 提交SQL分析 |
| GET | `/api/v1/sql-review/pending` | 待审核列表 |
| GET | `/api/v1/sql-review/{id}` | 请求详情 |
| POST | `/api/v1/sql-review/{id}/review` | 审核决策 |
| GET | `/api/v1/sql-review/stats` | 审核统计 |

### 9.2 控制台API (sql-optimizer-console-api)

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/optimization/pending` | 待审核列表 |
| GET | `/api/v1/slow-queries` | 慢查询列表 |
| GET | `/api/v1/trends/slow-queries` | 慢查询趋势 |
| GET | `/api/v1/trends/issue-distribution` | 问题类型分布 |

## 10. 扩展性设计

### 10.1 AI模型扩展
- 实现AiProvider接口创建新的Provider
- 在AiProviderFactory中注册新类型

### 10.2 规则引擎扩展
- 实现SqlRule接口添加新规则
- 支持自定义规则优先级

### 10.3 缓存策略
- L1本地缓存（Caffeine）
- L2分布式缓存（Redis）
- 支持缓存失效策略

## 11. 与Spring Boot集成

```java
// 只需添加依赖并配置，拦截器自动生效
// 注入审核服务进行人工审核
@Autowired
private SqlOptimizationService optimizationService;

// 获取待审核列表
List<String> pendingIds = optimizationService.getPendingRequestIds(0, 20);

// 审核决策
optimizationService.review(requestId, true, "admin", "Approved");
```

## 12. 已完成功能

- [x] 规则引擎完善（SELECT *、LIMIT缺失、隐式转换等）
- [x] 缓存层完善（Caffeine本地缓存 + Redis分布式缓存）
- [x] 多数据库支持（MySQL、PostgreSQL、达梦）
- [x] **MyBatis插件集成**（sql-optimizer-starter）
- [x] **Web控制台API**（sql-optimizer-console-api）
- [x] **MiniMax Provider实现**

## 13. 后续扩展计划

- [ ] Gemini/DashScope/Wenxin Provider实现
- [ ] IDEA插件开发
- [ ] 趋势分析图表前端
- [ ] 质量门禁集成（Jenkins/GitLab CI）

# 智能SQL优化顾问

智能SQL优化顾问是一个Java组件，能够自动分析SQL语句，结合数据库元数据和AI能力，检测性能瓶颈（如全表扫描、缺失索引、隐式转换等），并生成优化建议和改写后的SQL。

**核心亮点**：通过 MyBatis Starter 实现运行时 SQL 拦截、AI优化分析、人工审核和自动替换。

## 核心特性

- **多模型支持**：OpenAI GPT、Claude、MiniMax、Ollama本地模型等，通过配置轻松切换
- **规则预校验**：AI调用前先进行规则校验，节省Token开销
- **多数据库支持**：MySQL、PostgreSQL、Oracle、达梦等
- **缓存加速**：本地缓存 + Redis分布式缓存，减少重复分析
- **MyBatis拦截**：运行时SQL拦截，智能审核与替换

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.sqloptimizer</groupId>
    <artifactId>sql-optimizer-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置

```yaml
sql:
  optimizer:
    enabled: true
    mybatis:
      enabled: true
      intercept-mode: MANUAL_REVIEW    # 推荐生产环境
      slow-query-threshold-ms: 1000
    workflow:
      review-mode: MANUAL            # 所有优化需人工确认
    ai:
      providers:
        - name: minimax
          type: MINIMAX
          api-key: ${MINIMAX_API_KEY}
          model: MiniMax-M2.7
    server:
      enabled: true
      port: 8088
```

### 3. 使用

引入后自动生效，MyBatis SQL会被拦截分析。

**审核API**：
```bash
# 查看待审核列表
curl http://localhost:8088/api/v1/sql-review/pending

# 审核决策
curl -X POST "http://localhost:8088/api/v1/sql-review/{id}/review?approved=true&reviewer=admin"
```

## 模块说明

| 模块 | 说明 | 推荐场景 |
|------|------|----------|
| **sql-optimizer-starter** | MyBatis拦截+审核+自动替换 | **生产环境推荐** |
| sql-optimizer-spring-boot-starter | 基础Spring Boot自动配置 | 轻量级集成 |
| sql-optimizer-mybatis-plugin | 独立MyBatis插件 | 自定义集成 |
| sql-optimizer-console-api | 控制台REST API | 管理界面后端 |
| sql-optimizer-core | 核心引擎 | 基础依赖 |

## 技术栈

| 组件 | 技术 |
|------|------|
| 基础框架 | JDK 17 + Spring Boot 3.x |
| AI集成 | LangChain4j + HTTP直调 |
| SQL解析 | JSqlParser |
| 缓存 | Caffeine + Redis |
| 规则引擎 | 自定义轻量规则 |
| MyBatis | 3.5+ SQL拦截与改写 |

## 项目结构

```
sql-optimizer-parent/
├── sql-optimizer-core/                    # 核心模块
│   └── java/com/sqloptimizer/core/
│       ├── ai/                           # AI Provider抽象层
│       ├── analyzer/                     # 性能分析器
│       ├── cache/                        # 缓存机制
│       ├── database/                     # 数据库适配器
│       ├── model/                        # 数据模型
│       ├── parser/                      # SQL解析器
│       ├── rule/                        # 规则引擎
│       └── service/                     # 服务层
├── sql-optimizer-starter/                # 智能优化Starter（推荐）
│   └── java/com/sqloptimizer/starter/
│       ├── config/                      # 自动配置
│       ├── controller/                  # 审核API
│       ├── entity/                      # 数据实体
│       ├── interceptor/                # MyBatis拦截器
│       ├── service/                      # 核心服务
│       └── workflow/                    # 审核状态机
└── sql-optimizer-console-api/           # 控制台API
```

## API列表

### 审核API (`/api/v1/sql-review`)

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /submit | 提交SQL分析 |
| GET | /pending | 待审核列表 |
| GET | /{id} | 请求详情 |
| POST | /{id}/review | 审核决策 |
| GET | /stats | 审核统计 |

### 控制台API (`/api/v1`)

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | /optimization/pending | 待审核列表 |
| POST | /optimization/{id}/apply | 执行优化SQL |
| GET | /slow-queries | 慢查询列表 |
| GET | /trends/slow-queries | 慢查询趋势 |
| GET | /trends/issue-distribution | 问题类型分布 |

## 许可证

MIT License


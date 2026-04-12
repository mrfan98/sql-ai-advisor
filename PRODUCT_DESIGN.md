# 智能SQL优化顾问 - 产品设计文档

## 1. 产品概述

智能SQL优化顾问是一个Java组件，能够自动分析SQL语句，结合数据库元数据和AI能力，检测性能瓶颈（如全表扫描、缺失索引、隐式转换等），并生成优化建议和改写后的SQL。它既可以作为Maven/Gradle依赖引入，也可以通过Spring Boot Starter自动配置，无缝集成到现有应用或CI流程中。

**核心亮点**：通过 MyBatis 拦截器实现运行时 SQL 分析，结合 AI 优化建议和人工审核流程，实现 SQL 的智能改写与应用。

## 2. 核心价值

- **帮助开发者在开发阶段快速发现SQL性能问题**
- **利用AI生成可读性强的建议和具体改进示例**
- **降低数据库优化的门槛，提升应用整体性能**
- **支持运行时自动拦截与智能审核，确保生产环境安全**

## 3. 核心功能

### 3.1 SQL分析
- 语法解析与验证
- 性能瓶颈检测（全表扫描、缺失索引、隐式转换等）
- 执行计划分析
- 数据库元数据收集与分析

### 3.2 优化建议
- 自动生成优化建议
- AI驱动的SQL改写
- 索引优化建议
- 执行计划优化建议

### 3.3 SQL智能改写（新增）
- **MyBatis拦截**：自动拦截Mapper执行的SQL
- **AI优化分析**：异步提交AI分析，生成优化SQL
- **人工审核**：提供REST API进行审核确认
- **自动替换**：审核通过后自动替换原SQL执行

### 3.4 集成方式
| 方式 | 说明 | 适用场景 |
|------|------|----------|
| sql-optimizer-starter | Spring Boot Starter，MyBatis拦截+审核+自动替换 | **生产环境推荐** |
| sql-optimizer-spring-boot-starter | Spring Boot自动配置 | 轻量级集成 |
| sql-optimizer-maven-plugin | Maven插件，构建时扫描 | CI/CD集成 |
| sql-optimizer-mybatis-plugin | 独立MyBatis插件 | 自定义集成 |

### 3.5 报告输出
- 详细的优化报告
- 历史分析记录
- 趋势分析图表

## 4. 核心模块

### 4.1 sql-optimizer-starter（智能优化Starter）
**推荐生产环境使用**

```
MyBatis SQL -> 拦截器 -> 异步AI分析 -> 优化建议 -> 人工审核 -> 自动替换/执行
```

**审核模式**：
- `MANUAL` - 所有优化需人工确认（生产推荐）
- `AUTO_APPROVE_RULE_BASED` - 规则引擎能解决的问题自动通过
- `AUTO_APPROVE_ALL` - 所有优化自动通过（仅开发环境）

### 4.2 sql-optimizer-core（核心引擎）
- AI优化顾问
- 规则引擎
- 多模型支持（OpenAI、Claude、MiniMax等）
- 缓存机制

## 5. 使用场景

### 5.1 开发阶段
- 开发人员编写SQL后进行性能检查
- MyBatis插件自动拦截分析
- 代码审查时的SQL性能评估

### 5.2 构建阶段
- CI/CD流程中的SQL质量检查
- Maven插件构建时自动扫描SQL文件

### 5.3 运行时（生产环境）
- MyBatis拦截器实时监控SQL
- 慢查询自动记录
- AI优化建议生成
- 人工审核后自动应用

## 6. 产品架构

```
┌─────────────────────────────────────────────────────────────┐
│                    sql-optimizer-starter                    │
│                  (智能优化Starter - 推荐)                     │
├─────────────────────────────────────────────────────────────┤
│  MyBatis拦截层  │  审核服务层  │  REST API层  │  缓存层      │
│  ┌───────────┐  │  ┌───────┐  │  ┌───────┐  │  ┌─────┐   │
│  │ 拦截器    │  │  │审核流程│  │  │审核API│  │  │Redis│   │
│  │ +替换    │  │  │+状态机 │  │  │+统计  │  │  │缓存 │   │
│  └───────────┘  │  └───────┘  │  └───────┘  │  └─────┘   │
├─────────────────────────────────────────────────────────────┤
│                    sql-optimizer-core                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ AI Advisor   │  │ 规则引擎   │  │ 多模型支持  │        │
│  │ +报告生成   │  │ +问题检测  │  │ OpenAI/Claude│        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

## 7. 快速开始

### 7.1 添加依赖
```xml
<dependency>
    <groupId>com.sqloptimizer</groupId>
    <artifactId>sql-optimizer-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 7.2 配置
```yaml
sql:
  optimizer:
    enabled: true
    mybatis:
      enabled: true
      intercept-mode: MANUAL_REVIEW    # 推荐生产环境
      slow-query-threshold-ms: 1000
    workflow:
      review-mode: MANUAL                # 所有优化需人工确认
    server:
      enabled: true
      port: 8088                        # 审核API端口
```

### 7.3 使用
引入后自动生效，MyBatis SQL会被拦截分析。

**审核API**：
```bash
# 查看待审核列表
curl http://localhost:8088/api/v1/sql-review/pending

# 审核决策
curl -X POST http://localhost:8088/api/v1/sql-review/{id}/review?approved=true
```

## 8. 后续扩展

- [x] **SQL智能改写**：AI生成优化SQL + 人工审核 + 自动应用
- [x] **MyBatis插件**：拦截执行前的SQL，实时分析
- [x] **Web控制台**：管理界面，查看历史记录和趋势
- [ ] 持续集成：集成到Jenkins/GitLab CI，作为质量门禁
- [ ] Gemini/DashScope/Wenxin Provider实现

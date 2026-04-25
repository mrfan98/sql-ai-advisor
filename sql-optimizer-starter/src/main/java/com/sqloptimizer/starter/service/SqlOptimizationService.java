package com.sqloptimizer.starter.service;

import com.sqloptimizer.starter.config.SqlOptimizerStarterProperties;
import com.sqloptimizer.starter.entity.SqlAdvice;
import com.sqloptimizer.starter.entity.SqlIssue;
import com.sqloptimizer.starter.entity.SqlOptimizationRequest;
import com.sqloptimizer.starter.workflow.SqlReviewStatus;
import com.sqloptimizer.core.database.DatabaseType;
import com.sqloptimizer.core.model.OptimizationAdvice;
import com.sqloptimizer.core.model.OptimizationIssue;
import com.sqloptimizer.core.model.OptimizationReport;
import com.sqloptimizer.core.rule.SqlRuleEngine;
import com.sqloptimizer.core.service.SqlOptimizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * SQL优化服务 - 核心服务，处理分析、审核、替换流程
 */
@Service
public class SqlOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(SqlOptimizationService.class);

    /**
     * 替换缓存Redis Key前缀
     */
    private static final String REPLACEMENT_CACHE_KEY_PREFIX = "sql_optimizer:replacement:";

    /**
     * 待审核队列 - 使用Redis List存储
     */
    private static final String PENDING_QUEUE_KEY = "sql_optimizer:pending_queue";

    /**
     * SQL请求存储前缀
     */
    private static final String REQUEST_KEY_PREFIX = "sql_optimizer:request:";

    /**
     * 限流窗口时间（秒）
     */
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60;

    /**
     * 限流阈值（每个窗口内最大提交数）
     */
    private static final int RATE_LIMIT_MAX_SUBMISSIONS = 100;

    /**
     * 限流计数器 key
     */
    private static final String RATE_LIMIT_KEY = "sql_optimizer:rate_limit:submit_count";

    private final SqlOptimizerService sqlOptimizerService;
    private final SqlRuleEngine ruleEngine;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SqlOptimizerStarterProperties properties;
    private final DataSource dataSource;

    public SqlOptimizationService(SqlOptimizerService sqlOptimizerService,
                                  SqlRuleEngine ruleEngine,
                                  RedisTemplate<String, Object> redisTemplate,
                                  SqlOptimizerStarterProperties properties,
                                  DataSource dataSource) {
        this.sqlOptimizerService = sqlOptimizerService;
        this.ruleEngine = ruleEngine;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 提交SQL进行优化分析
     * @param executionTimeMs 执行时间（毫秒），用于判断是否为慢查询，可为null
     * @return null 如果被限流拒绝
     */
    public SqlOptimizationRequest submitForOptimization(String sql, String mapperId, Long executionTimeMs) {
        // 限流检查
        if (!checkRateLimit()) {
            log.warn("Rate limit exceeded, submission rejected: mapperId={}, sql={}", mapperId, truncateSql(sql));
            return null;
        }

        long startTime = System.currentTimeMillis();

        // 自动检测数据库类型
        String databaseType = detectDatabaseType();

        // 规则引擎预分析
        List<OptimizationIssue> ruleIssues = ruleEngine.analyze(sql);
        boolean canHandleByRule = ruleIssues.isEmpty() || !ruleEngine.shouldCallAi(sql);

        // 根据审核模式决定后续流程
        SqlOptimizationRequest request = new SqlOptimizationRequest();
        request.setOriginalSql(sql);
        request.setMapperId(mapperId);
        request.setDatabaseType(databaseType);
        request.setExecutionTimeMs(executionTimeMs);
        request.setSubmittedAt(new Timestamp(System.currentTimeMillis()));
        request.setAnalysisTimeMs(System.currentTimeMillis() - startTime);

        SqlReviewStatus initialStatus;
        String optimizedSql = null;
        List<SqlAdvice> advices = null;
        List<SqlIssue> issues = null;

        if (properties.getWorkflow().getReviewMode() == SqlOptimizerStarterProperties.ReviewMode.AUTO_APPROVE_ALL) {
            // 自动通过模式 - 直接执行优化
            initialStatus = SqlReviewStatus.APPROVED;
            OptimizationReport report = sqlOptimizerService.analyze(sql, dataSource);
            optimizedSql = report.getOptimizedSql();
            issues = convertIssues(report.getIssues());
            advices = convertAdvices(report.getAdvice());
            saveReplacementToRedis(sql, optimizedSql);
        } else if (canHandleByRule && properties.getWorkflow().isAutoApproveRuleBased()) {
            // 规则引擎能处理，自动通过
            initialStatus = SqlReviewStatus.SKIPPED;
            optimizedSql = ruleEngine.autoFix(sql);
            issues = convertRuleIssues(ruleIssues);
            advices = convertRuleAdvices(ruleIssues);
            saveReplacementToRedis(sql, optimizedSql);
        } else {
            // 需要AI分析或人工审核
            OptimizationReport report = sqlOptimizerService.analyze(sql, dataSource);
            optimizedSql = report.getOptimizedSql();
            issues = convertIssues(report.getIssues());
            advices = convertAdvices(report.getAdvice());

            if (properties.getWorkflow().getReviewMode() == SqlOptimizerStarterProperties.ReviewMode.AUTO_APPROVE_RULE_BASED
                && issues.stream().allMatch(i -> i.getType() == SqlIssue.IssueType.OTHER || i.getType() == SqlIssue.IssueType.SELECT_ALL_COLUMNS)) {
                // 只有规则引擎能处理的问题，自动通过
                initialStatus = SqlReviewStatus.APPROVED;
                saveReplacementToRedis(sql, optimizedSql);
            } else {
                // 需要人工审核
                initialStatus = SqlReviewStatus.PENDING;
            }
        }

        request.setStatus(initialStatus);
        request.setOptimizedSql(optimizedSql);
        request.setIssues(issues);
        request.setAdvices(advices);
        request.setAnalysisTimeMs(System.currentTimeMillis() - startTime);

        // 保存到Redis
        String requestId = UUID.randomUUID().toString();
        String requestKey = REQUEST_KEY_PREFIX + requestId;
        redisTemplate.opsForValue().set(requestKey, request, properties.getWorkflow().getExpireDays(), TimeUnit.DAYS);

        // 加入待审核队列
        if (initialStatus == SqlReviewStatus.PENDING) {
            redisTemplate.opsForList().rightPush(PENDING_QUEUE_KEY, requestId);
            log.info("SQL submitted for review: id={}, mapper={}", requestId, mapperId);
        }

        return request;
    }

    /**
     * 限流检查
     * 使用 Redis INCR + EXPIRE 实现滑动窗口限流
     * @return true 如果允许提交，false 如果被限流拒绝
     */
    private boolean checkRateLimit() {
        try {
            String windowKey = RATE_LIMIT_KEY + ":" + (System.currentTimeMillis() / 1000 / RATE_LIMIT_WINDOW_SECONDS);
            Long count = redisTemplate.opsForValue().increment(windowKey);
            if (count != null && count == 1) {
                // 第一次设置该窗口，过期时间设为窗口大小的2倍
                redisTemplate.expire(windowKey, RATE_LIMIT_WINDOW_SECONDS * 2, TimeUnit.SECONDS);
            }
            return count == null || count <= RATE_LIMIT_MAX_SUBMISSIONS;
        } catch (Exception e) {
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
            return true; // Redis异常时允许通过，避免影响业务
        }
    }

    /**
     * 从DataSource自动检测数据库类型
     */
    private String detectDatabaseType() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String productName = metaData.getDatabaseProductName();
            DatabaseType dbType = DatabaseType.fromProductName(productName);
            return dbType != DatabaseType.UNKNOWN ? dbType.name() : productName;
        } catch (Exception e) {
            log.debug("Failed to detect database type: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * 获取待审核列表
     */
    public List<String> getPendingRequestIds(int page, int size) {
        List<Object> ids = redisTemplate.opsForList().range(PENDING_QUEUE_KEY, page * size, (page + 1) * size - 1);
        return ids != null ? ids.stream().map(Object::toString).collect(Collectors.toList()) : List.of();
    }

    /**
     * 获取请求详情
     */
    public SqlOptimizationRequest getRequest(String requestId) {
        String requestKey = REQUEST_KEY_PREFIX + requestId;
        return (SqlOptimizationRequest) redisTemplate.opsForValue().get(requestKey);
    }

    /**
     * 审核决策
     */
    public boolean review(String requestId, boolean approved, String reviewer, String comment) {
        SqlOptimizationRequest request = getRequest(requestId);
        if (request == null) {
            log.warn("Request not found: {}", requestId);
            return false;
        }

        if (!request.getStatus().canReview()) {
            log.warn("Request cannot be reviewed: id={}, status={}", requestId, request.getStatus());
            return false;
        }

        SqlReviewStatus newStatus = approved ? SqlReviewStatus.APPROVED : SqlReviewStatus.REJECTED;
        request.setStatus(newStatus);
        request.setReviewer(reviewer);
        request.setReviewComment(comment);
        request.setReviewedAt(new Timestamp(System.currentTimeMillis()));

        String requestKey = REQUEST_KEY_PREFIX + requestId;
        redisTemplate.opsForValue().set(requestKey, request, properties.getWorkflow().getExpireDays(), TimeUnit.DAYS);

        // 从待审核队列移除
        redisTemplate.opsForList().remove(PENDING_QUEUE_KEY, 1, requestId);

        // 如果批准，持久化到Redis替换缓存
        if (approved && request.getOptimizedSql() != null) {
            saveReplacementToRedis(request.getOriginalSql(), request.getOptimizedSql());
            log.info("SQL optimization approved: id={}, reviewer={}", requestId, reviewer);
        }

        return true;
    }

    /**
     * 从Redis获取优化后的SQL（如果有缓存的批准结果）
     */
    public String getOptimizedSql(String originalSql) {
        String cacheKey = REPLACEMENT_CACHE_KEY_PREFIX + generateSqlKey(originalSql);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            return cached != null ? cached.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to get optimized SQL from Redis: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查是否需要替换SQL
     */
    public boolean shouldReplace(String sql) {
        if (properties.getMybatis().getInterceptMode() != SqlOptimizerStarterProperties.InterceptMode.MANUAL_REVIEW) {
            return false;
        }
        return getOptimizedSql(sql) != null;
    }

    /**
     * 将替换关系持久化到Redis
     */
    private void saveReplacementToRedis(String originalSql, String optimizedSql) {
        String cacheKey = REPLACEMENT_CACHE_KEY_PREFIX + generateSqlKey(originalSql);
        try {
            redisTemplate.opsForValue().set(cacheKey, optimizedSql,
                    properties.getWorkflow().getExpireDays(), TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Failed to save replacement to Redis: originalSql={}, error={}",
                    truncateSql(originalSql), e.getMessage());
        }
    }

    /**
     * 执行优化SQL（用于已批准的请求）
     */
    public boolean executeOptimization(String requestId, javax.sql.DataSource dataSource) {
        SqlOptimizationRequest request = getRequest(requestId);
        if (request == null) {
            log.warn("Request not found: {}", requestId);
            return false;
        }

        if (!request.getStatus().canExecute()) {
            log.warn("Request cannot be executed: id={}, status={}", requestId, request.getStatus());
            return false;
        }

        String optimizedSql = request.getOptimizedSql();
        if (optimizedSql == null || optimizedSql.isEmpty()) {
            log.warn("No optimized SQL to execute: id={}", requestId);
            return false;
        }

        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {

            stmt.execute(optimizedSql);
            request.setStatus(SqlReviewStatus.EXECUTED);
            request.setExecutedAt(new Timestamp(System.currentTimeMillis()));

            String requestKey = REQUEST_KEY_PREFIX + requestId;
            redisTemplate.opsForValue().set(requestKey, request, properties.getWorkflow().getExpireDays(), TimeUnit.DAYS);

            log.info("SQL optimization executed: id={}", requestId);
            return true;
        } catch (Exception e) {
            log.error("Failed to execute optimized SQL: id={}", requestId, e);
            return false;
        }
    }

    /**
     * 获取待审核数量
     */
    public long getPendingCount() {
        Long size = redisTemplate.opsForList().size(PENDING_QUEUE_KEY);
        return size != null ? size : 0;
    }

    /**
     * 生成SQL缓存Key
     */
    private String generateSqlKey(String sql) {
        // 简单处理：去除多余空格后哈希
        String normalized = sql.trim().replaceAll("\\s+", " ");
        return String.valueOf(normalized.hashCode());
    }

    private String truncateSql(String sql) {
        if (sql == null) return "null";
        return sql.length() > 100 ? sql.substring(0, 100) + "..." : sql;
    }

    /**
     * 转换Issue列表
     */
    private List<SqlIssue> convertIssues(List<OptimizationIssue> issues) {
        if (issues == null) return List.of();
        return issues.stream().map(i -> {
            SqlIssue.IssueType type = mapIssueType(i.getType());
            return new SqlIssue(type, i.getDescription(), i.getSeverity());
        }).collect(Collectors.toList());
    }

    /**
     * 转换Advice列表
     */
    private List<SqlAdvice> convertAdvices(List<OptimizationAdvice> advices) {
        if (advices == null) return List.of();
        return advices.stream().map(a -> {
            SqlAdvice advice = new SqlAdvice(a.getTitle(), a.getDescription(), a.getRecommendation(), false);
            advice.setExampleSql(a.getExample());
            advice.setSource("AI");
            return advice;
        }).collect(Collectors.toList());
    }

    /**
     * 转换规则引擎问题
     */
    private List<SqlIssue> convertRuleIssues(List<OptimizationIssue> issues) {
        if (issues == null) return List.of();
        return issues.stream().map(i -> {
            SqlIssue.IssueType type = mapIssueType(i.getType());
            return new SqlIssue(type, i.getDescription(), i.getSeverity());
        }).collect(Collectors.toList());
    }

    /**
     * 转换规则引擎建议
     */
    private List<SqlAdvice> convertRuleAdvices(List<OptimizationIssue> issues) {
        if (issues == null) return List.of();
        return issues.stream().map(i -> {
            SqlAdvice advice = new SqlAdvice(i.getType().name(), i.getDescription(), "可自动优化", true);
            advice.setSource("RULE_ENGINE");
            return advice;
        }).collect(Collectors.toList());
    }

    /**
     * 映射问题类型
     */
    private SqlIssue.IssueType mapIssueType(OptimizationIssue.IssueType type) {
        if (type == null) return SqlIssue.IssueType.OTHER;
        switch (type) {
            case SELECT_ALL_COLUMNS: return SqlIssue.IssueType.SELECT_ALL_COLUMNS;
            case FULL_TABLE_SCAN: return SqlIssue.IssueType.FULL_TABLE_SCAN;
            case MISSING_INDEX: return SqlIssue.IssueType.MISSING_INDEX;
            case IMPLICIT_CONVERSION: return SqlIssue.IssueType.IMPLICIT_CONVERSION;
            case NESTED_LOOP: return SqlIssue.IssueType.NESTED_LOOP;
            case TEMP_TABLE: return SqlIssue.IssueType.TEMP_TABLE;
            case SORT_OPERATION: return SqlIssue.IssueType.SORT_OPERATION;
            default: return SqlIssue.IssueType.OTHER;
        }
    }
}

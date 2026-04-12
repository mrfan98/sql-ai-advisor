package com.sqloptimizer.starter.service;

import com.sqloptimizer.starter.config.SqlOptimizerStarterProperties;
import com.sqloptimizer.starter.entity.SqlAdvice;
import com.sqloptimizer.starter.entity.SqlIssue;
import com.sqloptimizer.starter.entity.SqlOptimizationRequest;
import com.sqloptimizer.starter.workflow.SqlReviewStatus;
import com.sqloptimizer.core.model.OptimizationAdvice;
import com.sqloptimizer.core.model.OptimizationIssue;
import com.sqloptimizer.core.model.OptimizationReport;
import com.sqloptimizer.core.rule.SqlRuleEngine;
import com.sqloptimizer.core.service.SqlOptimizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * SQL优化服务 - 核心服务，处理分析、审核、替换流程
 */
@Service
public class SqlOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(SqlOptimizationService.class);

    private final SqlOptimizerService sqlOptimizerService;
    private final SqlRuleEngine ruleEngine;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SqlOptimizerStarterProperties properties;

    /**
     * SQL替换缓存 - 审核通过后，原始SQL -> 优化SQL 的映射
     */
    private final Map<String, String> sqlReplacementCache = new ConcurrentHashMap<>();

    /**
     * 待审核队列 - 使用Redis List存储
     */
    private static final String PENDING_QUEUE_KEY = "sql_optimizer:pending_queue";

    /**
     * SQL请求存储前缀
     */
    private static final String REQUEST_KEY_PREFIX = "sql_optimizer:request:";

    public SqlOptimizationService(SqlOptimizerService sqlOptimizerService,
                                  SqlRuleEngine ruleEngine,
                                  RedisTemplate<String, Object> redisTemplate,
                                  SqlOptimizerStarterProperties properties) {
        this.sqlOptimizerService = sqlOptimizerService;
        this.ruleEngine = ruleEngine;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 提交SQL进行优化分析
     */
    public SqlOptimizationRequest submitForOptimization(String sql, String mapperId, String databaseType) {
        long startTime = System.currentTimeMillis();

        // 规则引擎预分析
        List<OptimizationIssue> ruleIssues = ruleEngine.analyze(sql);
        boolean canHandleByRule = ruleIssues.isEmpty() || !ruleEngine.shouldCallAi(sql);

        // 根据审核模式决定后续流程
        SqlOptimizationRequest request = new SqlOptimizationRequest();
        request.setOriginalSql(sql);
        request.setMapperId(mapperId);
        request.setDatabaseType(databaseType != null ? databaseType : "DM");
        request.setSubmittedAt(new Timestamp(System.currentTimeMillis()));
        request.setAnalysisTimeMs(System.currentTimeMillis() - startTime);

        SqlReviewStatus initialStatus;
        String optimizedSql = null;
        List<SqlAdvice> advices = null;
        List<SqlIssue> issues = null;

        if (properties.getWorkflow().getReviewMode() == SqlOptimizerStarterProperties.ReviewMode.AUTO_APPROVE_ALL) {
            // 自动通过模式 - 直接执行优化
            initialStatus = SqlReviewStatus.APPROVED;
            OptimizationReport report = sqlOptimizerService.analyze(sql, null);
            optimizedSql = report.getOptimizedSql();
            issues = convertIssues(report.getIssues());
            advices = convertAdvices(report.getAdvice());
            sqlReplacementCache.put(generateSqlKey(sql), optimizedSql);
        } else if (canHandleByRule && properties.getWorkflow().isAutoApproveRuleBased()) {
            // 规则引擎能处理，自动通过
            initialStatus = SqlReviewStatus.SKIPPED;
            optimizedSql = ruleEngine.autoFix(sql);
            issues = convertRuleIssues(ruleIssues);
            advices = convertRuleAdvices(ruleIssues);
            sqlReplacementCache.put(generateSqlKey(sql), optimizedSql);
        } else {
            // 需要AI分析或人工审核
            OptimizationReport report = sqlOptimizerService.analyze(sql, null);
            optimizedSql = report.getOptimizedSql();
            issues = convertIssues(report.getIssues());
            advices = convertAdvices(report.getAdvice());

            if (properties.getWorkflow().getReviewMode() == SqlOptimizerStarterProperties.ReviewMode.AUTO_APPROVE_RULE_BASED
                && issues.stream().allMatch(i -> i.getType() == SqlIssue.IssueType.OTHER || i.getType() == SqlIssue.IssueType.SELECT_ALL_COLUMNS)) {
                // 只有规则引擎能处理的问题，自动通过
                initialStatus = SqlReviewStatus.APPROVED;
                sqlReplacementCache.put(generateSqlKey(sql), optimizedSql);
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

        // 如果批准，加入替换缓存
        if (approved && request.getOptimizedSql() != null) {
            sqlReplacementCache.put(generateSqlKey(request.getOriginalSql()), request.getOptimizedSql());
            log.info("SQL optimization approved: id={}, reviewer={}", requestId, reviewer);
        }

        return true;
    }

    /**
     * 获取优化后的SQL（如果有缓存的批准结果）
     */
    public String getOptimizedSql(String originalSql) {
        return sqlReplacementCache.get(generateSqlKey(originalSql));
    }

    /**
     * 检查是否需要替换SQL
     */
    public boolean shouldReplace(String sql) {
        if (properties.getMybatis().getInterceptMode() != SqlOptimizerStarterProperties.InterceptMode.MANUAL_REVIEW) {
            return false;
        }
        return sqlReplacementCache.containsKey(generateSqlKey(sql));
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

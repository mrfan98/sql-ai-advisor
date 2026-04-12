package com.sqloptimizer.core.rule;

import com.sqloptimizer.core.model.OptimizationIssue;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 缺失LIMIT规则
 * 检测SELECT语句没有LIMIT限制，可能导致返回过多数据
 */
public class LimitMissingRule implements SqlRule {

    // 匹配 SELECT ... FROM（不带 LIMIT）
    private static final Pattern SELECT_NO_LIMIT_PATTERN = Pattern.compile(
            "(?i)SELECT\\s+.*\\s+FROM\\s+"
    );

    // 排除已带LIMIT的SELECT
    private static final Pattern HAS_LIMIT_PATTERN = Pattern.compile(
            "(?i)LIMIT\\s+\\d+",
            Pattern.CASE_INSENSITIVE
    );

    // 匹配 INSERT INTO ... VALUES, UPDATE, DELETE（不需要LIMIT）
    private static final Pattern WRITE_OPS_PATTERN = Pattern.compile(
            "(?i)^\\s*(INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE)\\s+",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "LIMIT_MISSING";
    }

    @Override
    public String getDescription() {
        return "检测可能返回大量数据的SELECT语句缺少LIMIT限制";
    }

    @Override
    public boolean match(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String trimmedSql = sql.trim();

        // 写操作不需要检查LIMIT
        if (WRITE_OPS_PATTERN.matcher(trimmedSql).find()) {
            return false;
        }

        // SELECT语句没有LIMIT
        if (!SELECT_NO_LIMIT_PATTERN.matcher(trimmedSql).find()) {
            return false;
        }

        // 已经带LIMIT的不需要检查
        if (HAS_LIMIT_PATTERN.matcher(trimmedSql).find()) {
            return false;
        }

        return true;
    }

    @Override
    public List<OptimizationIssue> analyze(String sql) {
        if (!match(sql)) {
            return List.of();
        }

        OptimizationIssue issue = new OptimizationIssue(
                OptimizationIssue.IssueType.OTHER,
                "SELECT语句缺少LIMIT限制，可能返回过多数据导致内存溢出或性能问题",
                "中",
                sql
        );

        return List.of(issue);
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public boolean isAutoFixable() {
        return true;
    }

    @Override
    public String autoFix(String sql) {
        if (!match(sql)) {
            return sql;
        }
        // 简单地在SQL末尾添加 LIMIT 1000
        // 实际使用时可能需要更智能的处理
        return sql.trim() + " LIMIT 1000";
    }
}

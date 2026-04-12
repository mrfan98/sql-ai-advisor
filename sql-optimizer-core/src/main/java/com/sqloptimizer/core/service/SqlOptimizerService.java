package com.sqloptimizer.core.service;

import com.sqloptimizer.core.model.OptimizationAdvice;
import com.sqloptimizer.core.model.OptimizationIssue;
import com.sqloptimizer.core.model.OptimizationReport;

import javax.sql.DataSource;
import java.util.List;

public interface SqlOptimizerService {

    /**
     * 分析SQL并生成优化报告
     *
     * @param sql        SQL语句
     * @param dataSource 数据源
     * @return 优化报告
     */
    OptimizationReport analyze(String sql, DataSource dataSource);

    /**
     * 检测SQL中的性能问题
     *
     * @param sql        SQL语句
     * @param dataSource 数据源
     * @return 性能问题列表
     */
    List<OptimizationIssue> detectIssues(String sql, DataSource dataSource);

    /**
     * 生成优化建议
     *
     * @param issues 性能问题列表
     * @return 优化建议列表
     */
    List<OptimizationAdvice> generateAdvice(List<OptimizationIssue> issues);

    /**
     * 生成优化后的SQL
     *
     * @param originalSql 原始SQL
     * @param issues      性能问题列表
     * @return 优化后的SQL
     */
    String generateOptimizedSql(String originalSql, List<OptimizationIssue> issues);
}

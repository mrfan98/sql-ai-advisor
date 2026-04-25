package com.sqloptimizer.starter.entity;

import com.sqloptimizer.starter.workflow.SqlReviewStatus;

import java.sql.Timestamp;
import java.util.List;

/**
 * SQL优化请求记录
 */
public class SqlOptimizationRequest {

    private Long id;

    /**
     * 原始SQL
     */
    private String originalSql;

    /**
     * mapper接口/方法标识
     */
    private String mapperId;

    /**
     * 数据库类型
     */
    private String databaseType;

    /**
     * 审核状态
     */
    private SqlReviewStatus status;

    /**
     * 优化后的SQL
     */
    private String optimizedSql;

    /**
     * 检测到的问题列表
     */
    private List<SqlIssue> issues;

    /**
     * 优化建议
     */
    private List<SqlAdvice> advices;

    /**
     * 分析耗时(毫秒)
     */
    private Long analysisTimeMs;

    /**
     * 审核人
     */
    private String reviewer;

    /**
     * 审核意见
     */
    private String reviewComment;

    /**
     * 提交时间
     */
    private Timestamp submittedAt;

    /**
     * 审核时间
     */
    private Timestamp reviewedAt;

    /**
     * SQL执行时间（毫秒），用于判断是否为慢查询
     */
    private Long executionTimeMs;

    /**
     * 执行时间
     */
    private Timestamp executedAt;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    public SqlOptimizationRequest() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalSql() {
        return originalSql;
    }

    public void setOriginalSql(String originalSql) {
        this.originalSql = originalSql;
    }

    public String getMapperId() {
        return mapperId;
    }

    public void setMapperId(String mapperId) {
        this.mapperId = mapperId;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public SqlReviewStatus getStatus() {
        return status;
    }

    public void setStatus(SqlReviewStatus status) {
        this.status = status;
    }

    public String getOptimizedSql() {
        return optimizedSql;
    }

    public void setOptimizedSql(String optimizedSql) {
        this.optimizedSql = optimizedSql;
    }

    public List<SqlIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<SqlIssue> issues) {
        this.issues = issues;
    }

    public List<SqlAdvice> getAdvices() {
        return advices;
    }

    public void setAdvices(List<SqlAdvice> advices) {
        this.advices = advices;
    }

    public Long getAnalysisTimeMs() {
        return analysisTimeMs;
    }

    public void setAnalysisTimeMs(Long analysisTimeMs) {
        this.analysisTimeMs = analysisTimeMs;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Timestamp getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Timestamp executedAt) {
        this.executedAt = executedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

package com.sqloptimizer.console.entity;

import com.sqloptimizer.console.workflow.ReviewStatus;

import java.sql.Timestamp;

/**
 * SQL优化记录实体
 */
public class OptimizationRecord {

    private Long id;
    private String originalSql;
    private String optimizedSql;
    private String databaseType;
    private ReviewStatus status;
    private String reviewer;
    private String reviewComment;
    private Long analysisTimeMs;
    private String issuesJson;
    private String adviceJson;
    private Timestamp submittedAt;
    private Timestamp reviewedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public OptimizationRecord() {
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

    public String getOptimizedSql() {
        return optimizedSql;
    }

    public void setOptimizedSql(String optimizedSql) {
        this.optimizedSql = optimizedSql;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
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

    public Long getAnalysisTimeMs() {
        return analysisTimeMs;
    }

    public void setAnalysisTimeMs(Long analysisTimeMs) {
        this.analysisTimeMs = analysisTimeMs;
    }

    public String getIssuesJson() {
        return issuesJson;
    }

    public void setIssuesJson(String issuesJson) {
        this.issuesJson = issuesJson;
    }

    public String getAdviceJson() {
        return adviceJson;
    }

    public void setAdviceJson(String adviceJson) {
        this.adviceJson = adviceJson;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}

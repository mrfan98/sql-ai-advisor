package com.sqloptimizer.console.entity;

import java.sql.Timestamp;

/**
 * 慢查询记录实体
 */
public class SlowQueryRecord {

    private Long id;
    private String sqlText;
    private String sqlHash;
    private Long executionTimeMs;
    private String databaseType;
    private String sqlType;
    private String tableName;
    private Integer frequency;
    private String issuesJson;
    private Timestamp executedAt;
    private Timestamp createdAt;

    public SlowQueryRecord() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getSqlHash() {
        return sqlHash;
    }

    public void setSqlHash(String sqlHash) {
        this.sqlHash = sqlHash;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }

    public String getIssuesJson() {
        return issuesJson;
    }

    public void setIssuesJson(String issuesJson) {
        this.issuesJson = issuesJson;
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

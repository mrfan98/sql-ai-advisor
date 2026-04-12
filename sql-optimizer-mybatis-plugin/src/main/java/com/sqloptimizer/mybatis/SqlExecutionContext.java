package com.sqloptimizer.mybatis;

import java.util.List;

/**
 * SQL执行上下文信息
 */
public class SqlExecutionContext {

    private String sql;
    private String sqlHash;
    private long executionTimeMs;
    private String databaseType;
    private List<String> tableNames;
    private String sqlType;  // SELECT, INSERT, UPDATE, DELETE, OTHER
    private Object[] parameters;
    private String statementId;

    public SqlExecutionContext() {
    }

    public SqlExecutionContext(String sql, long executionTimeMs) {
        this.sql = sql;
        this.executionTimeMs = executionTimeMs;
    }

    // Getters and Setters
    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getSqlHash() {
        return sqlHash;
    }

    public void setSqlHash(String sqlHash) {
        this.sqlHash = sqlHash;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public void setTableNames(List<String> tableNames) {
        this.tableNames = tableNames;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    @Override
    public String toString() {
        return "SqlExecutionContext{" +
                "sql='" + sql + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                ", databaseType='" + databaseType + '\'' +
                ", sqlType='" + sqlType + '\'' +
                '}';
    }
}

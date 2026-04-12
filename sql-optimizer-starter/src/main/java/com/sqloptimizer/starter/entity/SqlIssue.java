package com.sqloptimizer.starter.entity;

/**
 * SQL问题
 */
public class SqlIssue {

    /**
     * 问题类型
     */
    private IssueType type;

    /**
     * 问题描述
     */
    private String description;

    /**
     * 严重程度: LOW, MEDIUM, HIGH, CRITICAL
     */
    private String severity;

    /**
     * 问题位置
     */
    private String location;

    public SqlIssue() {
    }

    public SqlIssue(IssueType type, String description, String severity) {
        this.type = type;
        this.description = description;
        this.severity = severity;
    }

    public IssueType getType() {
        return type;
    }

    public void setType(IssueType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 问题类型枚举
     */
    public enum IssueType {
        SELECT_ALL_COLUMNS("使用SELECT *"),
        MISSING_INDEX("缺失索引"),
        FULL_TABLE_SCAN("全表扫描"),
        IMPLICIT_CONVERSION("隐式类型转换"),
        NESTED_LOOP("嵌套循环"),
        TEMP_TABLE("临时表"),
        SORT_OPERATION("排序操作"),
        OTHER("其他问题");

        private final String description;

        IssueType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}

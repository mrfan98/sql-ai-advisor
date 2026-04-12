package com.sqloptimizer.starter.entity;

/**
 * SQL优化建议
 */
public class SqlAdvice {

    /**
     * 建议标题
     */
    private String title;

    /**
     * 建议描述
     */
    private String description;

    /**
     * 建议内容
     */
    private String recommendation;

    /**
     * 示例SQL
     */
    private String exampleSql;

    /**
     * 是否可自动应用
     */
    private boolean autoApplicable;

    /**
     * 来源: RULE_ENGINE, AI
     */
    private String source;

    public SqlAdvice() {
    }

    public SqlAdvice(String title, String description, String recommendation, boolean autoApplicable) {
        this.title = title;
        this.description = description;
        this.recommendation = recommendation;
        this.autoApplicable = autoApplicable;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getExampleSql() {
        return exampleSql;
    }

    public void setExampleSql(String exampleSql) {
        this.exampleSql = exampleSql;
    }

    public boolean isAutoApplicable() {
        return autoApplicable;
    }

    public void setAutoApplicable(boolean autoApplicable) {
        this.autoApplicable = autoApplicable;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}

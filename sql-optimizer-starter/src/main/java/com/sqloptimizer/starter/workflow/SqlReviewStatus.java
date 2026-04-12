package com.sqloptimizer.starter.workflow;

/**
 * SQL审核状态
 */
public enum SqlReviewStatus {

    /**
     * 待审核
     */
    PENDING("待审核"),

    /**
     * 已批准（可执行）
     */
    APPROVED("已批准"),

    /**
     * 已拒绝
     */
    REJECTED("已拒绝"),

    /**
     * 已执行
     */
    EXECUTED("已执行"),

    /**
     * 已过期
     */
    EXPIRED("已过期"),

    /**
     * 跳过（规则引擎自动处理）
     */
    SKIPPED("已跳过");

    private final String description;

    SqlReviewStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canReview() {
        return this == PENDING;
    }

    public boolean canExecute() {
        return this == APPROVED;
    }

    public boolean isFinal() {
        return this == REJECTED || this == EXECUTED || this == EXPIRED || this == SKIPPED;
    }
}

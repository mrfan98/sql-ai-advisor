package com.sqloptimizer.console.workflow;

/**
 * 审核状态枚举
 */
public enum ReviewStatus {

    /**
     * 待审核
     */
    PENDING("待审核"),

    /**
     * 已通过
     */
    APPROVED("已通过"),

    /**
     * 已拒绝
     */
    REJECTED("已拒绝"),

    /**
     * 已应用(优化SQL已执行)
     */
    APPLIED("已应用"),

    /**
     * 已过期
     */
    EXPIRED("已过期");

    private final String description;

    ReviewStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查是否可以进行审核
     */
    public boolean canReview() {
        return this == PENDING;
    }

    /**
     * 检查是否可以执行优化SQL
     */
    public boolean canApply() {
        return this == APPROVED;
    }

    /**
     * 检查是否已结束(不再需要处理)
     */
    public boolean isFinal() {
        return this == REJECTED || this == APPLIED || this == EXPIRED;
    }
}

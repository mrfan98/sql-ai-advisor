package com.sqloptimizer.console.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 审核决策请求
 */
public class ReviewDecisionRequest {

    @NotBlank(message = "决策不能为空")
    private String decision;  // APPROVE 或 REJECT

    private String comment;

    private String operator;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}

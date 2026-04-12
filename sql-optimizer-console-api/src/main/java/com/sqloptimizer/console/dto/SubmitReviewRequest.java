package com.sqloptimizer.console.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 提交审核请求
 */
public class SubmitReviewRequest {

    @NotBlank(message = "SQL不能为空")
    private String sql;

    private String dataSourceId;

    private boolean executeImmediately = false;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public boolean isExecuteImmediately() {
        return executeImmediately;
    }

    public void setExecuteImmediately(boolean executeImmediately) {
        this.executeImmediately = executeImmediately;
    }
}

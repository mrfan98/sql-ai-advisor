package com.sqloptimizer.mybatis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis插件配置属性
 */
@ConfigurationProperties(prefix = "sql.optimizer.mybatis")
public class MyBatisPluginProperties {

    /**
     * 是否启用插件
     */
    private boolean enabled = true;

    /**
     * 是否启用SQL拦截
     */
    private boolean interceptEnabled = true;

    /**
     * 慢查询阈值(毫秒)，超过此阈值的SQL将被记录
     */
    private long slowQueryThresholdMs = 1000;

    /**
     * 是否异步记录慢查询
     */
    private boolean asyncRecord = true;

    /**
     * Redis键前缀
     */
    private String redisKeyPrefix = "sql_optimizer:mybatis:";

    /**
     * 慢查询统计TTL(天)
     */
    private int statsTtlDays = 90;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInterceptEnabled() {
        return interceptEnabled;
    }

    public void setInterceptEnabled(boolean interceptEnabled) {
        this.interceptEnabled = interceptEnabled;
    }

    public long getSlowQueryThresholdMs() {
        return slowQueryThresholdMs;
    }

    public void setSlowQueryThresholdMs(long slowQueryThresholdMs) {
        this.slowQueryThresholdMs = slowQueryThresholdMs;
    }

    public boolean isAsyncRecord() {
        return asyncRecord;
    }

    public void setAsyncRecord(boolean asyncRecord) {
        this.asyncRecord = asyncRecord;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public int getStatsTtlDays() {
        return statsTtlDays;
    }

    public void setStatsTtlDays(int statsTtlDays) {
        this.statsTtlDays = statsTtlDays;
    }
}

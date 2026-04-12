package com.sqloptimizer.console;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 控制台配置属性
 */
@ConfigurationProperties(prefix = "sql.optimizer.console")
public class ConsoleProperties {

    /**
     * 是否启用控制台API
     */
    private boolean enabled = true;

    /**
     * 审核过期时间(天)
     */
    private int reviewExpireDays = 30;

    /**
     * 最大待审核记录数
     */
    private int maxPendingRecords = 1000;

    /**
     * Redis键前缀
     */
    private String redisKeyPrefix = "sql_optimizer:console:";

    /**
     * 分页默认大小
     */
    private int defaultPageSize = 20;

    /**
     * 分页最大大小
     */
    private int maxPageSize = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getReviewExpireDays() {
        return reviewExpireDays;
    }

    public void setReviewExpireDays(int reviewExpireDays) {
        this.reviewExpireDays = reviewExpireDays;
    }

    public int getMaxPendingRecords() {
        return maxPendingRecords;
    }

    public void setMaxPendingRecords(int maxPendingRecords) {
        this.maxPendingRecords = maxPendingRecords;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
}

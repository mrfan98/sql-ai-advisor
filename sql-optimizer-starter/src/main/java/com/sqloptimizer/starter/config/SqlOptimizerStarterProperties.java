package com.sqloptimizer.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SQL智能优化Starter配置属性
 */
@ConfigurationProperties(prefix = "sql.optimizer")
public class SqlOptimizerStarterProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * MyBatis SQL拦截配置
     */
    private Mybatis mybatis = new Mybatis();

    /**
     * 审核工作流配置
     */
    private Workflow workflow = new Workflow();

    /**
     * 内嵌审核服务器配置
     */
    private Server server = new Server();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Mybatis getMybatis() {
        return mybatis;
    }

    public void setMybatis(Mybatis mybatis) {
        this.mybatis = mybatis;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * MyBatis拦截配置
     */
    public static class Mybatis {
        /**
         * 是否启用SQL拦截
         */
        private boolean enabled = true;

        /**
         * 慢查询阈值(毫秒)
         */
        private long slowQueryThresholdMs = 1000;

        /**
         * 是否异步记录
         */
        private boolean asyncRecord = true;

        /**
         * 拦截模式
         * - ADVISORY_ONLY: 只记录，不替换
         * - AUTO_REPLACE: 自动替换（仅开发模式）
         * - MANUAL_REVIEW: 人工审核后替换（生产推荐）
         */
        private InterceptMode interceptMode = InterceptMode.MANUAL_REVIEW;

        /**
         * 排除的SQL语句前缀（不进行分析）
         */
        private String[] excludePrefixes = {"SELECT 1", "SHOW", "EXPLAIN", "DESCRIBE"};

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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

        public InterceptMode getInterceptMode() {
            return interceptMode;
        }

        public void setInterceptMode(InterceptMode interceptMode) {
            this.interceptMode = interceptMode;
        }

        public String[] getExcludePrefixes() {
            return excludePrefixes;
        }

        public void setExcludePrefixes(String[] excludePrefixes) {
            this.excludePrefixes = excludePrefixes;
        }
    }

    /**
     * 拦截模式
     */
    public enum InterceptMode {
        /**
         * 仅记录，不替换
         */
        ADVISORY_ONLY,

        /**
         * 人工审核后替换（推荐生产环境）
         */
        MANUAL_REVIEW,

        /**
         * 自动替换（仅开发环境）
         */
        AUTO_REPLACE
    }

    /**
     * 审核工作流配置
     */
    public static class Workflow {
        /**
         * 审核模式
         * - MANUAL: 所有优化需人工确认
         * - AUTO_APPROVE_RULE_BASED: 规则引擎能解决的自动通过
         * - AUTO_APPROVE_ALL: 所有优化自动通过（危险）
         */
        private ReviewMode reviewMode = ReviewMode.MANUAL;

        /**
         * 自动审批规则引擎可处理的问题
         */
        private boolean autoApproveRuleBased = true;

        /**
         * 审批超时时间(天)
         */
        private int expireDays = 30;

        /**
         * 最大待审批数量
         */
        private int maxPendingCount = 1000;

        public ReviewMode getReviewMode() {
            return reviewMode;
        }

        public void setReviewMode(ReviewMode reviewMode) {
            this.reviewMode = reviewMode;
        }

        public boolean isAutoApproveRuleBased() {
            return autoApproveRuleBased;
        }

        public void setAutoApproveRuleBased(boolean autoApproveRuleBased) {
            this.autoApproveRuleBased = autoApproveRuleBased;
        }

        public int getExpireDays() {
            return expireDays;
        }

        public void setExpireDays(int expireDays) {
            this.expireDays = expireDays;
        }

        public int getMaxPendingCount() {
            return maxPendingCount;
        }

        public void setMaxPendingCount(int maxPendingCount) {
            this.maxPendingCount = maxPendingCount;
        }
    }

    /**
     * 审核模式
     */
    public enum ReviewMode {
        /**
         * 所有优化需人工确认
         */
        MANUAL,

        /**
         * 规则引擎能解决的自动通过
         */
        AUTO_APPROVE_RULE_BASED,

        /**
         * 所有优化自动通过（危险）
         */
        AUTO_APPROVE_ALL
    }

    /**
     * 内嵌审核服务器配置
     */
    public static class Server {
        /**
         * 是否启用内嵌审核服务器
         */
        private boolean enabled = true;

        /**
         * 监听端口
         */
        private int port = 8088;

        /**
         * 是否允许远程访问
         */
        private boolean allowRemoteAccess = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isAllowRemoteAccess() {
            return allowRemoteAccess;
        }

        public void setAllowRemoteAccess(boolean allowRemoteAccess) {
            this.allowRemoteAccess = allowRemoteAccess;
        }
    }
}

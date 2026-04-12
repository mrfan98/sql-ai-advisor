package com.sqloptimizer.springboot;

import com.sqloptimizer.core.ai.model.AiProviderConfig;
import com.sqloptimizer.core.ai.model.AiProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL Optimizer配置属性
 */
@ConfigurationProperties(prefix = "sql.optimizer")
public class SqlOptimizerProperties {

    private boolean enabled = true;
    private Ai ai = new Ai();
    private Report report = new Report();
    private Cache cache = new Cache();
    private Rule rule = new Rule();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    /**
     * AI配置
     */
    public static class Ai {
        private boolean enabled = true;
        private boolean enableRuleFirst = true;
        private List<Provider> providers = new ArrayList<>();
        private String defaultProvider;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnableRuleFirst() {
            return enableRuleFirst;
        }

        public void setEnableRuleFirst(boolean enableRuleFirst) {
            this.enableRuleFirst = enableRuleFirst;
        }

        public List<Provider> getProviders() {
            return providers;
        }

        public void setProviders(List<Provider> providers) {
            this.providers = providers;
        }

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }

        /**
         * 兼容旧版配置
         */
        private String apiKey;
        private String model = "gpt-4o-mini";
        private String baseUrl;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    /**
     * AI Provider配置
     */
    public static class Provider {
        private String name;
        private String type;
        private String apiKey;
        private String model;
        private String baseUrl;
        private Float temperature = 0.7f;
        private Integer maxTokens = 4096;
        private boolean enabled = true;
        private String apiKeyEnvVar;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Float getTemperature() {
            return temperature;
        }

        public void setTemperature(Float temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKeyEnvVar() {
            return apiKeyEnvVar;
        }

        public void setApiKeyEnvVar(String apiKeyEnvVar) {
            this.apiKeyEnvVar = apiKeyEnvVar;
        }

        /**
         * 转换为核心模块配置
         */
        public AiProviderConfig toProviderConfig() {
            AiProviderType providerType = null;
            if (type != null) {
                try {
                    providerType = AiProviderType.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            AiProviderConfig.Builder builder = AiProviderConfig.builder()
                    .name(name)
                    .type(providerType)
                    .apiKey(apiKey)
                    .model(model)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .enabled(enabled)
                    .apiKeyEnvVar(apiKeyEnvVar);

            if (baseUrl != null && !baseUrl.isEmpty()) {
                builder.baseUrl(baseUrl);
            }

            return builder.build();
        }
    }

    /**
     * 报告配置
     */
    public static class Report {
        private String outputPath = "target/sql-optimizer-reports";
        private String format = "html";

        public String getOutputPath() {
            return outputPath;
        }

        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    /**
     * 缓存配置
     */
    public static class Cache {
        private boolean enabled = true;
        private long ttlSeconds = 3600;
        private long maxSize = 1000;
        private boolean enableDistributed = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }

        public boolean isEnableDistributed() {
            return enableDistributed;
        }

        public void setEnableDistributed(boolean enableDistributed) {
            this.enableDistributed = enableDistributed;
        }
    }

    /**
     * 规则引擎配置
     */
    public static class Rule {
        private boolean enabled = true;
        private boolean ruleFirst = true;
        private List<String> enabledRuleTypes;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRuleFirst() {
            return ruleFirst;
        }

        public void setRuleFirst(boolean ruleFirst) {
            this.ruleFirst = ruleFirst;
        }

        public List<String> getEnabledRuleTypes() {
            return enabledRuleTypes;
        }

        public void setEnabledRuleTypes(List<String> enabledRuleTypes) {
            this.enabledRuleTypes = enabledRuleTypes;
        }
    }
}

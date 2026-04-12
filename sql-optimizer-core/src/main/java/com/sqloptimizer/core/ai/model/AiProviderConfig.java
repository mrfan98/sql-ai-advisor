package com.sqloptimizer.core.ai.model;

import java.util.Objects;

/**
 * AI Provider配置信息
 */
public class AiProviderConfig {

    /**
     * 提供商名称（唯一标识）
     */
    private String name;

    /**
     * 提供商类型
     */
    private AiProviderType type;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model;

    /**
     * API Base URL（可选，默认使用type提供的默认URL）
     */
    private String baseUrl;

    /**
     * 温度参数（创造性程度）
     */
    private Float temperature = 0.7f;

    /**
     * 最大Token数
     */
    private Integer maxTokens = 4096;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * API Key环境变量名称（优先使用）
     */
    private String apiKeyEnvVar;

    public AiProviderConfig() {
    }

    public AiProviderConfig(String name, AiProviderType type, String apiKey, String model) {
        this.name = name;
        this.type = type;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AiProviderType getType() {
        return type;
    }

    public void setType(AiProviderType type) {
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
        if (baseUrl == null || baseUrl.isEmpty()) {
            return type.getDefaultBaseUrl();
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiProviderConfig that = (AiProviderConfig) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "AiProviderConfig{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", model='" + model + '\'' +
                ", enabled=" + enabled +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AiProviderConfig config = new AiProviderConfig();

        public Builder name(String name) {
            config.setName(name);
            return this;
        }

        public Builder type(AiProviderType type) {
            config.setType(type);
            return this;
        }

        public Builder apiKey(String apiKey) {
            config.setApiKey(apiKey);
            return this;
        }

        public Builder model(String model) {
            config.setModel(model);
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            config.setBaseUrl(baseUrl);
            return this;
        }

        public Builder temperature(Float temperature) {
            config.setTemperature(temperature);
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            config.setMaxTokens(maxTokens);
            return this;
        }

        public Builder enabled(boolean enabled) {
            config.setEnabled(enabled);
            return this;
        }

        public Builder apiKeyEnvVar(String apiKeyEnvVar) {
            config.setApiKeyEnvVar(apiKeyEnvVar);
            return this;
        }

        public AiProviderConfig build() {
            return config;
        }
    }
}

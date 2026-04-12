package com.sqloptimizer.core.ai.provider;

import com.sqloptimizer.core.ai.model.AiProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * AI Provider抽象基类，提供通用功能
 */
public abstract class AbstractAiProvider implements AiProvider {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final AiProviderConfig config;

    protected AbstractAiProvider(AiProviderConfig config) {
        this.config = config;
    }

    @Override
    public AiProviderConfig getConfig() {
        return config;
    }

    @Override
    public String getProviderName() {
        return config.getName();
    }

    @Override
    public boolean isAvailable() {
        return config.isEnabled() && config.getApiKey() != null && !config.getApiKey().isEmpty();
    }

    @Override
    public int estimateTokens(String text) {
        // 简单估算：中文约2字符/token，英文约4字符/token
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        return (int) Math.ceil(chineseChars / 2.0) + (int) Math.ceil(otherChars / 4.0);
    }

    @Override
    public List<String> getSupportedModels() {
        // 子类可以覆盖此方法返回支持的模型列表
        return List.of(config.getModel());
    }

    /**
     * 获取API Key，优先从环境变量获取
     */
    protected String resolveApiKey() {
        if (config.getApiKeyEnvVar() != null && !config.getApiKeyEnvVar().isEmpty()) {
            String envValue = System.getenv(config.getApiKeyEnvVar());
            if (envValue != null && !envValue.isEmpty()) {
                return envValue;
            }
        }
        return config.getApiKey();
    }

    /**
     * 构建默认的系统提示
     */
    protected String getDefaultSystemPrompt() {
        return """
                你是一位资深的SQL优化专家，擅长分析SQL性能问题并提供优化建议。
                请根据提供的SQL语句和性能分析结果，给出具体的优化建议和改写后的SQL。
                优化建议应该包括：
                1. 问题原因分析
                2. 具体的优化步骤
                3. 优化后的SQL语句
                4. 预期的性能提升
                """;
    }

    /**
     * 验证配置是否有效
     */
    protected boolean validateConfig() {
        if (config.getType() == null) {
            log.error("Provider {}: type is required", config.getName());
            return false;
        }
        if (config.getModel() == null || config.getModel().isEmpty()) {
            log.error("Provider {}: model is required", config.getName());
            return false;
        }
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("Provider {}: apiKey is required", config.getName());
            return false;
        }
        return true;
    }
}

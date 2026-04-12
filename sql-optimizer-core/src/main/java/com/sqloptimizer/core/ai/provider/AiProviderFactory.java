package com.sqloptimizer.core.ai.provider;

import com.sqloptimizer.core.ai.model.AiProviderConfig;
import com.sqloptimizer.core.ai.model.AiProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI Provider工厂类
 * 根据配置类型创建对应的Provider实例
 */
public class AiProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AiProviderFactory.class);

    private AiProviderFactory() {
        // 工具类
    }

    /**
     * 根据配置创建Provider实例
     *
     * @param config Provider配置
     * @return Provider实例
     */
    public static AiProvider create(AiProviderConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Provider config cannot be null");
        }

        if (config.getName() == null || config.getName().isEmpty()) {
            throw new IllegalArgumentException("Provider name is required");
        }

        if (config.getType() == null) {
            throw new IllegalArgumentException("Provider type is required for: " + config.getName());
        }

        log.info("Creating AI provider: {} (type: {}, model: {})",
                config.getName(), config.getType(), config.getModel());

        AiProvider provider = switch (config.getType()) {
            case OPENAI, AZURE_OPENAI -> new OpenAiProvider(config);
            case CLAUDE -> new ClaudeProvider(config);
            case MINIMAX -> new MiniMaxProvider(config);
            case GEMINI -> createGeminiProvider(config);
            case DASHSCOPE -> createDashScopeProvider(config);
            case WENXIN -> createWenxinProvider(config);
            case OLLAMA -> createOllamaProvider(config);
            case CUSTOM -> createCustomProvider(config);
        };

        log.info("AI provider created: {}", provider.getProviderName());
        return provider;
    }

    /**
     * 根据类型名称创建Provider
     *
     * @param name    Provider名称
     * @param type    类型名称
     * @param apiKey  API Key
     * @param model   模型名称
     * @return Provider实例
     */
    public static AiProvider create(String name, String type, String apiKey, String model) {
        AiProviderType providerType;
        try {
            providerType = AiProviderType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown provider type: " + type);
        }

        AiProviderConfig config = AiProviderConfig.builder()
                .name(name)
                .type(providerType)
                .apiKey(apiKey)
                .model(model)
                .build();

        return create(config);
    }

    /**
     * 创建Gemini Provider
     */
    private static AiProvider createGeminiProvider(AiProviderConfig config) {
        // Gemini需要特殊的处理，使用LangChain4j的Google AI或者自定义实现
        // 这里先做一个占位实现
        throw new UnsupportedOperationException(
                "Gemini provider not yet implemented. Please use OPENAI, CLAUDE, or OLLAMA.");
    }

    /**
     * 创建DashScope Provider (阿里通义千问)
     */
    private static AiProvider createDashScopeProvider(AiProviderConfig config) {
        // 通义千问需要使用阿里云的SDK
        throw new UnsupportedOperationException(
                "DashScope provider not yet implemented. Please use OPENAI, CLAUDE, or OLLAMA.");
    }

    /**
     * 创建文心一言 Provider
     */
    private static AiProvider createWenxinProvider(AiProviderConfig config) {
        throw new UnsupportedOperationException(
                "Wenxin provider not yet implemented. Please use OPENAI, CLAUDE, or OLLAMA.");
    }

    /**
     * 创建Ollama Provider (本地模型)
     */
    private static AiProvider createOllamaProvider(AiProviderConfig config) {
        // Ollama支持OpenAI兼容的API，可以复用OpenAiProvider
        if (config.getBaseUrl() == null || config.getBaseUrl().isEmpty()) {
            config.setBaseUrl(AiProviderType.OLLAMA.getDefaultBaseUrl());
        }
        return new OpenAiProvider(config);
    }

    /**
     * 创建自定义Provider
     */
    private static AiProvider createCustomProvider(AiProviderConfig config) {
        // 自定义Provider需要baseUrl
        if (config.getBaseUrl() == null || config.getBaseUrl().isEmpty()) {
            throw new IllegalArgumentException(
                    "Custom provider requires baseUrl to be specified");
        }
        return new OpenAiProvider(config);
    }

    /**
     * 创建默认Provider (使用环境变量或默认配置)
     */
    public static AiProvider createDefaultProvider() {
        // 尝试从环境变量获取配置
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            return create(AiProviderConfig.builder()
                    .name("openai-default")
                    .type(AiProviderType.OPENAI)
                    .apiKey(apiKey)
                    .model("gpt-4o-mini")
                    .build());
        }

        // 尝试Claude
        String claudeKey = System.getenv("ANTHROPIC_API_KEY");
        if (claudeKey != null && !claudeKey.isEmpty()) {
            return create(AiProviderConfig.builder()
                    .name("claude-default")
                    .type(AiProviderType.CLAUDE)
                    .apiKey(claudeKey)
                    .model("claude-3-5-sonnet-20241022")
                    .build());
        }

        throw new IllegalStateException(
                "No AI provider configured. Set OPENAI_API_KEY or ANTHROPIC_API_KEY environment variable, " +
                        "or configure a provider explicitly.");
    }
}

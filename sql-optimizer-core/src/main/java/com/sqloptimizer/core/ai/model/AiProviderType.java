package com.sqloptimizer.core.ai.model;

/**
 * AI模型提供商类型
 */
public enum AiProviderType {

    /**
     * OpenAI 系列 (GPT-4, GPT-3.5)
     */
    OPENAI("OpenAI", "https://api.openai.com/v1"),

    /**
     * Anthropic Claude 系列
     */
    CLAUDE("Claude", "https://api.anthropic.com/v1"),

    /**
     * Google Gemini
     */
    GEMINI("Gemini", "https://generativelanguage.googleapis.com/v1beta"),

    /**
     * 阿里通义千问
     */
    DASHSCOPE("DashScope", "https://dashscope.aliyuncs.com/compatible-mode/v1"),

    /**
     * 百度文心一言
     */
    WENXIN("Wenxin", "https://qianfan.baidubce.com/v2"),

    /**
     * 本地Ollama模型
     */
    OLLAMA("Ollama", "http://localhost:11434/v1"),

    /**
     * Azure OpenAI
     */
    AZURE_OPENAI("Azure OpenAI", ""),

    /**
     * 自定义API
     */
    CUSTOM("Custom", ""),

    /**
     * MiniMax
     */
    MINIMAX("MiniMax", "https://api.minimax.chat/v1");

    private final String displayName;
    private final String defaultBaseUrl;

    AiProviderType(String displayName, String defaultBaseUrl) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }
}

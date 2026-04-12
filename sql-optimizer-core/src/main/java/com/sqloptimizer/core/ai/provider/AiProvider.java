package com.sqloptimizer.core.ai.provider;

import com.sqloptimizer.core.ai.model.AiProviderConfig;

import java.util.List;

/**
 * AI Provider统一接口
 * 所有AI模型提供商都需要实现此接口
 */
public interface AiProvider {

    /**
     * 获取提供商配置
     */
    AiProviderConfig getConfig();

    /**
     * 获取提供商类型名称
     */
    String getProviderName();

    /**
     * 验证API Key是否有效
     */
    boolean validateApiKey();

    /**
     * 发送聊天请求
     *
     * @param messages 消息列表
     * @return AI响应内容
     */
    String chat(String... messages);

    /**
     * 发送聊天请求
     *
     * @param systemPrompt 系统提示
     * @param userMessage  用户消息
     * @return AI响应内容
     */
    String chat(String systemPrompt, String userMessage);

    /**
     * 估算token数量（近似值）
     *
     * @param text 文本
     * @return token数量
     */
    int estimateTokens(String text);

    /**
     * 检查是否支持流式输出
     */
    boolean supportsStreaming();

    /**
     * 是否可用
     */
    boolean isAvailable();

    /**
     * 获取支持的模型列表
     */
    List<String> getSupportedModels();
}

package com.sqloptimizer.core.ai.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sqloptimizer.core.ai.model.AiProviderConfig;
import com.sqloptimizer.core.ai.model.AiProviderType;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Claude系列模型Provider
 * Claude使用独立的API，不是OpenAI兼容的，这里通过HTTP调用实现
 */
public class ClaudeProvider extends AbstractAiProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeProvider.class);
    private static final Gson GSON = new Gson();

    private HttpClient httpClient;

    public ClaudeProvider(AiProviderConfig config) {
        super(config);
        if (config.getType() == null) {
            config.setType(AiProviderType.CLAUDE);
        }
    }

    @Override
    protected boolean validateConfig() {
        if (!super.validateConfig()) {
            return false;
        }
        if (config.getType() != AiProviderType.CLAUDE) {
            log.error("Provider {}: type must be CLAUDE", config.getName());
            return false;
        }
        return true;
    }

    @Override
    public boolean validateApiKey() {
        if (!validateConfig()) {
            return false;
        }
        try {
            chat("Hello", "Say 'OK' if you receive this message.");
            return true;
        } catch (Exception e) {
            log.warn("Provider {}: API key validation failed: {}", config.getName(), e.getMessage());
            return false;
        }
    }

    @Override
    public String chat(String... messages) {
        if (messages.length == 0) {
            return "";
        }
        if (messages.length == 1) {
            return chat(getDefaultSystemPrompt(), messages[0]);
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < messages.length - 1; i++) {
            if (i > 0) context.append("\n");
            context.append(messages[i]);
        }
        return chat(context.toString(), messages[messages.length - 1]);
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        ensureHttpClientInitialized();

        try {
            // Claude API 使用不同的端点和请求格式
            String url = buildApiUrl();

            // 构建请求体 (Claude API格式)
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getModel());
            requestBody.addProperty("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 4096);
            requestBody.addProperty("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7f);

            // 构建消息
            JsonArray messagesArray = new JsonArray();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "user");
                systemMsg.addProperty("content", systemPrompt + "\n\n" + userMessage);
            } else {
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", userMessage);
                messagesArray.add(userMsg);
            }

            requestBody.add("messages", messagesArray);

            // 发送请求
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(GSON.toJson(requestBody), ContentType.APPLICATION_JSON));

            // 设置请求头
            String apiKey = resolveApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                request.setHeader("x-api-key", apiKey);
                request.setHeader("anthropic-version", "2023-06-01");
            }
            request.setHeader("Content-Type", "application/json");

            return sendRequest(request);

        } catch (Exception e) {
            log.error("Provider {}: chat failed: {}", config.getName(), e.getMessage());
            throw new RuntimeException("Chat failed: " + e.getMessage(), e);
        }
    }

    private String sendRequest(HttpPost request) throws Exception {
        return httpClient.execute(request, response -> {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            if (statusCode != 200) {
                throw new RuntimeException("API call failed with status " + statusCode + ": " + responseBody);
            }

            JsonObject jsonResponse = GSON.fromJson(responseBody, JsonObject.class);

            // Claude API响应格式
            if (jsonResponse.has("content")) {
                JsonArray content = jsonResponse.getAsJsonArray("content");
                if (content != null && content.size() > 0) {
                    JsonObject firstContent = content.get(0).getAsJsonObject();
                    if (firstContent.has("text")) {
                        return firstContent.get("text").getAsString();
                    }
                }
            }

            return "";
        });
    }

    private String buildApiUrl() {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = AiProviderType.CLAUDE.getDefaultBaseUrl();
        }
        return baseUrl + "/messages";
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public List<String> getSupportedModels() {
        return List.of(
                "claude-3-5-sonnet-20241022",
                "claude-3-opus-latest",
                "claude-3-sonnet-latest",
                "claude-3-haiku-latest"
        );
    }

    private synchronized void ensureHttpClientInitialized() {
        if (httpClient == null) {
            httpClient = HttpClients.createDefault();
        }
    }

    public static AiProviderConfig createDefaultConfig(String apiKey) {
        return AiProviderConfig.builder()
                .name("claude-default")
                .type(AiProviderType.CLAUDE)
                .apiKey(apiKey)
                .model("claude-3-5-sonnet-20241022")
                .temperature(0.7f)
                .maxTokens(4096)
                .build();
    }
}

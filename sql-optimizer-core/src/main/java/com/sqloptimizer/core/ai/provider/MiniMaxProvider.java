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
 * MiniMax AI模型Provider
 * MiniMax API 与 OpenAI API 兼容
 */
public class MiniMaxProvider extends AbstractAiProvider {

    private static final Logger log = LoggerFactory.getLogger(MiniMaxProvider.class);
    private static final Gson GSON = new Gson();

    private HttpClient httpClient;

    public MiniMaxProvider(AiProviderConfig config) {
        super(config);
        if (config.getType() == null) {
            config.setType(AiProviderType.MINIMAX);
        }
    }

    @Override
    protected boolean validateConfig() {
        if (!super.validateConfig()) {
            return false;
        }
        if (config.getType() != AiProviderType.MINIMAX) {
            log.error("Provider {}: type must be MINIMAX", config.getName());
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
            String url = buildApiUrl();

            // 构建请求体 (MiniMax格式)
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getModel());
            requestBody.addProperty("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7f);
            requestBody.addProperty("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 4096);

            // 构建消息
            JsonArray messagesArray = new JsonArray();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", systemPrompt);
                messagesArray.add(systemMsg);
            }

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messagesArray.add(userMsg);

            requestBody.add("messages", messagesArray);

            // 发送请求
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(GSON.toJson(requestBody), ContentType.APPLICATION_JSON));

            // 设置请求头
            String apiKey = resolveApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                request.setHeader("Authorization", "Bearer " + apiKey);
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

            // MiniMax API响应格式可能是 choices 或 output 字段
            JsonArray choices = null;
            try {
                choices = jsonResponse.getAsJsonArray("choices");
            } catch (Exception e) {
                // ignore
            }

            if (choices != null && choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                if (choice.has("message")) {
                    JsonObject message = choice.getAsJsonObject().getAsJsonObject("message");
                    return message.get("content").getAsString();
                } else if (choice.has("text")) {
                    return choice.get("text").getAsString();
                }
            }

            // 尝试从 output 字段获取（部分MiniMax模型）
            if (jsonResponse.has("output")) {
                return jsonResponse.get("output").getAsString();
            }

            // 返回原始响应用于调试
            log.warn("Unexpected MiniMax response format: {}", responseBody);
            return responseBody;
        });
    }

    private String buildApiUrl() {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = AiProviderType.MINIMAX.getDefaultBaseUrl();
        }
        return baseUrl + "/text/chatcompletion_v2";
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public List<String> getSupportedModels() {
        return List.of(
                "MiniMax-Text-01",
                "abab6.5s-chat",
                "abab6.5g-chat"
        );
    }

    private synchronized void ensureHttpClientInitialized() {
        if (httpClient == null) {
            httpClient = HttpClients.createDefault();
        }
    }

    public static AiProviderConfig createDefaultConfig(String apiKey) {
        return AiProviderConfig.builder()
                .name("minimax-default")
                .type(AiProviderType.MINIMAX)
                .apiKey(apiKey)
                .model("abab6.5s-chat")  // 免费可用模型
                .temperature(0.7f)
                .maxTokens(4096)
                .build();
    }
}

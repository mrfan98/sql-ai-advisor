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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * OpenAI GPT系列模型Provider
 * 使用直接HTTP调用实现，支持OpenAI兼容API
 */
public class OpenAiProvider extends AbstractAiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);
    private static final Gson GSON = new Gson();

    private HttpClient httpClient;

    public OpenAiProvider(AiProviderConfig config) {
        super(config);
        if (config.getType() == null) {
            config.setType(AiProviderType.OPENAI);
        }
    }

    @Override
    protected boolean validateConfig() {
        if (!super.validateConfig()) {
            return false;
        }
        if (config.getType() != AiProviderType.OPENAI && config.getType() != AiProviderType.AZURE_OPENAI
                && config.getType() != AiProviderType.OLLAMA && config.getType() != AiProviderType.CUSTOM) {
            log.error("Provider {}: type must be OPENAI, AZURE_OPENAI, OLLAMA, or CUSTOM", config.getName());
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

            // 构建请求体
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
            if (apiKey != null && !apiKey.isEmpty() && config.getType() != AiProviderType.OLLAMA) {
                request.setHeader("Authorization", "Bearer " + apiKey);
            }
            request.setHeader("Content-Type", "application/json");

            // 执行请求
            httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode != 200) {
                    throw new RuntimeException("API call failed with status " + statusCode + ": " + responseBody);
                }

                // 解析响应
                JsonObject jsonResponse = GSON.fromJson(responseBody, JsonObject.class);
                JsonArray choices = jsonResponse.getAsJsonArray("choices");

                if (choices != null && choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject message = choice.getAsJsonObject().getAsJsonObject("message");
                    return message.get("content").getAsString();
                }

                return "";
            });

            // 由于HttpClient的execute是void返回，我们需要用另一种方式
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
            JsonArray choices = jsonResponse.getAsJsonArray("choices");

            if (choices != null && choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject().getAsJsonObject("message");
                return message.get("content").getAsString();
            }

            return "";
        });
    }

    private String buildApiUrl() {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = config.getType().getDefaultBaseUrl();
        }
        return baseUrl + "/chat/completions";
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public List<String> getSupportedModels() {
        return List.of(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4-turbo",
                "gpt-4",
                "gpt-3.5-turbo"
        );
    }

    private synchronized void ensureHttpClientInitialized() {
        if (httpClient == null) {
            httpClient = HttpClients.createDefault();
        }
    }

    public static AiProviderConfig createDefaultConfig() {
        return AiProviderConfig.builder()
                .name("openai-default")
                .type(AiProviderType.OPENAI)
                .model("gpt-4o-mini")
                .temperature(0.7f)
                .maxTokens(4096)
                .build();
    }
}

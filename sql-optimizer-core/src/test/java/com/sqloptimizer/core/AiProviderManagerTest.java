package com.sqloptimizer.core;

import com.sqloptimizer.core.ai.model.AiProviderConfig;
import com.sqloptimizer.core.ai.model.AiProviderType;
import com.sqloptimizer.core.ai.provider.AiProvider;
import com.sqloptimizer.core.ai.provider.AiProviderFactory;
import com.sqloptimizer.core.ai.provider.AiProviderManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI Provider多模型接入测试
 */
public class AiProviderManagerTest {

    @Test
    void testRegisterSingleProvider() {
        AiProviderManager manager = new AiProviderManager();

        AiProviderConfig config = AiProviderConfig.builder()
                .name("test-openai")
                .type(AiProviderType.OPENAI)
                .apiKey("test-key")
                .model("gpt-4o-mini")
                .build();

        manager.registerProvider(config);

        assertEquals(1, manager.getProviderCount());
        assertTrue(manager.getProviderNames().contains("test-openai"));
    }

    @Test
    void testRegisterMultipleProviders() {
        AiProviderManager manager = new AiProviderManager();

        // 注册OpenAI Provider
        AiProviderConfig openAiConfig = AiProviderConfig.builder()
                .name("openai-gpt4")
                .type(AiProviderType.OPENAI)
                .apiKey("sk-openai")
                .model("gpt-4o")
                .build();
        manager.registerProvider(openAiConfig);

        // 注册Claude Provider
        AiProviderConfig claudeConfig = AiProviderConfig.builder()
                .name("claude-sonnet")
                .type(AiProviderType.CLAUDE)
                .apiKey("sk-ant")
                .model("claude-3-5-sonnet")
                .build();
        manager.registerProvider(claudeConfig);

        // 注册Ollama Provider (disabled)
        AiProviderConfig ollamaConfig = AiProviderConfig.builder()
                .name("ollama-local")
                .type(AiProviderType.OLLAMA)
                .apiKey("not-required")
                .model("llama3.2")
                .baseUrl("http://localhost:11434/v1")
                .enabled(false)  // 禁用的Provider不会成为默认
                .build();
        manager.registerProvider(ollamaConfig);

        assertEquals(3, manager.getProviderCount());
        assertEquals(3, manager.getProviderNames().size());

        // 验证默认Provider是第一个注册的enabled Provider
        // 由于claude-sonnet后注册且enabled，它覆盖了默认Provider
        assertEquals("claude-sonnet", manager.getDefaultProvider().get().getProviderName());
    }

    @Test
    void testSwitchProvider() {
        AiProviderManager manager = new AiProviderManager();

        AiProviderConfig openAiConfig = AiProviderConfig.builder()
                .name("openai")
                .type(AiProviderType.OPENAI)
                .apiKey("sk-openai")
                .model("gpt-4o-mini")
                .build();
        manager.registerProvider(openAiConfig);

        AiProviderConfig claudeConfig = AiProviderConfig.builder()
                .name("claude")
                .type(AiProviderType.CLAUDE)
                .apiKey("sk-ant")
                .model("claude-3-5-sonnet")
                .build();
        manager.registerProvider(claudeConfig);

        // 切换默认Provider
        manager.setDefaultProvider("claude");
        assertEquals("claude", manager.getDefaultProvider().get().getProviderName());

        // 切换回OpenAI
        manager.setDefaultProvider("openai");
        assertEquals("openai", manager.getDefaultProvider().get().getProviderName());
    }

    @Test
    void testRemoveProvider() {
        AiProviderManager manager = new AiProviderManager();

        AiProviderConfig config = AiProviderConfig.builder()
                .name("to-remove")
                .type(AiProviderType.OPENAI)
                .apiKey("sk-test")
                .model("gpt-4o-mini")
                .build();
        manager.registerProvider(config);

        assertEquals(1, manager.getProviderCount());

        manager.removeProvider("to-remove");
        assertEquals(0, manager.getProviderCount());
        assertTrue(manager.getDefaultProvider().isEmpty());
    }

    @Test
    void testAiProviderFactory() {
        // 测试OpenAI Provider创建
        AiProviderConfig openAiConfig = AiProviderConfig.builder()
                .name("factory-test")
                .type(AiProviderType.OPENAI)
                .apiKey("sk-test")
                .model("gpt-4o-mini")
                .build();

        AiProvider provider = AiProviderFactory.create(openAiConfig);
        assertNotNull(provider);
        assertEquals("factory-test", provider.getProviderName());
        assertTrue(provider.isAvailable()); // 因为有apiKey

        // 测试Ollama Provider创建
        AiProviderConfig ollamaConfig = AiProviderConfig.builder()
                .name("ollama-test")
                .type(AiProviderType.OLLAMA)
                .model("llama3.2")
                .baseUrl("http://localhost:11434/v1")
                .build();

        AiProvider ollamaProvider = AiProviderFactory.create(ollamaConfig);
        assertNotNull(ollamaProvider);
        assertEquals("ollama-test", ollamaProvider.getProviderName());
    }

    @Test
    void testUnsupportedProviderType() {
        AiProviderConfig config = AiProviderConfig.builder()
                .name("gemini-test")
                .type(AiProviderType.GEMINI)
                .apiKey("sk-gemini")
                .model("gemini-pro")
                .build();

        assertThrows(UnsupportedOperationException.class, () -> {
            AiProviderFactory.create(config);
        });
    }

    @Test
    void testProviderConfigValidation() {
        // 测试缺少type字段会抛出异常
        AiProviderConfig invalidConfig = AiProviderConfig.builder()
                .name("invalid")
                .apiKey("sk-test")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            AiProviderFactory.create(invalidConfig);
        });
    }

    @Test
    void testGetAllProviders() {
        AiProviderManager manager = new AiProviderManager();

        manager.registerProvider(AiProviderConfig.builder()
                .name("p1").type(AiProviderType.OPENAI).apiKey("key1").model("gpt-4").build());
        manager.registerProvider(AiProviderConfig.builder()
                .name("p2").type(AiProviderType.CLAUDE).apiKey("key2").model("claude-3").build());

        List<AiProvider> allProviders = manager.getAllProviders();
        assertEquals(2, allProviders.size());
    }

    @Test
    void testClearAllProviders() {
        AiProviderManager manager = new AiProviderManager();

        manager.registerProvider(AiProviderConfig.builder()
                .name("p1").type(AiProviderType.OPENAI).apiKey("key1").model("gpt-4").build());

        assertEquals(1, manager.getProviderCount());

        manager.clear();
        assertEquals(0, manager.getProviderCount());
        assertFalse(manager.hasAvailableProvider());
    }

    @Test
    void testMiniMaxProvider() {
        // 测试MiniMax Provider创建
        AiProviderConfig config = AiProviderConfig.builder()
                .name("minimax-test")
                .type(AiProviderType.MINIMAX)
                .apiKey("test-minimax-key")
                .model("MiniMax-Text-01")
                .build();

        AiProvider provider = AiProviderFactory.create(config);
        assertNotNull(provider);
        assertEquals("minimax-test", provider.getProviderName());
        assertTrue(provider.isAvailable());

        // 验证支持的模型
        List<String> models = provider.getSupportedModels();
        assertTrue(models.contains("MiniMax-Text-01"));
    }

    @Test
    void testRegisterMiniMaxProvider() {
        AiProviderManager manager = new AiProviderManager();

        AiProviderConfig minimaxConfig = AiProviderConfig.builder()
                .name("minimax")
                .type(AiProviderType.MINIMAX)
                .apiKey("sk-minimax")
                .model("MiniMax-Text-01")
                .build();
        manager.registerProvider(minimaxConfig);

        assertEquals(1, manager.getProviderCount());
        assertTrue(manager.getProviderNames().contains("minimax"));
        assertEquals("minimax", manager.getDefaultProvider().get().getProviderName());
    }
}

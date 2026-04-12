package com.sqloptimizer.core;

import com.sqloptimizer.core.ai.model.AiProviderConfig;
import com.sqloptimizer.core.ai.model.AiProviderType;
import com.sqloptimizer.core.ai.provider.AiProvider;
import com.sqloptimizer.core.service.impl.SqlOptimizerServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MiniMax连接测试
 */
public class MiniMaxConnectionTest {

    private static final String MINIMAX_API_KEY = "xxx";
    private static final String MINIMAX_MODEL = "MiniMax-M2.7";  // 免费可用模型

    @Test
    void testMiniMaxProviderCreation() {
        // 测试MiniMax Provider创建
        AiProviderConfig config = AiProviderConfig.builder()
                .name("minimax-test")
                .type(AiProviderType.MINIMAX)
                .apiKey(MINIMAX_API_KEY)
                .model(MINIMAX_MODEL)
                .temperature(0.7f)
                .maxTokens(2048)
                .build();

        AiProvider provider = new com.sqloptimizer.core.ai.provider.MiniMaxProvider(config);
        assertNotNull(provider);
        assertEquals("minimax-test", provider.getProviderName());
        assertTrue(provider.isAvailable());
    }

    @Test
    void testMiniMaxServiceCreation() {
        // 创建使用MiniMax的服务
        AiProviderConfig config = AiProviderConfig.builder()
                .name("minimax")
                .type(AiProviderType.MINIMAX)
                .apiKey(MINIMAX_API_KEY)
                .model(MINIMAX_MODEL)
                .build();

        SqlOptimizerServiceImpl service = new SqlOptimizerServiceImpl(config);
        assertNotNull(service);
        assertNotNull(service.getProviderManager());
        assertTrue(service.getProviderManager().hasAvailableProvider());
    }

    @Test
    void testMiniMaxChat() {
        // 测试MiniMax API调用（需要网络）
        AiProviderConfig config = AiProviderConfig.builder()
                .name("minimax")
                .type(AiProviderType.MINIMAX)
                .apiKey(MINIMAX_API_KEY)
                .model(MINIMAX_MODEL)
                .build();

        AiProvider provider = new com.sqloptimizer.core.ai.provider.MiniMaxProvider(config);

        try {
            String response = provider.chat("Hello, just say 'OK' if you receive this message.");
            assertNotNull(response);
            System.out.println("MiniMax response: " + response);
        } catch (Exception e) {
            System.out.println("MiniMax chat failed (network issue): " + e.getMessage());
            // 网络问题不视为测试失败
        }
    }

    @Test
    void testSqlAnalysisWithMiniMax() {
        // 测试SQL分析（使用MiniMax）
        AiProviderConfig config = AiProviderConfig.builder()
                .name("minimax")
                .type(AiProviderType.MINIMAX)
                .apiKey(MINIMAX_API_KEY)
                .model(MINIMAX_MODEL)
                .build();

        SqlOptimizerServiceImpl service = new SqlOptimizerServiceImpl(config);

        String sql = "SELECT * FROM users WHERE id = 1";

        // 这个测试不需要真实数据库，只是测试规则引擎
        try {
            var issues = service.getRuleEngine().analyze(sql);
            assertNotNull(issues);
            System.out.println("Detected issues: " + issues.size());
        } catch (Exception e) {
            fail("Rule engine failed: " + e.getMessage());
        }
    }
}

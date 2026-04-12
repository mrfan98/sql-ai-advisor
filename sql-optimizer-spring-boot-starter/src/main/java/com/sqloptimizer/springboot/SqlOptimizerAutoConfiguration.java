package com.sqloptimizer.springboot;

import com.sqloptimizer.core.ai.model.AiProviderConfig;
import com.sqloptimizer.core.ai.provider.AiProvider;
import com.sqloptimizer.core.ai.provider.AiProviderManager;
import com.sqloptimizer.core.service.SqlOptimizerService;
import com.sqloptimizer.core.service.impl.SqlOptimizerServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL Optimizer自动配置
 */
@Configuration
@EnableConfigurationProperties(SqlOptimizerProperties.class)
@ConditionalOnProperty(prefix = "sql.optimizer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SqlOptimizerAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SqlOptimizerAutoConfiguration.class);

    private final SqlOptimizerProperties properties;

    public SqlOptimizerAutoConfiguration(SqlOptimizerProperties properties) {
        this.properties = properties;
    }

    /**
     * AI Provider管理器
     */
    @Bean
    @ConditionalOnMissingBean(AiProviderManager.class)
    @ConditionalOnProperty(prefix = "sql.optimizer.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AiProviderManager aiProviderManager() {
        AiProviderManager manager = new AiProviderManager();
        SqlOptimizerProperties.Ai aiConfig = properties.getAi();

        // 收集所有Provider配置
        List<AiProviderConfig> configs = new ArrayList<>();

        // 旧版兼容：如果有apiKey配置，添加为默认Provider
        if (aiConfig.getApiKey() != null && !aiConfig.getApiKey().isEmpty()) {
            AiProviderConfig legacyConfig = AiProviderConfig.builder()
                    .name("openai-legacy")
                    .type(com.sqloptimizer.core.ai.model.AiProviderType.OPENAI)
                    .apiKey(aiConfig.getApiKey())
                    .model(aiConfig.getModel() != null ? aiConfig.getModel() : "gpt-4o-mini")
                    .baseUrl(aiConfig.getBaseUrl())
                    .build();
            configs.add(legacyConfig);
        }

        // 新版Provider列表配置
        if (aiConfig.getProviders() != null && !aiConfig.getProviders().isEmpty()) {
            for (SqlOptimizerProperties.Provider provider : aiConfig.getProviders()) {
                configs.add(provider.toProviderConfig());
            }
        }

        // 注册所有Provider
        for (AiProviderConfig config : configs) {
            try {
                manager.registerProvider(config);
                log.info("Registered AI provider: {} ({})", config.getName(), config.getType());
            } catch (Exception e) {
                log.warn("Failed to register AI provider {}: {}", config.getName(), e.getMessage());
            }
        }

        // 设置默认Provider
        if (aiConfig.getDefaultProvider() != null) {
            manager.setDefaultProvider(aiConfig.getDefaultProvider());
        }

        return manager;
    }

    /**
     * SQL优化服务
     */
    @Bean
    @ConditionalOnMissingBean(SqlOptimizerService.class)
    public SqlOptimizerService sqlOptimizerService(AiProviderManager providerManager) {
        SqlOptimizerServiceImpl service = new SqlOptimizerServiceImpl(providerManager);

        // 设置规则优先模式
        if (!properties.getAi().isEnableRuleFirst()) {
            log.info("Rule-first mode is disabled, AI will be called directly");
        }

        return service;
    }
}

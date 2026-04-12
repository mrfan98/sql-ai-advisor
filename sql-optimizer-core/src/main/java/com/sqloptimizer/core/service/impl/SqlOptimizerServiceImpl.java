package com.sqloptimizer.core.service.impl;

import com.sqloptimizer.core.ai.AiAdvisor;
import com.sqloptimizer.core.ai.model.AiProviderConfig;
import com.sqloptimizer.core.ai.provider.AiProvider;
import com.sqloptimizer.core.ai.provider.AiProviderManager;
import com.sqloptimizer.core.analyzer.PerformanceAnalyzer;
import com.sqloptimizer.core.cache.CaffeineCache;
import com.sqloptimizer.core.cache.SqlAnalysisCache;
import com.sqloptimizer.core.database.DatabaseAdapter;
import com.sqloptimizer.core.database.DatabaseAdapterManager;
import com.sqloptimizer.core.model.OptimizationAdvice;
import com.sqloptimizer.core.model.OptimizationIssue;
import com.sqloptimizer.core.model.OptimizationReport;
import com.sqloptimizer.core.parser.SqlParser;
import com.sqloptimizer.core.rule.SqlRuleEngine;
import com.sqloptimizer.core.service.SqlOptimizerService;

import javax.sql.DataSource;
import java.util.List;

/**
 * SQL优化服务实现
 */
public class SqlOptimizerServiceImpl implements SqlOptimizerService {

    private final SqlParser sqlParser;
    private final PerformanceAnalyzer performanceAnalyzer;
    private final AiAdvisor aiAdvisor;
    private final AiProviderManager providerManager;
    private final SqlAnalysisCache cache;
    private final SqlRuleEngine ruleEngine;
    private final DatabaseAdapterManager dbAdapterManager;

    /**
     * 默认构造函数
     */
    public SqlOptimizerServiceImpl() {
        this.sqlParser = new SqlParser();
        this.performanceAnalyzer = new PerformanceAnalyzer(sqlParser);
        this.aiAdvisor = new AiAdvisor();
        this.providerManager = new AiProviderManager();
        this.cache = new CaffeineCache();
        this.ruleEngine = aiAdvisor.getRuleEngine();
        this.dbAdapterManager = new DatabaseAdapterManager();
    }

    /**
     * 使用单个Provider构造
     */
    public SqlOptimizerServiceImpl(AiProviderConfig providerConfig) {
        this.sqlParser = new SqlParser();
        this.performanceAnalyzer = new PerformanceAnalyzer(sqlParser);
        this.providerManager = new AiProviderManager();

        if (providerConfig != null) {
            AiProvider provider = providerManager.registerProvider(providerConfig);
            this.aiAdvisor = new AiAdvisor(provider);
        } else {
            this.aiAdvisor = new AiAdvisor();
        }

        this.cache = new CaffeineCache();
        this.ruleEngine = aiAdvisor.getRuleEngine();
        this.dbAdapterManager = new DatabaseAdapterManager();
    }

    /**
     * 使用ProviderManager构造
     */
    public SqlOptimizerServiceImpl(AiProviderManager providerManager) {
        this.sqlParser = new SqlParser();
        this.performanceAnalyzer = new PerformanceAnalyzer(sqlParser);
        this.providerManager = providerManager;
        this.aiAdvisor = new AiAdvisor(providerManager);
        this.cache = new CaffeineCache();
        this.ruleEngine = aiAdvisor.getRuleEngine();
        this.dbAdapterManager = new DatabaseAdapterManager();
    }

    /**
     * 使用OpenAI API Key构造 (向后兼容)
     */
    public SqlOptimizerServiceImpl(String openAiApiKey) {
        this.sqlParser = new SqlParser();
        this.performanceAnalyzer = new PerformanceAnalyzer(sqlParser);
        this.providerManager = new AiProviderManager();

        if (openAiApiKey != null && !openAiApiKey.isEmpty()) {
            AiProviderConfig config = AiProviderConfig.builder()
                    .name("openai-legacy")
                    .type(com.sqloptimizer.core.ai.model.AiProviderType.OPENAI)
                    .apiKey(openAiApiKey)
                    .model("gpt-4o-mini")
                    .build();
            AiProvider provider = providerManager.registerProvider(config);
            this.aiAdvisor = new AiAdvisor(provider);
        } else {
            this.aiAdvisor = new AiAdvisor();
        }

        this.cache = new CaffeineCache();
        this.ruleEngine = aiAdvisor.getRuleEngine();
        this.dbAdapterManager = new DatabaseAdapterManager();
    }

    @Override
    public OptimizationReport analyze(String sql, DataSource dataSource) {
        long startTime = System.currentTimeMillis();

        // 获取数据库适配器
        DatabaseAdapter dbAdapter = dbAdapterManager.getAdapter(dataSource);
        String dbType = dbAdapter.getType().getCode();

        // 先检查缓存
        if (cache != null) {
            var cachedReport = cache.get(sql, dbType);
            if (cachedReport.isPresent()) {
                return cachedReport.get();
            }
        }

        // 检测性能问题
        List<OptimizationIssue> issues = detectIssues(sql, dataSource);

        // 使用规则引擎进行规则预检
        List<OptimizationIssue> ruleIssues = ruleEngine.analyze(sql);
        issues.addAll(ruleIssues);

        // 生成优化建议
        List<OptimizationAdvice> advice = aiAdvisor.generateAdvice(sql, issues);

        // 生成优化后的SQL
        String optimizedSql = aiAdvisor.generateOptimizedSql(sql, issues);

        long analysisTime = System.currentTimeMillis() - startTime;

        OptimizationReport report = new OptimizationReport(issues, advice, optimizedSql, analysisTime);

        // 缓存结果
        if (cache != null) {
            cache.put(sql, dbType, report);
        }

        return report;
    }

    @Override
    public List<OptimizationIssue> detectIssues(String sql, DataSource dataSource) {
        return performanceAnalyzer.analyze(sql, dataSource);
    }

    @Override
    public List<OptimizationAdvice> generateAdvice(List<OptimizationIssue> issues) {
        return aiAdvisor.generateAdvice("", issues);
    }

    /**
     * 生成优化建议
     */
    public List<OptimizationAdvice> generateAdvice(String sql, List<OptimizationIssue> issues) {
        return aiAdvisor.generateAdvice(sql, issues);
    }

    @Override
    public String generateOptimizedSql(String originalSql, List<OptimizationIssue> issues) {
        return aiAdvisor.generateOptimizedSql(originalSql, issues);
    }

    /**
     * 获取Provider管理器
     */
    public AiProviderManager getProviderManager() {
        return providerManager;
    }

    /**
     * 获取当前AI Advisor
     */
    public AiAdvisor getAiAdvisor() {
        return aiAdvisor;
    }

    /**
     * 获取规则引擎
     */
    public SqlRuleEngine getRuleEngine() {
        return ruleEngine;
    }

    /**
     * 获取缓存
     */
    public SqlAnalysisCache getCache() {
        return cache;
    }

    /**
     * 获取数据库适配器管理器
     */
    public DatabaseAdapterManager getDbAdapterManager() {
        return dbAdapterManager;
    }

    /**
     * 获取当前数据库适配器
     */
    public DatabaseAdapter getDatabaseAdapter(DataSource dataSource) {
        return dbAdapterManager.getAdapter(dataSource);
    }

    /**
     * 切换AI Provider
     */
    public void switchProvider(String providerName) {
        aiAdvisor.setCurrentProvider(providerName);
    }

    /**
     * 添加Provider
     */
    public void addProvider(AiProviderConfig config) {
        AiProvider provider = providerManager.registerProvider(config);
        if (providerManager.getProviderCount() == 1) {
            aiAdvisor.setCurrentProvider(config.getName());
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 关闭资源
     */
    public void close() {
        aiAdvisor.close();
        if (cache != null) {
            cache.clear();
        }
    }
}

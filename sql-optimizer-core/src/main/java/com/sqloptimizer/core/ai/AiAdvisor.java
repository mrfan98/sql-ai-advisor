package com.sqloptimizer.core.ai;

import com.sqloptimizer.core.model.OptimizationAdvice;
import com.sqloptimizer.core.model.OptimizationIssue;
import com.sqloptimizer.core.ai.provider.AiProvider;
import com.sqloptimizer.core.ai.provider.AiProviderManager;
import com.sqloptimizer.core.rule.SqlRule;
import com.sqloptimizer.core.rule.SqlRuleEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * AI优化顾问 - 支持多模型接入和规则引擎
 */
public class AiAdvisor {

    private AiProviderManager providerManager;
    private String currentProviderName;
    private SqlRuleEngine ruleEngine;
    private boolean ruleFirstEnabled = true;

    public AiAdvisor() {
        this.ruleEngine = new SqlRuleEngine(ruleFirstEnabled, false); // 默认不使用AI
    }

    public AiAdvisor(AiProviderManager providerManager) {
        this.providerManager = providerManager;
        this.ruleEngine = new SqlRuleEngine(ruleFirstEnabled, providerManager.hasAvailableProvider());
    }

    /**
     * 使用指定Provider初始化
     */
    public AiAdvisor(AiProvider provider) {
        this.providerManager = new AiProviderManager();
        this.providerManager.registerProvider(provider.getConfig());
        this.currentProviderName = provider.getConfig().getName();
        this.ruleEngine = new SqlRuleEngine(ruleFirstEnabled, true);
    }

    /**
     * 使用规则引擎构造
     */
    public AiAdvisor(SqlRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * 设置Provider管理器
     */
    public void setProviderManager(AiProviderManager providerManager) {
        this.providerManager = providerManager;
    }

    /**
     * 设置当前使用的Provider
     */
    public void setCurrentProvider(String providerName) {
        this.currentProviderName = providerName;
    }

    /**
     * 设置规则引擎
     */
    public void setRuleEngine(SqlRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * 获取规则引擎
     */
    public SqlRuleEngine getRuleEngine() {
        return ruleEngine;
    }

    /**
     * 设置规则优先模式
     */
    public void setRuleFirstEnabled(boolean enabled) {
        this.ruleFirstEnabled = enabled;
    }

    /**
     * 获取当前Provider
     */
    public AiProvider getCurrentProvider() {
        if (providerManager == null) {
            return null;
        }
        return providerManager.getProvider(currentProviderName)
                .orElseGet(() -> providerManager.getDefaultProvider().orElse(null));
    }

    /**
     * 检查AI能力是否可用
     */
    public boolean isAiEnabled() {
        return getCurrentProvider() != null && getCurrentProvider().isAvailable();
    }

    /**
     * 检查是否需要调用AI（基于规则引擎决策）
     */
    public boolean shouldCallAi(String sql) {
        return ruleEngine != null && ruleEngine.shouldCallAi(sql);
    }

    /**
     * 生成优化建议
     */
    public List<OptimizationAdvice> generateAdvice(String originalSql, List<OptimizationIssue> issues) {
        List<OptimizationAdvice> adviceList = new ArrayList<>();

        if (issues.isEmpty()) {
            return adviceList;
        }

        // 如果规则可以处理所有问题，不调用AI
        if (ruleFirstEnabled && !shouldCallAi(originalSql)) {
            return generateRuleBasedAdvice(originalSql, issues);
        }

        // 调用AI生成建议
        if (isAiEnabled()) {
            AiProvider provider = getCurrentProvider();
            String issuesDescription = generateIssuesDescription(issues);

            try {
                String aiAdvice = provider.chat(
                        buildSystemPrompt(),
                        buildUserPrompt(originalSql, issuesDescription)
                );

                OptimizationAdvice advice = parseAiAdvice(aiAdvice, originalSql);
                if (advice != null) {
                    adviceList.add(advice);
                }
            } catch (Exception e) {
                // AI调用失败，回退到规则
                return generateRuleBasedAdvice(originalSql, issues);
            }
        } else {
            // AI不可用，使用规则
            return generateRuleBasedAdvice(originalSql, issues);
        }

        return adviceList;
    }

    /**
     * 生成优化后的SQL
     */
    public String generateOptimizedSql(String originalSql, List<OptimizationIssue> issues) {
        if (issues.isEmpty()) {
            return originalSql;
        }

        // 如果规则可以处理，不调用AI
        if (ruleFirstEnabled && !shouldCallAi(originalSql)) {
            return generateRuleBasedOptimizedSql(originalSql, issues);
        }

        // 调用AI
        if (isAiEnabled()) {
            AiProvider provider = getCurrentProvider();
            String issuesDescription = generateIssuesDescription(issues);

            try {
                String prompt = buildRewritePrompt(originalSql, issuesDescription);
                return provider.chat(prompt).trim();
            } catch (Exception e) {
                // AI调用失败，回退到规则
                return generateRuleBasedOptimizedSql(originalSql, issues);
            }
        } else {
            return generateRuleBasedOptimizedSql(originalSql, issues);
        }
    }

    /**
     * 使用规则生成优化建议
     */
    private List<OptimizationAdvice> generateRuleBasedAdvice(String originalSql, List<OptimizationIssue> issues) {
        List<OptimizationAdvice> adviceList = new ArrayList<>();

        for (OptimizationIssue issue : issues) {
            OptimizationAdvice advice = generateAdviceForIssue(originalSql, issue);
            if (advice != null) {
                adviceList.add(advice);
            }
        }

        return adviceList;
    }

    /**
     * 为特定问题生成优化建议
     */
    private OptimizationAdvice generateAdviceForIssue(String originalSql, OptimizationIssue issue) {
        switch (issue.getType()) {
            case SELECT_ALL_COLUMNS:
                return generateSelectAllAdvice(originalSql);
            case FULL_TABLE_SCAN:
                return generateFullTableScanAdvice(originalSql);
            case MISSING_INDEX:
                return generateMissingIndexAdvice(originalSql);
            case IMPLICIT_CONVERSION:
                return generateImplicitConversionAdvice(originalSql);
            case TEMP_TABLE:
                return generateTempTableAdvice(originalSql);
            case SORT_OPERATION:
                return generateSortOperationAdvice(originalSql);
            default:
                return generateDefaultAdvice(originalSql, issue);
        }
    }

    private OptimizationAdvice generateSelectAllAdvice(String originalSql) {
        return new OptimizationAdvice(
                "避免使用SELECT *",
                "使用SELECT * 会查询所有列，增加网络传输和处理开销",
                "只选择需要的列，减少数据传输和处理开销",
                "-- 优化前\n" + originalSql + "\n\n-- 优化后\nSELECT column1, column2 FROM table WHERE condition"
        );
    }

    private OptimizationAdvice generateFullTableScanAdvice(String originalSql) {
        return new OptimizationAdvice(
                "避免全表扫描",
                "全表扫描会遍历表中的所有行，当表数据量大时性能会很差",
                "为查询条件中的列创建索引，使用索引加速查询",
                "-- 优化前\n" + originalSql + "\n\n-- 优化后（添加索引）\nCREATE INDEX idx_column ON table(column);"
        );
    }

    private OptimizationAdvice generateMissingIndexAdvice(String originalSql) {
        return new OptimizationAdvice(
                "添加缺失的索引",
                "查询条件中使用的列没有索引，导致查询性能较差",
                "为查询条件中的列创建合适的索引",
                "-- 优化前\n" + originalSql + "\n\n-- 优化后（添加索引）\nCREATE INDEX idx_column ON table(column);"
        );
    }

    private OptimizationAdvice generateImplicitConversionAdvice(String originalSql) {
        return new OptimizationAdvice(
                "避免隐式类型转换",
                "隐式类型转换会导致索引失效，影响查询性能",
                "确保查询条件中的数据类型与表结构一致",
                "-- 优化前\n" + originalSql + "\n\n-- 优化后（确保类型一致）\nSELECT * FROM table WHERE column = 123;"
        );
    }

    private OptimizationAdvice generateTempTableAdvice(String originalSql) {
        return new OptimizationAdvice(
                "优化临时表使用",
                "查询使用了临时表，可能影响性能",
                "优化查询逻辑，减少临时表的使用",
                "-- 优化前\n" + originalSql + "\n\n-- 优化后（简化查询）\nSELECT ... FROM table WHERE condition GROUP BY ..."
        );
    }

    private OptimizationAdvice generateSortOperationAdvice(String originalSql) {
        return new OptimizationAdvice(
                "优化排序操作",
                "查询中存在文件排序，可能影响性能",
                "为排序字段创建索引，使用索引排序",
                "-- 优化前\n" + originalSql + "\n\n-- 优化后（添加索引）\nCREATE INDEX idx_sort_column ON table(sort_column);"
        );
    }

    private OptimizationAdvice generateDefaultAdvice(String originalSql, OptimizationIssue issue) {
        return new OptimizationAdvice(
                issue.getType().name(),
                issue.getDescription(),
                "请根据具体问题进行优化",
                "-- 问题SQL\n" + originalSql
        );
    }

    /**
     * 使用规则生成优化后的SQL
     */
    private String generateRuleBasedOptimizedSql(String originalSql, List<OptimizationIssue> issues) {
        String optimizedSql = originalSql;

        if (issues.stream().anyMatch(issue -> issue.getType() == OptimizationIssue.IssueType.SELECT_ALL_COLUMNS)) {
            optimizedSql = optimizedSql.replace("SELECT *", "SELECT id, name");
        }

        // 应用规则引擎的自动修复
        optimizedSql = ruleEngine.autoFix(optimizedSql);

        return optimizedSql;
    }

    private String generateIssuesDescription(List<OptimizationIssue> issues) {
        StringBuilder description = new StringBuilder();
        for (OptimizationIssue issue : issues) {
            description.append("- ").append(issue.getDescription()).append("\n");
        }
        return description.toString();
    }

    private OptimizationAdvice parseAiAdvice(String aiAdvice, String originalSql) {
        return new OptimizationAdvice(
                "AI优化建议",
                "基于AI分析的SQL优化建议",
                aiAdvice,
                "-- 原始SQL\n" + originalSql + "\n\n-- AI优化建议\n" + aiAdvice
        );
    }

    private String buildSystemPrompt() {
        return """
                你是一位资深的SQL优化专家，擅长分析SQL性能问题并提供优化建议。
                请根据提供的SQL语句和性能分析结果，给出具体的优化建议和改写后的SQL。
                优化建议应该包括：
                1. 问题原因分析
                2. 具体的优化步骤
                3. 优化后的SQL语句
                4. 预期的性能提升
                """;
    }

    private String buildUserPrompt(String originalSql, String issuesDescription) {
        return String.format("""
                SQL语句：
                %s

                性能问题：
                %s

                请提供详细的优化建议。
                """, originalSql, issuesDescription);
    }

    private String buildRewritePrompt(String originalSql, String issuesDescription) {
        return String.format("""
                作为SQL优化专家，请根据以下性能问题，优化并改写以下SQL语句：
                只返回优化后的SQL语句，不要包含其他内容。

                原始SQL：
                %s

                性能问题：
                %s
                """, originalSql, issuesDescription);
    }

    public void close() {
        // 资源清理
        if (providerManager != null) {
            providerManager.clear();
        }
    }
}

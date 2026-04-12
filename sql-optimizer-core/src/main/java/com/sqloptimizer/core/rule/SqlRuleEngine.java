package com.sqloptimizer.core.rule;

import com.sqloptimizer.core.model.OptimizationIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SQL规则引擎
 * 管理规则执行，决定是否需要调用AI
 */
public class SqlRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(SqlRuleEngine.class);

    private final Map<String, SqlRule> rules = new ConcurrentHashMap<>();
    private final boolean ruleFirstEnabled;
    private final boolean aiEnabled;

    public SqlRuleEngine() {
        this(true, true);
    }

    public SqlRuleEngine(boolean ruleFirstEnabled, boolean aiEnabled) {
        this.ruleFirstEnabled = ruleFirstEnabled;
        this.aiEnabled = aiEnabled;
        registerDefaultRules();
    }

    /**
     * 注册默认规则
     */
    private void registerDefaultRules() {
        register(new SelectAllColumnsRule());
        register(new LimitMissingRule());
        register(new ImplicitConversionRule());
    }

    /**
     * 注册规则
     */
    public void register(SqlRule rule) {
        if (rule != null && rule.isEnabled()) {
            rules.put(rule.getName(), rule);
            log.debug("Registered rule: {} (priority: {})", rule.getName(), rule.getPriority());
        }
    }

    /**
     * 移除规则
     */
    public SqlRule remove(String ruleName) {
        return rules.remove(ruleName);
    }

    /**
     * 获取规则
     */
    public SqlRule get(String ruleName) {
        return rules.get(ruleName);
    }

    /**
     * 获取所有规则
     */
    public List<SqlRule> getAllRules() {
        return rules.values().stream()
                .sorted(Comparator.comparingInt(SqlRule::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * 分析SQL
     *
     * @param sql SQL语句
     * @return 问题列表
     */
    public List<OptimizationIssue> analyze(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<OptimizationIssue> allIssues = new ArrayList<>();

        for (SqlRule rule : getAllRules()) {
            try {
                if (rule.match(sql)) {
                    List<OptimizationIssue> issues = rule.analyze(sql);
                    if (issues != null && !issues.isEmpty()) {
                        allIssues.addAll(issues);
                        log.debug("Rule {} matched, found {} issues", rule.getName(), issues.size());
                    }
                }
            } catch (Exception e) {
                log.warn("Rule {} analysis failed: {}", rule.getName(), e.getMessage());
            }
        }

        return allIssues;
    }

    /**
     * 检查是否需要调用AI
     * 如果规则命中了问题，说明规则可以处理，不需要调用AI
     *
     * @param sql SQL语句
     * @return true表示需要调用AI，false表示规则可以处理
     */
    public boolean shouldCallAi(String sql) {
        // 如果禁用规则优先模式，总是调用AI
        if (!ruleFirstEnabled) {
            return aiEnabled;
        }

        // 如果AI被禁用，不调用AI
        if (!aiEnabled) {
            return false;
        }

        // 检查是否有规则命中
        List<OptimizationIssue> issues = analyze(sql);

        // 如果没有规则命中，需要AI深入分析
        if (issues.isEmpty()) {
            return true;
        }

        // 如果所有问题都可以自动修复，不需要调用AI
        boolean allAutoFixable = issues.stream()
                .allMatch(issue -> {
                    SqlRule rule = findRuleForIssue(issue);
                    return rule != null && rule.isAutoFixable();
                });

        return !allAutoFixable;
    }

    /**
     * 查找问题对应的规则
     */
    private SqlRule findRuleForIssue(OptimizationIssue issue) {
        String sql = issue.getLocation();
        if (sql == null || sql.isEmpty()) {
            return null;
        }

        for (SqlRule rule : rules.values()) {
            if (rule.match(sql)) {
                List<OptimizationIssue> ruleIssues = rule.analyze(sql);
                if (ruleIssues != null && ruleIssues.contains(issue)) {
                    return rule;
                }
            }
        }
        return null;
    }

    /**
     * 尝试自动修复SQL
     *
     * @param sql SQL语句
     * @return 修复后的SQL
     */
    public String autoFix(String sql) {
        String fixedSql = sql;

        for (SqlRule rule : getAllRules()) {
            try {
                if (rule.match(fixedSql) && rule.isAutoFixable()) {
                    fixedSql = rule.autoFix(fixedSql);
                    log.debug("Rule {} auto-fixed SQL", rule.getName());
                }
            } catch (Exception e) {
                log.warn("Rule {} auto-fix failed: {}", rule.getName(), e.getMessage());
            }
        }

        return fixedSql;
    }

    /**
     * 检查规则命中情况
     *
     * @param sql SQL语句
     * @return 命中的规则列表
     */
    public List<String> checkRules(String sql) {
        List<String> matchedRules = new ArrayList<>();

        for (SqlRule rule : getAllRules()) {
            try {
                if (rule.match(sql)) {
                    matchedRules.add(rule.getName());
                }
            } catch (Exception e) {
                log.warn("Rule {} match check failed: {}", rule.getName(), e.getMessage());
            }
        }

        return matchedRules;
    }

    /**
     * 获取规则数量
     */
    public int getRuleCount() {
        return rules.size();
    }

    /**
     * 清空所有规则
     */
    public void clear() {
        rules.clear();
    }

    /**
     * 是否启用规则优先
     */
    public boolean isRuleFirstEnabled() {
        return ruleFirstEnabled;
    }

    /**
     * 是否启用AI
     */
    public boolean isAiEnabled() {
        return aiEnabled;
    }

    @Override
    public String toString() {
        return "SqlRuleEngine{" +
                "ruleCount=" + rules.size() +
                ", ruleFirstEnabled=" + ruleFirstEnabled +
                ", aiEnabled=" + aiEnabled +
                '}';
    }
}

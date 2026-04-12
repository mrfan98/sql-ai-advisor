package com.sqloptimizer.core;

import com.sqloptimizer.core.model.OptimizationIssue;
import com.sqloptimizer.core.rule.SelectAllColumnsRule;
import com.sqloptimizer.core.rule.LimitMissingRule;
import com.sqloptimizer.core.rule.ImplicitConversionRule;
import com.sqloptimizer.core.rule.SqlRuleEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL规则引擎测试
 */
public class SqlRuleEngineTest {

    @Test
    void testSelectAllColumnsRule() {
        SqlRuleEngine engine = new SqlRuleEngine();
        engine.clear(); // 清空默认规则
        engine.register(new SelectAllColumnsRule());

        String sql = "SELECT * FROM orders WHERE customer_id = 123";
        List<OptimizationIssue> issues = engine.analyze(sql);

        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(i -> i.getType() == OptimizationIssue.IssueType.SELECT_ALL_COLUMNS));
    }

    @Test
    void testLimitMissingRule() {
        SqlRuleEngine engine = new SqlRuleEngine();
        engine.clear();
        engine.register(new LimitMissingRule());

        String sql = "SELECT id, name FROM users WHERE status = 1";
        List<OptimizationIssue> issues = engine.analyze(sql);

        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(i -> i.getDescription().contains("LIMIT")));
    }

    @Test
    void testImplicitConversionRule() {
        SqlRuleEngine engine = new SqlRuleEngine();
        engine.clear();
        engine.register(new ImplicitConversionRule());

        String sql = "SELECT * FROM users WHERE age = '25'";
        List<OptimizationIssue> issues = engine.analyze(sql);

        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(i -> i.getType() == OptimizationIssue.IssueType.IMPLICIT_CONVERSION));
    }

    @Test
    void testNoIssuesForNormalSql() {
        SqlRuleEngine engine = new SqlRuleEngine();
        engine.clear();
        engine.register(new SelectAllColumnsRule()); // 只注册需要的规则

        String sql = "SELECT id, name FROM users WHERE id = 1 LIMIT 10";
        List<OptimizationIssue> issues = engine.analyze(sql);

        assertTrue(issues.isEmpty());
    }

    @Test
    void testWriteOperationsNoLimitCheck() {
        SqlRuleEngine engine = new SqlRuleEngine();
        engine.clear();

        // INSERT语句不应该触发LIMIT缺失规则
        String insertSql = "INSERT INTO users (name, age) VALUES ('John', 25)";
        List<OptimizationIssue> issues = engine.analyze(insertSql);

        // 应该没有LIMIT相关问题
        boolean hasLimitIssue = issues.stream()
                .anyMatch(i -> i.getDescription().contains("LIMIT"));
        assertFalse(hasLimitIssue);
    }

    @Test
    void testAutoFix() {
        SqlRuleEngine engine = new SqlRuleEngine();
        engine.clear();
        engine.register(new LimitMissingRule()); // 只注册这个规则

        String sql = "SELECT id, name FROM users WHERE status = 1";
        String fixedSql = engine.autoFix(sql);

        assertTrue(fixedSql.contains("LIMIT"));
    }

    @Test
    void testShouldCallAi() {
        SqlRuleEngine engine = new SqlRuleEngine(true, true);
        engine.clear(); // 清空默认规则

        // 简单SQL，没有规则命中，应该调用AI
        String simpleSql = "SELECT id, name FROM users WHERE id = 1";
        assertTrue(engine.shouldCallAi(simpleSql));
    }

    @Test
    void testRulePriority() {
        SqlRuleEngine engine = new SqlRuleEngine();
        engine.clear();

        // 添加自定义规则
        engine.register(new SelectAllColumnsRule());
        engine.register(new LimitMissingRule());
        engine.register(new ImplicitConversionRule());

        assertEquals(3, engine.getRuleCount());

        List<String> rules = engine.checkRules("SELECT * FROM users WHERE age = '25'");
        assertTrue(rules.contains("SELECT_ALL_COLUMNS"));
        assertTrue(rules.contains("IMPLICIT_CONVERSION"));
    }

    @Test
    void testClearRules() {
        SqlRuleEngine engine = new SqlRuleEngine();
        engine.clear(); // 清空默认规则

        engine.register(new SelectAllColumnsRule());
        assertEquals(1, engine.getRuleCount());

        engine.clear();
        assertEquals(0, engine.getRuleCount());
    }

    @Test
    void testRuleFirstDisabled() {
        SqlRuleEngine engine = new SqlRuleEngine(false, false);
        engine.clear();

        // 规则优先禁用时，即使没有规则命中也不调用AI
        String sql = "SELECT id, name FROM users WHERE id = 1";
        assertFalse(engine.shouldCallAi(sql));
    }

    @Test
    void testAiDisabled() {
        SqlRuleEngine engine = new SqlRuleEngine(true, false);
        engine.clear();

        // AI禁用时，不调用AI
        String sql = "SELECT * FROM users";
        assertFalse(engine.shouldCallAi(sql));
    }
}

package com.sqloptimizer.core.rule;

import com.sqloptimizer.core.model.OptimizationIssue;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

import java.util.List;
import java.util.regex.Pattern;

/**
 * SELECT * 规则
 * 检测使用 SELECT * 的SQL，建议明确指定列名
 */
public class SelectAllColumnsRule implements SqlRule {

    private static final Pattern SELECT_ALL_PATTERN = Pattern.compile(
            "(?i)SELECT\\s+\\*\\s+FROM"
    );

    @Override
    public String getName() {
        return "SELECT_ALL_COLUMNS";
    }

    @Override
    public String getDescription() {
        return "检测使用 SELECT * 的SQL语句";
    }

    @Override
    public boolean match(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        return SELECT_ALL_PATTERN.matcher(sql).find();
    }

    @Override
    public List<OptimizationIssue> analyze(String sql) {
        if (!match(sql)) {
            return List.of();
        }

        OptimizationIssue issue = new OptimizationIssue(
                OptimizationIssue.IssueType.SELECT_ALL_COLUMNS,
                "使用 SELECT * 会查询所有列，增加网络传输和处理开销",
                "高",
                sql
        );

        return List.of(issue);
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级
    }

    @Override
    public boolean isAutoFixable() {
        return false; // 需要解析表结构才能确定列名
    }
}

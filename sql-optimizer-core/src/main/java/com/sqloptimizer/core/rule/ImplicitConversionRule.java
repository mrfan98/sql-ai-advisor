package com.sqloptimizer.core.rule;

import com.sqloptimizer.core.model.OptimizationIssue;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 隐式类型转换规则
 * 检测SQL中可能存在的隐式类型转换问题
 */
public class ImplicitConversionRule implements SqlRule {

    // 数字和字符串比较: WHERE num_column = '123' (字符串和数字比较)
    private static final Pattern NUM_STRING_COMPARE_PATTERN = Pattern.compile(
            "(?i)WHERE\\s+(\\w+)\\s*=\\s*'\\d+'",
            Pattern.CASE_INSENSITIVE
    );

    // 日期和数字比较
    private static final Pattern DATE_NUM_COMPARE_PATTERN = Pattern.compile(
            "(?i)WHERE\\s+(\\w+(?:_?date|_?time)?)\\s*=\\s*\\d+",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "IMPLICIT_CONVERSION";
    }

    @Override
    public String getDescription() {
        return "检测可能存在的隐式类型转换问题";
    }

    @Override
    public boolean match(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        return NUM_STRING_COMPARE_PATTERN.matcher(sql).find()
                || DATE_NUM_COMPARE_PATTERN.matcher(sql).find();
    }

    @Override
    public List<OptimizationIssue> analyze(String sql) {
        if (!match(sql)) {
            return List.of();
        }

        OptimizationIssue issue = new OptimizationIssue(
                OptimizationIssue.IssueType.IMPLICIT_CONVERSION,
                "SQL中可能存在隐式类型转换，可能导致索引失效和性能问题",
                "中",
                sql
        );

        return List.of(issue);
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public boolean isAutoFixable() {
        return false; // 需要知道列的实际类型才能正确修复
    }
}

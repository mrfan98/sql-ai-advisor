package com.sqloptimizer.core.rule;

import com.sqloptimizer.core.model.OptimizationIssue;

import java.util.List;

/**
 * SQL规则接口
 */
public interface SqlRule {

    /**
     * 获取规则名称
     */
    String getName();

    /**
     * 获取规则描述
     */
    String getDescription();

    /**
     * 检查SQL是否命中此规则
     *
     * @param sql SQL语句
     * @return 是否命中
     */
    boolean match(String sql);

    /**
     * 分析SQL并返回检测到的问题
     *
     * @param sql SQL语句
     * @return 问题列表（可能为空）
     */
    List<OptimizationIssue> analyze(String sql);

    /**
     * 规则是否启用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 规则优先级（数字越小优先级越高）
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否可以自动修复
     */
    default boolean isAutoFixable() {
        return false;
    }

    /**
     * 自动修复SQL
     *
     * @param sql SQL语句
     * @return 修复后的SQL（如果无法修复则返回原SQL）
     */
    default String autoFix(String sql) {
        return sql;
    }
}

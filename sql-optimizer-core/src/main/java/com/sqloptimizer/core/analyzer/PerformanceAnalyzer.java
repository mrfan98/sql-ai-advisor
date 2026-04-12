package com.sqloptimizer.core.analyzer;

import com.sqloptimizer.core.model.OptimizationIssue;
import com.sqloptimizer.core.parser.SqlParser;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.Statement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PerformanceAnalyzer {

    private final SqlParser sqlParser;

    public PerformanceAnalyzer(SqlParser sqlParser) {
        this.sqlParser = sqlParser;
    }

    /**
     * 分析SQL性能并检测问题
     *
     * @param sql        SQL语句
     * @param dataSource 数据源
     * @return 性能问题列表
     */
    public List<OptimizationIssue> analyze(String sql, DataSource dataSource) {
        List<OptimizationIssue> issues = new ArrayList<>();

        try {
            // 解析SQL
            net.sf.jsqlparser.statement.Statement statement = sqlParser.parse(sql);

            // 检查是否为SELECT *
            if (sqlParser.isSelectAll(statement)) {
                issues.add(new OptimizationIssue(
                        OptimizationIssue.IssueType.SELECT_ALL_COLUMNS,
                        "使用SELECT * 会查询所有列，增加网络传输和处理开销",
                        "中等",
                        sql
                ));
            }

            // 分析执行计划
            String executionPlan = getExecutionPlan(sql, dataSource);
            if (executionPlan != null) {
                // 检测全表扫描
                if (executionPlan.contains("ALL")) {
                    issues.add(new OptimizationIssue(
                            OptimizationIssue.IssueType.FULL_TABLE_SCAN,
                            "执行计划中存在全表扫描，可能导致性能问题",
                            "高",
                            sql
                    ));
                }

                // 检测临时表
                if (executionPlan.contains("TEMPORARY")) {
                    issues.add(new OptimizationIssue(
                            OptimizationIssue.IssueType.TEMP_TABLE,
                            "执行计划中使用了临时表，可能影响性能",
                            "中等",
                            sql
                    ));
                }

                // 检测排序操作
                if (executionPlan.contains("Using filesort")) {
                    issues.add(new OptimizationIssue(
                            OptimizationIssue.IssueType.SORT_OPERATION,
                            "执行计划中存在文件排序，可能影响性能",
                            "中等",
                            sql
                    ));
                }
            }

            // 检测隐式转换
            if (containsImplicitConversion(sql)) {
                issues.add(new OptimizationIssue(
                        OptimizationIssue.IssueType.IMPLICIT_CONVERSION,
                        "SQL中可能存在隐式类型转换，影响索引使用",
                        "中等",
                        sql
                ));
            }

        } catch (Exception e) {
            // 解析或分析失败，添加错误信息
            issues.add(new OptimizationIssue(
                    OptimizationIssue.IssueType.OTHER,
                    "SQL分析失败: " + e.getMessage(),
                    "低",
                    sql
            ));
        }

        return issues;
    }

    /**
     * 获取SQL执行计划
     *
     * @param sql        SQL语句
     * @param dataSource 数据源
     * @return 执行计划
     */
    private String getExecutionPlan(String sql, DataSource dataSource) {
        StringBuilder plan = new StringBuilder();

        try (Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {

            // 对于MySQL
            String explainSql = "EXPLAIN " + sql;
            ResultSet rs = stmt.executeQuery(explainSql);

            while (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    plan.append(rs.getMetaData().getColumnName(i)).append(": ").append(rs.getString(i)).append("\n");
                }
                plan.append("\n");
            }

        } catch (Exception e) {
            // 执行计划获取失败，返回null
        }

        return plan.length() > 0 ? plan.toString() : null;
    }

    /**
     * 检测SQL中是否存在隐式转换
     *
     * @param sql SQL语句
     * @return 是否存在隐式转换
     */
    private boolean containsImplicitConversion(String sql) {
        // 简化处理，实际需要更复杂的检测逻辑
        // 例如：字符串与数字比较、日期格式不匹配等
        return false;
    }
}

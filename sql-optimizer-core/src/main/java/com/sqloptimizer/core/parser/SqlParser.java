package com.sqloptimizer.core.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.List;

public class SqlParser {

    /**
     * 解析SQL语句为语法树
     *
     * @param sql SQL语句
     * @return 语法树
     * @throws JSQLParserException 解析异常
     */
    public Statement parse(String sql) throws JSQLParserException {
        return CCJSqlParserUtil.parse(sql);
    }

    /**
     * 提取SQL中涉及的表
     *
     * @param statement 语法树
     * @return 表名列表
     */
    public List<String> extractTables(Statement statement) {
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        return tablesNamesFinder.getTableList(statement);
    }

    /**
     * 检查是否为SELECT * 查询
     *
     * @param statement 语法树
     * @return 是否为SELECT *
     */
    public boolean isSelectAll(Statement statement) {
        if (statement instanceof Select) {
            Select select = (Select) statement;
            SelectBody selectBody = select.getSelectBody();
            if (selectBody != null) {
                // 简化处理，直接检查SQL字符串是否包含SELECT *
                return statement.toString().toUpperCase().contains("SELECT *");
            }
        }
        return false;
    }

    /**
     * 提取SQL中的WHERE条件
     *
     * @param statement 语法树
     * @return WHERE条件
     */
    public String extractWhereClause(Statement statement) {
        if (statement instanceof Select) {
            Select select = (Select) statement;
            SelectBody selectBody = select.getSelectBody();
            if (selectBody != null) {
                // 这里简化处理，实际需要根据具体的SelectBody类型提取WHERE条件
                return selectBody.toString();
            }
        }
        return null;
    }
}

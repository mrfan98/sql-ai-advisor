package com.sqloptimizer.core.database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 数据库适配器接口
 * 不同数据库有不同的SQL语法和特性，需要适配
 */
public interface DatabaseAdapter {

    /**
     * 获取数据库类型
     */
    DatabaseType getType();

    /**
     * 获取数据库类型名称
     */
    String getTypeName();

    /**
     * 构建EXPLAIN查询语句
     *
     * @param sql 要分析的SQL
     * @return EXPLAIN语句
     */
    String buildExplainSql(String sql);

    /**
     * 构建自动修复的SQL（根据规则引擎的建议）
     *
     * @param sql 原始SQL
     * @param ruleName 规则名称
     * @return 修复后的SQL
     */
    String buildAutoFixSql(String sql, String ruleName);

    /**
     * 获取表结构信息
     *
     * @param dataSource 数据源
     * @param tableName 表名
     * @return 表信息
     */
    TableInfo getTableInfo(DataSource dataSource, String tableName) throws SQLException;

    /**
     * 获取表的索引信息
     *
     * @param dataSource 数据源
     * @param tableName 表名
     * @return 索引列表
     */
    List<IndexInfo> getIndexes(DataSource dataSource, String tableName) throws SQLException;

    /**
     * 获取SQL关键字的转义符
     */
    default String getIdentifierQuote() {
        return "\"";
    }

    /**
     * 构建分页SQL
     *
     * @param sql 原始SQL
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 分页SQL
     */
    String buildPaginatedSql(String sql, long offset, long limit);

    /**
     * 是否支持该数据库类型
     */
    boolean supports(DatabaseType type);

    /**
     * 获取默认的规则建议
     */
    List<String> getDefaultRuleHints();

    /**
     * 表信息
     */
    class TableInfo {
        private final String tableName;
        private final String schema;
        private final String catalog;
        private final String tableType;
        private final List<ColumnInfo> columns;

        public TableInfo(String tableName, String schema, String catalog, String tableType, List<ColumnInfo> columns) {
            this.tableName = tableName;
            this.schema = schema;
            this.catalog = catalog;
            this.tableType = tableType;
            this.columns = columns;
        }

        public String getTableName() { return tableName; }
        public String getSchema() { return schema; }
        public String getCatalog() { return catalog; }
        public String getTableType() { return tableType; }
        public List<ColumnInfo> getColumns() { return columns; }
    }

    /**
     * 列信息
     */
    class ColumnInfo {
        private final String columnName;
        private final String dataType;
        private final int dataLength;
        private final boolean nullable;
        private final String defaultValue;
        private final boolean primaryKey;

        public ColumnInfo(String columnName, String dataType, int dataLength, boolean nullable, String defaultValue, boolean primaryKey) {
            this.columnName = columnName;
            this.dataType = dataType;
            this.dataLength = dataLength;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
            this.primaryKey = primaryKey;
        }

        public String getColumnName() { return columnName; }
        public String getDataType() { return dataType; }
        public int getDataLength() { return dataLength; }
        public boolean isNullable() { return nullable; }
        public String getDefaultValue() { return defaultValue; }
        public boolean isPrimaryKey() { return primaryKey; }
    }

    /**
     * 索引信息
     */
    class IndexInfo {
        private final String indexName;
        private final String columnName;
        private final boolean unique;
        private final boolean primary;

        public IndexInfo(String indexName, String columnName, boolean unique, boolean primary) {
            this.indexName = indexName;
            this.columnName = columnName;
            this.unique = unique;
            this.primary = primary;
        }

        public String getIndexName() { return indexName; }
        public String getColumnName() { return columnName; }
        public boolean isUnique() { return unique; }
        public boolean isPrimary() { return primary; }
    }
}

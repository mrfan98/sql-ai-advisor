package com.sqloptimizer.core.database;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL数据库适配器
 */
public class MySqlAdapter implements DatabaseAdapter {

    @Override
    public DatabaseType getType() {
        return DatabaseType.MYSQL;
    }

    @Override
    public String getTypeName() {
        return "MySQL";
    }

    @Override
    public String buildExplainSql(String sql) {
        return "EXPLAIN " + sql;
    }

    @Override
    public String buildAutoFixSql(String sql, String ruleName) {
        switch (ruleName) {
            case "LIMIT_MISSING":
                if (!sql.toUpperCase().contains("LIMIT")) {
                    return sql + " LIMIT 1000";
                }
                break;
            case "SELECT_ALL_COLUMNS":
                // 需要表结构信息才能确定列名，这里返回原SQL
                break;
        }
        return sql;
    }

    @Override
    public TableInfo getTableInfo(DataSource dataSource, String tableName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        String schema = null;
        String catalog = null;

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取表信息
            try (ResultSet rs = metaData.getTables(catalog, schema, tableName, new String[]{"TABLE"})) {
                if (!rs.next()) {
                    return null;
                }
            }

            // 获取列信息
            try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("TYPE_NAME");
                    int dataLength = rs.getInt("COLUMN_SIZE");
                    boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                    String defaultValue = rs.getString("COLUMN_DEF");

                    boolean isPrimaryKey = isPrimaryKey(metaData, catalog, schema, tableName, columnName);

                    columns.add(new ColumnInfo(columnName, dataType, dataLength, nullable, defaultValue, isPrimaryKey));
                }
            }
        }

        return new TableInfo(tableName, schema, catalog, "TABLE", columns);
    }

    private boolean isPrimaryKey(DatabaseMetaData metaData, String catalog, String schema, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (rs.next()) {
                if (columnName.equals(rs.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<IndexInfo> getIndexes(DataSource dataSource, String tableName) throws SQLException {
        List<IndexInfo> indexes = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet rs = metaData.getIndexInfo(catalog(null, conn), schema(null, conn), tableName, false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    boolean unique = !rs.getBoolean("NON_UNIQUE");
                    boolean primary = "PRIMARY".equalsIgnoreCase(indexName);

                    if (columnName != null) {
                        indexes.add(new IndexInfo(indexName, columnName, unique, primary));
                    }
                }
            }
        }

        return indexes;
    }

    private String catalog(String catalog, Connection conn) throws SQLException {
        return conn.getCatalog();
    }

    private String schema(String schema, Connection conn) throws SQLException {
        return conn.getSchema();
    }

    @Override
    public String getIdentifierQuote() {
        return "`";
    }

    @Override
    public String buildPaginatedSql(String sql, long offset, long limit) {
        return sql + " LIMIT " + offset + ", " + limit;
    }

    @Override
    public boolean supports(DatabaseType type) {
        return type == DatabaseType.MYSQL || type == DatabaseType.MYSQL_V8;
    }

    @Override
    public List<String> getDefaultRuleHints() {
        return List.of(
                "避免使用SELECT *",
                "确保WHERE条件有索引支持",
                "避免在WHERE中对字段进行函数运算",
                "使用EXPLAIN分析执行计划"
        );
    }
}

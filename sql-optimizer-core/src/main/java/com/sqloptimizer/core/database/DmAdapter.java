package com.sqloptimizer.core.database;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 达梦(DM)数据库适配器
 */
public class DmAdapter implements DatabaseAdapter {

    @Override
    public DatabaseType getType() {
        return DatabaseType.DM;
    }

    @Override
    public String getTypeName() {
        return "达梦数据库";
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
                break;
        }
        return sql;
    }

    @Override
    public TableInfo getTableInfo(DataSource dataSource, String tableName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        String schema = null;

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet rs = metaData.getTables(null, schema, tableName, new String[]{"TABLE"})) {
                if (!rs.next()) {
                    return null;
                }
            }

            try (ResultSet rs = metaData.getColumns(null, schema, tableName, null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("TYPE_NAME");
                    int dataLength = rs.getInt("COLUMN_SIZE");
                    boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                    String defaultValue = rs.getString("COLUMN_DEF");

                    boolean isPrimaryKey = isPrimaryKey(metaData, null, schema, tableName, columnName);

                    columns.add(new ColumnInfo(columnName, dataType, dataLength, nullable, defaultValue, isPrimaryKey));
                }
            }
        }

        return new TableInfo(tableName, schema, null, "TABLE", columns);
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

            try (ResultSet rs = metaData.getIndexInfo(null, schema(conn), tableName, false, false)) {
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

    private String schema(Connection conn) throws SQLException {
        return conn.getMetaData().getUserName();
    }

    @Override
    public String getIdentifierQuote() {
        return "\"";
    }

    @Override
    public String buildPaginatedSql(String sql, long offset, long limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public boolean supports(DatabaseType type) {
        return type == DatabaseType.DM;
    }

    @Override
    public List<String> getDefaultRuleHints() {
        return List.of(
                "使用EXPLAIN分析执行计划",
                "创建合适的索引",
                "避免使用SELECT *",
                "注意事务隔离级别"
        );
    }
}

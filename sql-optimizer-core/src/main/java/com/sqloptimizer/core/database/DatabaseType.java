package com.sqloptimizer.core.database;

/**
 * 数据库类型枚举
 */
public enum DatabaseType {

    MYSQL("MySQL", "mysql", "jdbc:mysql:", "SELECT"),
    POSTGRESQL("PostgreSQL", "postgresql", "jdbc:postgresql:", "SELECT"),
    ORACLE("Oracle", "oracle", "jdbc:oracle:", "SELECT"),
    SQLSERVER("SQL Server", "sqlserver", "jdbc:sqlserver:", "SELECT"),
    MYSQL_V8("MySQL 8.0", "mysql8", "jdbc:mysql:", "SELECT"),
    DM("达梦", "dameng", "jdbc:dm:", "SELECT"),
    KINGBASE("金仓", "kingbase", "jdbc:kingbase:", "SELECT"),
    OCEANBASE("OceanBase", "oceanbase", "jdbc:oceanbase:", "SELECT"),
    CLICKHOUSE("ClickHouse", "clickhouse", "jdbc:clickhouse:", "SELECT"),
    H2("H2", "h2", "jdbc:h2:", "SELECT"),
    UNKNOWN("Unknown", "unknown", "", "SELECT");

    private final String displayName;
    private final String code;
    private final String jdbcPrefix;
    private final String defaultSchema;

    DatabaseType(String displayName, String code, String jdbcPrefix, String defaultSchema) {
        this.displayName = displayName;
        this.code = code;
        this.jdbcPrefix = jdbcPrefix;
        this.defaultSchema = defaultSchema;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public String getJdbcPrefix() {
        return jdbcPrefix;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    /**
     * 根据JDBC URL检测数据库类型
     */
    public static DatabaseType detectFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return UNKNOWN;
        }

        String lowerUrl = url.toLowerCase();

        if (lowerUrl.contains("mysql")) {
            if (lowerUrl.contains("mysql8") || lowerUrl.contains(" mariadb")) {
                return MYSQL_V8;
            }
            return MYSQL;
        } else if (lowerUrl.contains("postgresql")) {
            return POSTGRESQL;
        } else if (lowerUrl.contains("oracle")) {
            return ORACLE;
        } else if (lowerUrl.contains("sqlserver") || lowerUrl.contains("microsoft")) {
            return SQLSERVER;
        } else if (lowerUrl.contains("dm") || lowerUrl.contains("dameng")) {
            return DM;
        } else if (lowerUrl.contains("kingbase")) {
            return KINGBASE;
        } else if (lowerUrl.contains("oceanbase")) {
            return OCEANBASE;
        } else if (lowerUrl.contains("clickhouse")) {
            return CLICKHOUSE;
        } else if (lowerUrl.contains("h2")) {
            return H2;
        }

        return UNKNOWN;
    }

    /**
     * 根据代码获取数据库类型
     */
    public static DatabaseType fromCode(String code) {
        for (DatabaseType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}

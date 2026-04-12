package com.sqloptimizer.core.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 数据库适配器管理器
 */
public class DatabaseAdapterManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAdapterManager.class);

    private final Map<DatabaseType, DatabaseAdapter> adapters = new HashMap<>();

    public DatabaseAdapterManager() {
        // 注册默认适配器
        register(new MySqlAdapter());
        register(new PostgreSqlAdapter());
        register(new DmAdapter());
    }

    /**
     * 注册适配器
     */
    public void register(DatabaseAdapter adapter) {
        adapters.put(adapter.getType(), adapter);
        log.debug("Registered database adapter: {}", adapter.getTypeName());
    }

    /**
     * 移除适配器
     */
    public DatabaseAdapter remove(DatabaseType type) {
        return adapters.remove(type);
    }

    /**
     * 获取适配器
     */
    public Optional<DatabaseAdapter> get(DatabaseType type) {
        return Optional.ofNullable(adapters.get(type));
    }

    /**
     * 根据数据源自动检测并获取适配器
     */
    public DatabaseAdapter getAdapter(DataSource dataSource) {
        if (dataSource == null) {
            return getDefaultAdapter();
        }

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String url = metaData.getURL();
            String productName = metaData.getDatabaseProductName();

            DatabaseType type = DatabaseType.detectFromUrl(url);
            if (type == DatabaseType.UNKNOWN) {
                // 尝试从产品名称检测
                type = detectFromProductName(productName);
            }

            DatabaseAdapter adapter = adapters.get(type);
            if (adapter != null) {
                log.debug("Detected database: {} ({}), using adapter: {}",
                        productName, url, adapter.getTypeName());
                return adapter;
            }

        } catch (SQLException e) {
            log.warn("Failed to detect database type: {}", e.getMessage());
        }

        return getDefaultAdapter();
    }

    /**
     * 根据产品名称检测数据库类型
     */
    private DatabaseType detectFromProductName(String productName) {
        if (productName == null) {
            return DatabaseType.UNKNOWN;
        }

        String lower = productName.toLowerCase();
        if (lower.contains("mysql")) {
            return DatabaseType.MYSQL;
        } else if (lower.contains("postgresql") || lower.contains("postgres")) {
            return DatabaseType.POSTGRESQL;
        } else if (lower.contains("oracle")) {
            return DatabaseType.ORACLE;
        } else if (lower.contains("sql server") || lower.contains("microsoft")) {
            return DatabaseType.SQLSERVER;
        } else if (lower.contains("dm") || lower.contains("dameng")) {
            return DatabaseType.DM;
        } else if (lower.contains("kingbase")) {
            return DatabaseType.KINGBASE;
        } else if (lower.contains("oceanbase")) {
            return DatabaseType.OCEANBASE;
        } else if (lower.contains("clickhouse")) {
            return DatabaseType.CLICKHOUSE;
        } else if (lower.contains("h2")) {
            return DatabaseType.H2;
        }

        return DatabaseType.UNKNOWN;
    }

    /**
     * 获取默认适配器（MySQL）
     */
    public DatabaseAdapter getDefaultAdapter() {
        DatabaseAdapter adapter = adapters.get(DatabaseType.MYSQL);
        if (adapter == null) {
            // 如果没有注册任何适配器，创建一个默认的
            return new MySqlAdapter();
        }
        return adapter;
    }

    /**
     * 获取所有已注册的适配器
     */
    public Map<DatabaseType, DatabaseAdapter> getAllAdapters() {
        return new HashMap<>(adapters);
    }

    /**
     * 获取支持的数据库类型列表
     */
    public String[] getSupportedDatabases() {
        return adapters.values().stream()
                .map(DatabaseAdapter::getTypeName)
                .toArray(String[]::new);
    }

    @Override
    public String toString() {
        return "DatabaseAdapterManager{" +
                "adapters=" + adapters.keySet() +
                '}';
    }
}

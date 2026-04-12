package com.sqloptimizer.core;

import com.sqloptimizer.core.ai.model.AiProviderConfig;
import com.sqloptimizer.core.ai.model.AiProviderType;
import com.sqloptimizer.core.database.DatabaseAdapter;
import com.sqloptimizer.core.database.DatabaseType;
import com.sqloptimizer.core.model.OptimizationReport;
import com.sqloptimizer.core.service.impl.SqlOptimizerServiceImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 达梦数据库完整连接测试
 */
public class DamengConnectionTest {

    // 达梦数据库配置
    private static final String DB_URL = "jdbc:dm://ip:5236";
    private static final String DB_USER = "ddd";
    private static final String DB_PASSWORD = "xxx";

    // MiniMax配置
    private static final String MINIMAX_API_KEY = "xxx";
    private static final String MINIMAX_MODEL = "MiniMax-M2.7";

    private static DataSource dataSource;
    private static SqlOptimizerServiceImpl service;

    // 简单的DataSource实现
    static class SimpleDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String password;

        SimpleDataSource(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override
        public int getLoginTimeout() { return 0; }

        @Override
        public void setLoginTimeout(int seconds) { }

        @Override
        public java.io.PrintWriter getLogWriter() { return null; }

        @Override
        public void setLogWriter(java.io.PrintWriter out) { }

        @Override
        public java.util.logging.Logger getParentLogger() { return null; }

        @Override
        public <T> T unwrap(Class<T> iface) { return null; }

        @Override
        public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    @BeforeAll
    static void setup() throws Exception {
        // 检查达梦驱动是否可用
        // 创建服务（使用MiniMax AI）- 服务创建不依赖数据库驱动
        AiProviderConfig config = AiProviderConfig.builder()
                .name("minimax")
                .type(AiProviderType.MINIMAX)
                .apiKey(MINIMAX_API_KEY)
                .model(MINIMAX_MODEL)
                .temperature(0.7f)
                .maxTokens(2048)
                .build();

        service = new SqlOptimizerServiceImpl(config);

        // 检查达梦驱动是否可用
        try {
            Class.forName("dm.jdbc.driver.DmDriver");
        } catch (ClassNotFoundException e) {
            System.out.println("⚠ 达梦数据库驱动未找到，跳过数据库连接测试");
            System.out.println("  请确保 Dameng JDBC driver (DmDriver) 在classpath中");
            dataSource = null;
            return;
        }

        // 创建数据源
        dataSource = new SimpleDataSource(DB_URL, DB_USER, DB_PASSWORD);
    }

    @Test
    void testDamengConnection() throws Exception {
        Assumptions.assumeTrue(dataSource != null, "达梦驱动不可用，跳过此测试");

        // 测试数据库连接
        try (Connection conn = dataSource.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            System.out.println("✓ 达梦数据库连接成功");

            // 获取数据库信息
            String url = conn.getMetaData().getURL();
            String productName = conn.getMetaData().getDatabaseProductName();
            System.out.println("  数据库: " + productName);
            System.out.println("  URL: " + url);
        }
    }

    @Test
    void testDatabaseAdapterDetection() throws Exception {
        Assumptions.assumeTrue(dataSource != null, "达梦驱动不可用，跳过此测试");

        // 测试数据库类型检测
        DatabaseAdapter adapter = service.getDatabaseAdapter(dataSource);
        assertNotNull(adapter);
        System.out.println("✓ 检测到数据库适配器: " + adapter.getTypeName());

        DatabaseType type = adapter.getType();
        assertEquals(DatabaseType.DM, type);
        System.out.println("  数据库类型: " + type.getDisplayName());
    }

    @Test
    void testDatabaseTables() throws Exception {
        Assumptions.assumeTrue(dataSource != null, "达梦驱动不可用，跳过此测试");

        // 获取数据库中的表
        try (Connection conn = dataSource.getConnection()) {
            var metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                System.out.println("✓ 数据库中的表:");
                int count = 0;
                while (rs.next() && count < 10) {
                    String tableName = rs.getString("TABLE_NAME");
                    System.out.println("  - " + tableName);
                    count++;
                }
                if (count == 10) {
                    System.out.println("  ... (更多表省略)");
                }
            }
        }
    }

    @Test
    void testRuleEngineAnalysis() throws Exception {
        // 规则引擎不需要数据库连接
        String sql = "SELECT * FROM users WHERE id = 1";
        var issues = service.getRuleEngine().analyze(sql);

        System.out.println("✓ 规则引擎分析完成");
        System.out.println("  SQL: " + sql);
        System.out.println("  检测到问题数: " + issues.size());
        for (var issue : issues) {
            System.out.println("  - [" + issue.getType() + "] " + issue.getDescription());
        }

        assertFalse(issues.isEmpty());
    }

    @Test
    void testFullSqlAnalysis() throws Exception {
        Assumptions.assumeTrue(dataSource != null, "达梦驱动不可用，跳过此测试");

        // 完整SQL分析测试
        String sql = "SELECT * FROM users WHERE status = 1";

        System.out.println("✓ 开始完整SQL分析");
        System.out.println("  SQL: " + sql);

        OptimizationReport report = service.analyze(sql, dataSource);

        System.out.println("  分析耗时: " + report.getAnalysisTime() + "ms");
        System.out.println("  检测到问题数: " + report.getIssues().size());
        System.out.println("  优化建议数: " + report.getAdvice().size());

        for (var issue : report.getIssues()) {
            System.out.println("  问题: [" + issue.getType() + "] " + issue.getDescription());
        }

        for (var advice : report.getAdvice()) {
            System.out.println("  建议: " + advice.getTitle());
        }

        if (report.getOptimizedSql() != null) {
            System.out.println("  优化SQL: " + report.getOptimizedSql());
        }
    }

    @Test
    void testAiMiniMaxChat() throws Exception {
        // MiniMax AI测试不需要数据库连接
        System.out.println("✓ 测试MiniMax AI对话");

        var provider = service.getProviderManager().getDefaultProvider();
        assertTrue(provider.isPresent());

        try {
            String response = provider.get().chat("请简单介绍一下SQL优化的基本方法，用中文回复");
            System.out.println("  MiniMax回复: " + response);
            assertNotNull(response);
            assertTrue(response.length() > 0);
        } catch (Exception e) {
            System.out.println("  MiniMax调用失败: " + e.getMessage());
        }
    }

    @Test
    void testAiOptimizationWithMiniMax() throws Exception {
        Assumptions.assumeTrue(dataSource != null, "达梦驱动不可用，跳过此测试");

        // 使用MiniMax AI进行SQL优化
        String sql = "SELECT u.id, u.name, o.order_no, o.total_amount " +
                     "FROM users u, orders o " +
                     "WHERE u.id = o.user_id AND u.status = 1";

        System.out.println("✓ 使用MiniMax AI优化SQL");
        System.out.println("  原始SQL: " + sql);

        OptimizationReport report = service.analyze(sql, dataSource);

        System.out.println("  分析耗时: " + report.getAnalysisTime() + "ms");
        System.out.println("  检测到问题数: " + report.getIssues().size());

        for (var advice : report.getAdvice()) {
            System.out.println("  优化建议: " + advice.getTitle());
            if (advice.getDescription() != null) {
                System.out.println("    " + advice.getDescription().substring(0, Math.min(100, advice.getDescription().length())) + "...");
            }
        }
    }
}

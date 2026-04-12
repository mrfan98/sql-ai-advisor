package com.sqloptimizer.mybatis;

import com.sqloptimizer.core.model.OptimizationReport;
import com.sqloptimizer.core.service.SqlOptimizerService;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * MyBatis SQL优化拦截器
 * 拦截SQL执行，记录慢查询，并可进行实时分析
 */
@Intercepts({
    @Signature(type = org.apache.ibatis.executor.Executor.class, method = "update",
            args = {MappedStatement.class, Object.class}),
    @Signature(type = org.apache.ibatis.executor.Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = org.apache.ibatis.executor.Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class SqlOptimizerInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(SqlOptimizerInterceptor.class);

    private final SqlOptimizerService sqlOptimizerService;
    private final SlowQueryRecorder slowQueryRecorder;
    private final MyBatisPluginProperties properties;

    public SqlOptimizerInterceptor(SqlOptimizerService sqlOptimizerService,
                                   SlowQueryRecorder slowQueryRecorder,
                                   MyBatisPluginProperties properties) {
        this.sqlOptimizerService = sqlOptimizerService;
        this.slowQueryRecorder = slowQueryRecorder;
        this.properties = properties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!properties.isInterceptEnabled()) {
            return invocation.proceed();
        }

        long startTime = System.nanoTime();
        Object result = null;
        Throwable exception = null;

        try {
            result = invocation.proceed();
            return result;
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            if (executionTime > properties.getSlowQueryThresholdMs()) {
                try {
                    SqlExecutionContext context = buildExecutionContext(invocation, executionTime);
                    recordSlowQuery(context);
                } catch (Exception e) {
                    log.error("Failed to record slow query", e);
                }
            }
        }
    }

    /**
     * 构建SQL执行上下文
     */
    private SqlExecutionContext buildExecutionContext(Invocation invocation, long executionTime) {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql boundSql = getBoundSql(invocation, ms, parameter);

        String sql = boundSql.getSql();
        String sqlHash = generateSqlHash(sql);
        String sqlType = getSqlType(ms.getSqlCommandType().name());

        SqlExecutionContext context = new SqlExecutionContext(sql, executionTime);
        context.setSqlHash(sqlHash);
        context.setSqlType(sqlType);
        context.setStatementId(ms.getId());
        context.setDatabaseType("DM");  // 默认达梦，可通过配置获取

        return context;
    }

    private BoundSql getBoundSql(Invocation invocation, MappedStatement ms, Object parameter) {
        try {
            // 尝试从参数中获取 BoundSql
            if (invocation.getArgs().length > 5) {
                return (BoundSql) invocation.getArgs()[5];
            }
            // 通过反射获取
            Configuration configuration = ms.getConfiguration();
            return configuration.newBoundSql(ms, parameter);
        } catch (Exception e) {
            log.warn("Failed to get BoundSql", e);
            return new BoundSql(ms.getConfiguration(), "UNKNOWN_SQL", ms.getParameterMappings(), parameter);
        }
    }

    /**
     * 生成SQL哈希用于去重
     */
    private String generateSqlHash(String sql) {
        // 简单哈希，实际生产环境应使用更安全的方式
        return String.valueOf(sql.hashCode());
    }

    /**
     * 获取SQL类型
     */
    private String getSqlType(String commandType) {
        return commandType != null ? commandType.toUpperCase() : "OTHER";
    }

    /**
     * 记录慢查询
     */
    private void recordSlowQuery(SqlExecutionContext context) {
        if (properties.isAsyncRecord()) {
            CompletableFuture.runAsync(() -> doRecordSlowQuery(context));
        } else {
            doRecordSlowQuery(context);
        }
    }

    private void doRecordSlowQuery(SqlExecutionContext context) {
        try {
            // 获取分析报告
            OptimizationReport report = null;
            try {
                // 注意：这里需要 DataSource，实际使用时通过注入获取
                // report = sqlOptimizerService.analyze(context.getSql(), dataSource);
            } catch (Exception e) {
                log.warn("Failed to analyze slow query: {}", e.getMessage());
            }

            // 记录慢查询
            slowQueryRecorder.record(context, report);
            log.info("Slow query recorded: {} ({}ms)", truncateSql(context.getSql()), context.getExecutionTimeMs());

        } catch (Exception e) {
            log.error("Failed to record slow query", e);
        }
    }

    /**
     * 截断SQL日志输出
     */
    private String truncateSql(String sql) {
        if (sql == null) return "null";
        return sql.length() > 100 ? sql.substring(0, 100) + "..." : sql;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(org.apache.ibatis.session.Configuration configuration) {
        // MyBatis 3.x 要求实现此方法但可以为空
    }

    @Override
    public void setProperties(java.util.Properties properties) {
        // Spring Boot 集成时会通过构造方法注入
    }
}

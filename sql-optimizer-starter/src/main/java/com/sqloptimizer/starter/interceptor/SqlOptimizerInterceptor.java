package com.sqloptimizer.starter.interceptor;

import com.sqloptimizer.starter.config.SqlOptimizerStarterProperties;
import com.sqloptimizer.starter.entity.SqlOptimizationRequest;
import com.sqloptimizer.starter.service.SqlOptimizationService;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * MyBatis SQL优化拦截器
 * 拦截SQL执行，分析性能问题，记录慢查询，并在审核通过后替换SQL
 */
@Intercepts({
    @Signature(type = Executor.class, method = "update",
            args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class SqlOptimizerInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(SqlOptimizerInterceptor.class);

    private final SqlOptimizationService optimizationService;
    private final SqlOptimizerStarterProperties properties;

    public SqlOptimizerInterceptor(SqlOptimizationService optimizationService,
                                  SqlOptimizerStarterProperties properties) {
        this.optimizationService = optimizationService;
        this.properties = properties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!properties.getMybatis().isEnabled()) {
            return invocation.proceed();
        }

        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql boundSql = getBoundSql(invocation, ms, parameter);

        String originalSql = boundSql.getSql();
        String mapperId = ms.getId();

        // 检查是否应该跳过
        if (shouldSkip(originalSql)) {
            return invocation.proceed();
        }

        long startTime = System.nanoTime();
        Object result = null;
        Throwable exception = null;

        try {
            // 尝试替换SQL（如果已审核通过）
            if (properties.getMybatis().getInterceptMode() == SqlOptimizerStarterProperties.InterceptMode.MANUAL_REVIEW) {
                String optimizedSql = optimizationService.getOptimizedSql(originalSql);
                if (optimizedSql != null && !optimizedSql.equals(originalSql)) {
                    // 替换SQL
                    BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), optimizedSql,
                            boundSql.getParameterMappings(), parameter);
                    // 复制附加参数
                    copyAdditionalParameters(boundSql, newBoundSql);

                    // 更新MappedStatement中的BoundSql
                    updateBoundSql(invocation, ms, newBoundSql);
                    log.info("SQL replaced for mapper: {}, original: {}, optimized: {}",
                            mapperId, truncateSql(originalSql), truncateSql(optimizedSql));
                }
            }

            result = invocation.proceed();
            return result;
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // 记录慢查询或提交分析
            if (executionTime > properties.getMybatis().getSlowQueryThresholdMs()) {
                logSlowQuery(mapperId, originalSql, executionTime);
            }

            // 异步提交AI分析（非阻塞）
            if (properties.getMybatis().isAsyncRecord()) {
                submitForAnalysis(mapperId, originalSql, executionTime);
            }
        }
    }

    /**
     * 提交SQL进行分析（异步）
     */
    private void submitForAnalysis(String mapperId, String sql, long executionTime) {
        try {
            optimizationService.submitForOptimization(sql, mapperId, null);
        } catch (Exception e) {
            log.warn("Failed to submit SQL for analysis: {}", e.getMessage());
        }
    }

    /**
     * 记录慢查询
     */
    private void logSlowQuery(String mapperId, String sql, long executionTime) {
        log.warn("Slow query detected: mapper={}, time={}ms, sql={}",
                mapperId, executionTime, truncateSql(sql));
    }

    /**
     * 检查是否应该跳过分析
     */
    private boolean shouldSkip(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return true;
        }
        String upperSql = sql.trim().toUpperCase();
        for (String prefix : properties.getMybatis().getExcludePrefixes()) {
            if (upperSql.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取BoundSql
     */
    private BoundSql getBoundSql(Invocation invocation, MappedStatement ms, Object parameter) {
        try {
            if (invocation.getArgs().length > 5) {
                return (BoundSql) invocation.getArgs()[5];
            }
            return ms.getBoundSql(parameter);
        } catch (Exception e) {
            return ms.getBoundSql(parameter);
        }
    }

    /**
     * 更新BoundSql（通过反射）
     */
    private void updateBoundSql(Invocation invocation, MappedStatement ms, BoundSql newBoundSql) {
        try {
            // 获取 BoundSqlParameterMapping
            Object parameterObject = invocation.getArgs()[1];

            // 创建一个新的 MappedStatement，使用新的 SqlSource
            SqlSource newSqlSource = new BoundSqlSqlSource(newBoundSql);

            // 通过反射更新（不推荐，但MyBatis没有公开API）
            Field field = MappedStatement.class.getDeclaredField("sqlSource");
            field.setAccessible(true);
            field.set(ms, newSqlSource);
        } catch (Exception e) {
            log.warn("Failed to update BoundSql via reflection: {}", e.getMessage());
        }
    }

    /**
     * 复制附加参数
     */
    private void copyAdditionalParameters(BoundSql source, BoundSql target) {
        try {
            java.util.Map<String, Object> additionalParameters = source.getAdditionalParameters();
            for (Map.Entry<String, Object> entry : additionalParameters.entrySet()) {
                target.setAdditionalParameter(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            log.warn("Failed to copy additional parameters: {}", e.getMessage());
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
    public void setProperties(Properties properties) {
        // 通过构造方法注入
    }

    /**
     * 内部类：用于包装BoundSql为SqlSource
     */
    private static class BoundSqlSqlSource implements SqlSource {
        private final BoundSql boundSql;

        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }
}

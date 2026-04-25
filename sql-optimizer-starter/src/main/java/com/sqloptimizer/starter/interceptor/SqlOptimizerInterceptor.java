package com.sqloptimizer.starter.interceptor;

import com.sqloptimizer.starter.config.SqlOptimizerStarterProperties;
import com.sqloptimizer.starter.entity.SqlOptimizationRequest;
import com.sqloptimizer.starter.service.SqlOptimizationService;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
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
import java.util.concurrent.CompletableFuture;
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

    /**
     * ThreadLocal存储当前线程的SQL替换信息
     * Key: 原始SQL hash, Value: 优化后的SQL
     * 避免修改共享的MappedStatement，确保线程安全
     */
    private static final ThreadLocal<String> replacementSqlHolder = new ThreadLocal<>();

    /**
     * SQL类型枚举
     */
    private enum SqlType {
        SELECT, INSERT, UPDATE, DELETE, OTHER
    }

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
            // 使用ThreadLocal传递替换信息，避免修改共享的MappedStatement
            String optimizedSql = optimizationService.getOptimizedSql(originalSql);
            if (optimizedSql != null && !optimizedSql.equals(originalSql)) {
                // 将替换SQL存入ThreadLocal
                replacementSqlHolder.set(optimizedSql);

                // 创建新的BoundSql，包含替换后的SQL
                BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), optimizedSql,
                        boundSql.getParameterMappings(), parameter);
                copyAdditionalParameters(boundSql, newBoundSql);

                // 创建新的MappedStatement（基于原ms，使用不同的SqlSource）
                MappedStatement newMs = createMappedStatementWithSql(ms, newBoundSql);

                // 更新invocation参数，使用新的MappedStatement和BoundSql
                Object[] args = invocation.getArgs();
                args[0] = newMs;  // MappedStatement
                if (args.length > 5) {
                    args[5] = newBoundSql;  // BoundSql (用于6参数query方法)
                }

                log.debug("SQL replacement prepared for mapper: {}, original: {}, optimized: {}",
                        mapperId, truncateSql(originalSql), truncateSql(optimizedSql));
            }

            result = invocation.proceed();
            return result;
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // 记录慢查询
            if (executionTime > properties.getMybatis().getSlowQueryThresholdMs()) {
                logSlowQuery(mapperId, originalSql, executionTime);
            }

            // 异步提交AI分析（非阻塞）
            if (properties.getMybatis().isAsyncRecord()) {
                submitForAnalysisAsync(mapperId, originalSql, executionTime);
            }

            // 清理ThreadLocal
            replacementSqlHolder.remove();
        }
    }

    /**
     * 创建包含指定SQL的新MappedStatement
     * 使用新的SqlSource包装BoundSql，避免修改原MappedStatement
     */
    private MappedStatement createMappedStatementWithSql(MappedStatement ms, BoundSql boundSql) {
        // 使用BoundSqlSqlSource包装新的BoundSql
        SqlSource sqlSource = new BoundSqlSqlSource(boundSql);

        // 创建新的MappedStatement，ID加上后缀以区分
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(),
                ms.getId() + "_optimized",
                sqlSource,
                ms.getSqlCommandType()
        );
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(String.join(",", ms.getKeyProperties()));
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        return builder.build();
    }

    /**
     * 异步提交SQL进行分析
     */
    private void submitForAnalysisAsync(String mapperId, String sql, long executionTime) {
        CompletableFuture.runAsync(() -> {
            try {
                optimizationService.submitForOptimization(sql, mapperId, executionTime);
            } catch (Exception e) {
                log.error("Failed to submit SQL for analysis: mapperId={}, sql={}, error={}",
                        mapperId, truncateSql(sql), e.getMessage());
            }
        });
    }

    /**
     * 记录慢查询
     */
    private void logSlowQuery(String mapperId, String sql, long executionTime) {
        log.warn("Slow query detected: mapper={}, time={}ms, sql={}",
                mapperId, executionTime, truncateSql(sql));
    }

    /**
     * 检查SQL是否应该跳过分析
     * 使用JSqlParser进行准确的SQL类型判断
     */
    private boolean shouldSkip(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return true;
        }

        // 优先使用JSqlParser解析判断SQL类型
        SqlType sqlType = detectSqlType(sql);
        if (sqlType == SqlType.OTHER) {
            // 无法解析，使用前缀匹配作为后备
            return matchesExcludePrefix(sql);
        }

        // 根据配置的排除规则决定是否跳过
        switch (sqlType) {
            case SELECT:
                // SELECT语句通常需要分析，不跳过
                // 但配置了跳过前缀的除外
                if (matchesExcludePrefix(sql)) {
                    return true;
                }
                // 检查是否是配置中明确排除的SELECT类型
                for (String prefix : properties.getMybatis().getExcludePrefixes()) {
                    if (prefix.trim().equalsIgnoreCase("SELECT")) {
                        // 不跳过所有SELECT
                        break;
                    }
                }
                return false;

            case INSERT:
            case UPDATE:
            case DELETE:
                // DML语句通常需要分析
                return false;

            default:
                return matchesExcludePrefix(sql);
        }
    }

    /**
     * 使用JSqlParser检测SQL类型
     */
    private SqlType detectSqlType(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                return SqlType.SELECT;
            } else if (statement instanceof Insert) {
                return SqlType.INSERT;
            } else if (statement instanceof Update) {
                return SqlType.UPDATE;
            } else if (statement instanceof Delete) {
                return SqlType.DELETE;
            } else {
                return SqlType.OTHER;
            }
        } catch (JSQLParserException e) {
            log.debug("Failed to parse SQL, using prefix matching: {}", e.getMessage());
            return SqlType.OTHER;
        }
    }

    /**
     * 使用前缀匹配判断是否应该跳过
     */
    private boolean matchesExcludePrefix(String sql) {
        String normalizedSql = sql.trim();
        for (String prefix : properties.getMybatis().getExcludePrefixes()) {
            String normalizedPrefix = prefix.trim();
            if (normalizedSql.toUpperCase().startsWith(normalizedPrefix.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取BoundSql
     */
    private BoundSql getBoundSql(Invocation invocation, MappedStatement ms, Object parameter) {
        Object[] args = invocation.getArgs();
        // 6参数query方法: MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql
        if (args.length > 5) {
            return (BoundSql) args[5];
        }
        // 4参数query方法: MappedStatement, Object, RowBounds, ResultHandler
        // update方法: MappedStatement, Object
        return ms.getBoundSql(parameter);
    }

    /**
     * 复制附加参数
     */
    private void copyAdditionalParameters(BoundSql source, BoundSql target) {
        Map<String, Object> additionalParameters = source.getAdditionalParameters();
        for (Map.Entry<String, Object> entry : additionalParameters.entrySet()) {
            target.setAdditionalParameter(entry.getKey(), entry.getValue());
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

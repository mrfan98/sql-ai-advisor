package com.sqloptimizer.mybatis;

import com.google.gson.Gson;
import com.sqloptimizer.core.model.OptimizationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 慢查询记录器
 * 负责将慢查询异步记录到 Redis 和数据库
 */
public class SlowQueryRecorder {

    private static final Logger log = LoggerFactory.getLogger(SlowQueryRecorder.class);
    private static final Gson GSON = new Gson();

    private final RedisTemplate<String, Object> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final MyBatisPluginProperties properties;

    public SlowQueryRecorder(RedisTemplate<String, Object> redisTemplate,
                            JdbcTemplate jdbcTemplate,
                            MyBatisPluginProperties properties) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /**
     * 记录慢查询
     */
    public void record(SqlExecutionContext context, OptimizationReport report) {
        String prefix = properties.getRedisKeyPrefix();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        // 1. 更新 Redis 实时统计
        updateRedisStats(context, today);

        // 2. 记录到数据库
        saveToDatabase(context, report);
    }

    /**
     * 更新 Redis 实时统计
     */
    private void updateRedisStats(SqlExecutionContext context, String date) {
        try {
            String prefix = properties.getRedisKeyPrefix();
            String statsKey = prefix + "stats:" + date;
            String sqlHashSetKey = prefix + "sql_hash:" + date;

            // 更新统计 Hash
            redisTemplate.opsForHash().increment(statsKey, "total_count", 1);
            redisTemplate.opsForHash().increment(statsKey, "total_time", context.getExecutionTimeMs());

            // 获取当前最大值并比较
            Object maxTime = redisTemplate.opsForHash().get(statsKey, "max_time");
            long currentMax = maxTime != null ? Long.parseLong(maxTime.toString()) : 0;
            if (context.getExecutionTimeMs() > currentMax) {
                redisTemplate.opsForHash().put(statsKey, "max_time", context.getExecutionTimeMs());
            }

            // 记录 SQL 哈希去重集合
            redisTemplate.opsForSet().add(sqlHashSetKey, context.getSqlHash());

            // 设置 TTL
            redisTemplate.expire(statsKey, properties.getStatsTtlDays(), TimeUnit.DAYS);
            redisTemplate.expire(sqlHashSetKey, properties.getStatsTtlDays(), TimeUnit.DAYS);

            // 更新按数据库类型的统计
            if (context.getDatabaseType() != null) {
                String dbTypeKey = properties.getRedisKeyPrefix() + "stats:" + date + ":" + context.getDatabaseType();
                redisTemplate.opsForHash().increment(dbTypeKey, "count", 1);
                redisTemplate.opsForHash().increment(dbTypeKey, "time", context.getExecutionTimeMs());
                redisTemplate.expire(dbTypeKey, properties.getStatsTtlDays(), TimeUnit.DAYS);
            }

            log.debug("Updated Redis stats for date: {}, sqlHash: {}", date, context.getSqlHash());

        } catch (Exception e) {
            log.error("Failed to update Redis stats", e);
        }
    }

    /**
     * 保存到数据库
     */
    private void saveToDatabase(SqlExecutionContext context, OptimizationReport report) {
        try {
            // 检查是否已存在相同 SQL
            String checkSql = "SELECT ID, FREQUENCY FROM SLOW_QUERY_RECORD WHERE SQL_HASH = ? AND TRUNC(EXECUTED_AT) = TRUNC(SYSDATE)";
            var existing = jdbcTemplate.queryForList(checkSql, context.getSqlHash());

            if (!existing.isEmpty()) {
                // 更新频率
                Long id = (Long) existing.get(0).get("ID");
                Integer frequency = (Integer) existing.get(0).get("FREQUENCY");
                String updateSql = "UPDATE SLOW_QUERY_RECORD SET FREQUENCY = ?, EXECUTION_TIME_MS = ?, UPDATED_AT = SYSDATE WHERE ID = ?";
                jdbcTemplate.update(updateSql, frequency + 1, context.getExecutionTimeMs(), id);
                log.debug("Updated slow query record: id={}, frequency={}", id, frequency + 1);
            } else {
                // 插入新记录
                String insertSql = """
                    INSERT INTO SLOW_QUERY_RECORD (
                        SQL_TEXT, SQL_HASH, EXECUTION_TIME_MS, DATABASE_TYPE, SQL_TYPE,
                        TABLE_NAME, FREQUENCY, ISSUES_JSON, EXECUTED_AT, CREATED_AT
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

                String issuesJson = report != null ? GSON.toJson(report.getIssues()) : null;

                jdbcTemplate.update(insertSql,
                        context.getSql(),
                        context.getSqlHash(),
                        context.getExecutionTimeMs(),
                        context.getDatabaseType(),
                        context.getSqlType(),
                        context.getTableNames() != null ? String.join(",", context.getTableNames()) : null,
                        1,
                        issuesJson,
                        new Timestamp(System.currentTimeMillis()),
                        new Timestamp(System.currentTimeMillis())
                );
                log.debug("Inserted new slow query record: sqlHash={}", context.getSqlHash());
            }
        } catch (Exception e) {
            log.error("Failed to save slow query to database", e);
        }
    }

    /**
     * 获取指定日期的统计信息
     */
    public Map<Object, Object> getStats(String date) {
        String statsKey = properties.getRedisKeyPrefix() + "stats:" + date;
        return redisTemplate.opsForHash().entries(statsKey);
    }

    /**
     * 获取今日慢查询数量
     */
    public Long getTodaySlowQueryCount() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String statsKey = properties.getRedisKeyPrefix() + "stats:" + today;
        Object count = redisTemplate.opsForHash().get(statsKey, "total_count");
        return count != null ? Long.parseLong(count.toString()) : 0L;
    }

    /**
     * 获取今日平均执行时间
     */
    public Double getTodayAvgExecutionTime() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String statsKey = properties.getRedisKeyPrefix() + "stats:" + today;
        Object totalTime = redisTemplate.opsForHash().get(statsKey, "total_time");
        Object count = redisTemplate.opsForHash().get(statsKey, "total_count");
        if (totalTime != null && count != null) {
            return Double.parseDouble(totalTime.toString()) / Long.parseLong(count.toString());
        }
        return 0.0;
    }
}

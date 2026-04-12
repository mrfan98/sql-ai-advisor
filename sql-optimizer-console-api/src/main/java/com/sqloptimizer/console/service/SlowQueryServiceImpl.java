package com.sqloptimizer.console.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sqloptimizer.console.entity.OptimizationRecord;
import com.sqloptimizer.console.entity.SlowQueryRecord;
import com.sqloptimizer.console.repository.OptimizationRecordRepository;
import com.sqloptimizer.console.repository.SlowQueryRecordRepository;
import com.sqloptimizer.console.workflow.ReviewStatus;
import com.sqloptimizer.core.model.OptimizationIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 慢查询服务实现
 */
@Service
public class SlowQueryServiceImpl implements SlowQueryService {

    private static final Logger log = LoggerFactory.getLogger(SlowQueryServiceImpl.class);
    private static final Gson GSON = new Gson();

    private final SlowQueryRecordRepository slowQueryRepository;
    private final OptimizationRecordRepository optimizationRepository;
    private final ReviewService reviewService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DataSource dataSource;
    private final String redisKeyPrefix = "sql_optimizer:console:";

    public SlowQueryServiceImpl(SlowQueryRecordRepository slowQueryRepository,
                               OptimizationRecordRepository optimizationRepository,
                               ReviewService reviewService,
                               RedisTemplate<String, Object> redisTemplate,
                               DataSource dataSource) {
        this.slowQueryRepository = slowQueryRepository;
        this.optimizationRepository = optimizationRepository;
        this.reviewService = reviewService;
        this.redisTemplate = redisTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public List<SlowQueryRecord> getSlowQueries(int page, int size) {
        return slowQueryRepository.findList(page, size);
    }

    @Override
    public List<SlowQueryRecord> getSlowQueriesByDateRange(String startDate, String endDate, int page, int size) {
        return slowQueryRepository.findByDateRange(startDate, endDate, page, size);
    }

    @Override
    public SlowQueryRecord getSlowQuery(Long id) {
        return slowQueryRepository.findById(id);
    }

    @Override
    public long getTodayCount() {
        // 优先从Redis获取
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String statsKey = redisKeyPrefix + "stats:" + today;
        Object count = redisTemplate.opsForHash().get(statsKey, "total_count");
        if (count != null) {
            return Long.parseLong(count.toString());
        }
        // 降级到数据库查询
        return slowQueryRepository.countToday();
    }

    @Override
    public Map<String, Object> getTrend(int days) {
        Map<String, Object> trend = new LinkedHashMap<>();
        List<Map<String, Object>> dailyStats = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(DateTimeFormatter.ISO_DATE);
            String statsKey = redisKeyPrefix + "stats:" + date;

            Map<Object, Object> stats = redisTemplate.opsForHash().entries(statsKey);
            Map<String, Object> dayStat = new LinkedHashMap<>();
            dayStat.put("date", date);

            if (!stats.isEmpty()) {
                dayStat.put("count", stats.get("total_count") != null ? Long.parseLong(stats.get("total_count").toString()) : 0);
                dayStat.put("avgTime", stats.get("total_time") != null && stats.get("total_count") != null ?
                        Double.parseDouble(stats.get("total_time").toString()) / Long.parseLong(stats.get("total_count").toString()) : 0);
                dayStat.put("maxTime", stats.get("max_time") != null ? Long.parseLong(stats.get("max_time").toString()) : 0);
            } else {
                dayStat.put("count", 0);
                dayStat.put("avgTime", 0.0);
                dayStat.put("maxTime", 0L);
            }

            dailyStats.add(dayStat);
        }

        trend.put("dailyStats", dailyStats);
        trend.put("totalDays", days);

        return trend;
    }

    @Override
    public Map<String, Long> getIssueDistribution(int days) {
        // 从数据库统计问题类型分布
        String sql = """
            SELECT ISSUES_JSON FROM SQL_OPTIMIZATION_RECORD
            WHERE CREATED_AT >= DATEADD(DAY, ?, CURRENT_DATE)
            AND ISSUES_JSON IS NOT NULL
            """;

        // 简化实现，实际应查询数据库
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("SELECT_ALL_COLUMNS", 0L);
        distribution.put("MISSING_INDEX", 0L);
        distribution.put("IMPLICIT_CONVERSION", 0L);
        distribution.put("FULL_TABLE_SCAN", 0L);
        distribution.put("OTHER", 0L);

        return distribution;
    }

    @Override
    public List<SlowQueryRecord> getTopSlowQueries(int limit) {
        return slowQueryRepository.findRecent(limit);
    }

    @Override
    public Long optimizeSlowQuery(Long id) {
        SlowQueryRecord slowQuery = slowQueryRepository.findById(id);
        if (slowQuery == null) {
            log.warn("Slow query not found: id={}", id);
            return null;
        }

        // 提交进行优化分析
        OptimizationRecord record = reviewService.submitForReview(slowQuery.getSqlText(), null);
        return record.getId();
    }
}

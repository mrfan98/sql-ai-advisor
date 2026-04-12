package com.sqloptimizer.console.service;

import com.sqloptimizer.console.entity.SlowQueryRecord;

import java.util.List;
import java.util.Map;

/**
 * 慢查询服务接口
 */
public interface SlowQueryService {

    /**
     * 获取慢查询列表
     */
    List<SlowQueryRecord> getSlowQueries(int page, int size);

    /**
     * 获取指定日期范围的慢查询
     */
    List<SlowQueryRecord> getSlowQueriesByDateRange(String startDate, String endDate, int page, int size);

    /**
     * 获取慢查询详情
     */
    SlowQueryRecord getSlowQuery(Long id);

    /**
     * 获取今日慢查询数量
     */
    long getTodayCount();

    /**
     * 获取慢查询趋势
     */
    Map<String, Object> getTrend(int days);

    /**
     * 获取问题类型分布
     */
    Map<String, Long> getIssueDistribution(int days);

    /**
     * 获取Top N慢查询
     */
    List<SlowQueryRecord> getTopSlowQueries(int limit);

    /**
     * 对慢查询进行优化分析
     */
    Long optimizeSlowQuery(Long id);
}

package com.sqloptimizer.console.controller;

import com.sqloptimizer.console.entity.SlowQueryRecord;
import com.sqloptimizer.console.service.SlowQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 趋势统计控制器
 */
@RestController
@RequestMapping("/api/v1/trends")
public class TrendController {

    private static final Logger log = LoggerFactory.getLogger(TrendController.class);

    private final SlowQueryService slowQueryService;

    public TrendController(SlowQueryService slowQueryService) {
        this.slowQueryService = slowQueryService;
    }

    /**
     * 获取慢查询趋势
     */
    @GetMapping("/slow-queries")
    public ResponseEntity<Map<String, Object>> getSlowQueryTrend(
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> trend = slowQueryService.getTrend(days);
        return ResponseEntity.ok(trend);
    }

    /**
     * 获取问题类型分布
     */
    @GetMapping("/issue-distribution")
    public ResponseEntity<Map<String, Long>> getIssueDistribution(
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Long> distribution = slowQueryService.getIssueDistribution(days);
        return ResponseEntity.ok(distribution);
    }

    /**
     * 获取Top N慢查询
     */
    @GetMapping("/top-slow-queries")
    public ResponseEntity<List<SlowQueryRecord>> getTopSlowQueries(
            @RequestParam(defaultValue = "10") int limit) {
        List<SlowQueryRecord> topQueries = slowQueryService.getTopSlowQueries(limit);
        return ResponseEntity.ok(topQueries);
    }

    /**
     * 获取概览统计
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("todaySlowQueryCount", slowQueryService.getTodayCount());
        overview.put("topSlowQueries", slowQueryService.getTopSlowQueries(5));
        return ResponseEntity.ok(overview);
    }
}

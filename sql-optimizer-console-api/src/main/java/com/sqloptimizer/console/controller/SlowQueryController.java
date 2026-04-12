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
 * 慢查询控制器
 */
@RestController
@RequestMapping("/api/v1/slow-queries")
public class SlowQueryController {

    private static final Logger log = LoggerFactory.getLogger(SlowQueryController.class);

    private final SlowQueryService slowQueryService;

    public SlowQueryController(SlowQueryService slowQueryService) {
        this.slowQueryService = slowQueryService;
    }

    /**
     * 获取慢查询列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSlowQueries(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<SlowQueryRecord> records;
        if (startDate != null && endDate != null) {
            records = slowQueryService.getSlowQueriesByDateRange(startDate, endDate, page, size);
        } else {
            records = slowQueryService.getSlowQueries(page, size);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("records", records);
        response.put("page", page);
        response.put("size", size);
        response.put("todayCount", slowQueryService.getTodayCount());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取慢查询详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<SlowQueryRecord> getSlowQuery(@PathVariable Long id) {
        SlowQueryRecord record = slowQueryService.getSlowQuery(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    /**
     * 对慢查询进行优化分析
     */
    @PostMapping("/{id}/optimize")
    public ResponseEntity<Map<String, Object>> optimizeSlowQuery(@PathVariable Long id) {
        Long recordId = slowQueryService.optimizeSlowQuery(id);

        Map<String, Object> response = new HashMap<>();
        if (recordId != null) {
            response.put("recordId", recordId);
            response.put("message", "已提交优化分析");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "慢查询记录不存在");
            return ResponseEntity.notFound().build();
        }
    }
}

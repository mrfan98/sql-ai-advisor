package com.sqloptimizer.starter.controller;

import com.sqloptimizer.starter.entity.SqlOptimizationRequest;
import com.sqloptimizer.starter.service.SqlOptimizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQL审核控制器 - 提供REST API进行人工审核
 */
@RestController
@RequestMapping("/api/v1/sql-review")
public class SqlReviewController {

    private static final Logger log = LoggerFactory.getLogger(SqlReviewController.class);

    private final SqlOptimizationService optimizationService;

    public SqlReviewController(SqlOptimizationService optimizationService) {
        this.optimizationService = optimizationService;
    }

    /**
     * 提交SQL进行优化分析
     * POST /api/v1/sql-review/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitForReview(
            @RequestParam String sql,
            @RequestParam(required = false) String mapperId,
            @RequestParam(required = false) String databaseType) {

        SqlOptimizationRequest request = optimizationService.submitForOptimization(sql, mapperId, null);

        Map<String, Object> response = new HashMap<>();
        response.put("requestId", request.getId());  // Note: ID is not set in current implementation
        response.put("status", request.getStatus().name());
        response.put("originalSql", request.getOriginalSql());
        response.put("optimizedSql", request.getOptimizedSql());
        response.put("issues", request.getIssues());
        response.put("advices", request.getAdvices());
        response.put("analysisTimeMs", request.getAnalysisTimeMs());
        response.put("submittedAt", request.getSubmittedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取待审核列表
     * GET /api/v1/sql-review/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<String> requestIds = optimizationService.getPendingRequestIds(page, size);
        List<SqlOptimizationRequest> requests = requestIds.stream()
                .map(optimizationService::getRequest)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("requests", requests);
        response.put("page", page);
        response.put("size", size);
        response.put("total", optimizationService.getPendingCount());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取请求详情
     * GET /api/v1/sql-review/{requestId}
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<SqlOptimizationRequest> getRequest(@PathVariable String requestId) {
        SqlOptimizationRequest request = optimizationService.getRequest(requestId);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(request);
    }

    /**
     * 审核决策
     * POST /api/v1/sql-review/{requestId}/review
     */
    @PostMapping("/{requestId}/review")
    public ResponseEntity<Map<String, Object>> makeReviewDecision(
            @PathVariable String requestId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reviewer,
            @RequestParam(required = false) String comment) {

        boolean success = optimizationService.review(requestId, approved, reviewer, comment);

        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);

        if (success) {
            response.put("status", approved ? "APPROVED" : "REJECTED");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "FAILED");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 执行已批准的优化SQL
     * POST /api/v1/sql-review/{requestId}/execute
     */
    @PostMapping("/{requestId}/execute")
    public ResponseEntity<Map<String, Object>> executeOptimization(@PathVariable String requestId) {
        // 注意：需要注入DataSource，这里简化处理
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("status", "NOT_IMPLEMENTED");
        response.put("message", "需要通过SqlOptimizationService.executeOptimization()方法执行");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取审核统计
     * GET /api/v1/sql-review/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCount", optimizationService.getPendingCount());
        return ResponseEntity.ok(stats);
    }
}

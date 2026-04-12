package com.sqloptimizer.console.controller;

import com.sqloptimizer.console.dto.ReviewDecisionRequest;
import com.sqloptimizer.console.dto.SubmitReviewRequest;
import com.sqloptimizer.console.entity.OptimizationRecord;
import com.sqloptimizer.console.service.ReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL优化审核控制器
 */
@RestController
@RequestMapping("/api/v1/optimization")
public class OptimizationController {

    private static final Logger log = LoggerFactory.getLogger(OptimizationController.class);

    private final ReviewService reviewService;

    public OptimizationController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 提交SQL进行AI优化分析
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitForReview(@Valid @RequestBody SubmitReviewRequest request) {
        OptimizationRecord record = reviewService.submitForReview(request.getSql(), request.getDataSourceId());

        Map<String, Object> response = new HashMap<>();
        response.put("recordId", record.getId());
        response.put("status", record.getStatus().name());
        response.put("originalSql", record.getOriginalSql());
        response.put("optimizedSql", record.getOptimizedSql());
        response.put("issues", record.getIssuesJson());
        response.put("advice", record.getAdviceJson());
        response.put("analysisTimeMs", record.getAnalysisTimeMs());
        response.put("submittedAt", record.getSubmittedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取优化记录详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<OptimizationRecord> getRecord(@PathVariable Long id) {
        OptimizationRecord record = reviewService.getRecord(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    /**
     * 获取待审核列表
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<OptimizationRecord> records = reviewService.getPendingRecords(page, size);
        long total = reviewService.getPendingCount();

        Map<String, Object> response = new HashMap<>();
        response.put("records", records);
        response.put("page", page);
        response.put("size", size);
        response.put("total", total);

        return ResponseEntity.ok(response);
    }

    /**
     * 审核决策
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<Void> makeReviewDecision(
            @PathVariable Long id,
            @Valid @RequestBody ReviewDecisionRequest request) {

        boolean success = reviewService.review(id, request.getDecision(), request.getComment(), request.getOperator());
        if (!success) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * 执行优化SQL
     */
    @PostMapping("/{id}/apply")
    public ResponseEntity<Map<String, Object>> applyOptimizedSql(@PathVariable Long id) {
        boolean success = reviewService.apply(id);

        Map<String, Object> response = new HashMap<>();
        response.put("recordId", id);

        if (success) {
            response.put("status", "APPLIED");
            response.put("message", "优化SQL已成功应用");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "FAILED");
            response.put("message", "执行优化SQL失败");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 删除记录
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.ok().build();
    }
}

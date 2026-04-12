package com.sqloptimizer.console.service;

import com.sqloptimizer.console.entity.OptimizationRecord;
import com.sqloptimizer.console.workflow.ReviewStatus;

import java.util.List;

/**
 * 审核服务接口
 */
public interface ReviewService {

    /**
     * 提交SQL进行AI优化分析
     */
    OptimizationRecord submitForReview(String sql, String dataSourceId);

    /**
     * 获取待审核列表
     */
    List<OptimizationRecord> getPendingRecords(int page, int size);

    /**
     * 获取待审核记录数
     */
    long getPendingCount();

    /**
     * 获取记录详情
     */
    OptimizationRecord getRecord(Long id);

    /**
     * 审核决策
     */
    boolean review(Long id, String decision, String comment, String operator);

    /**
     * 执行优化SQL
     */
    boolean apply(Long id);

    /**
     * 删除记录
     */
    void delete(Long id);
}

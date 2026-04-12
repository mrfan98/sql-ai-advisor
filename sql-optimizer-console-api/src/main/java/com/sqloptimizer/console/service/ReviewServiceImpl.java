package com.sqloptimizer.console.service;

import com.google.gson.Gson;
import com.sqloptimizer.console.entity.OptimizationRecord;
import com.sqloptimizer.console.repository.OptimizationRecordRepository;
import com.sqloptimizer.console.workflow.ReviewStatus;
import com.sqloptimizer.core.database.DatabaseAdapterManager;
import com.sqloptimizer.core.model.OptimizationReport;
import com.sqloptimizer.core.service.SqlOptimizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;

/**
 * 审核服务实现
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);
    private static final Gson GSON = new Gson();

    private final SqlOptimizerService sqlOptimizerService;
    private final OptimizationRecordRepository recordRepository;
    private final DatabaseAdapterManager adapterManager;
    private final DataSource dataSource;

    public ReviewServiceImpl(SqlOptimizerService sqlOptimizerService,
                           OptimizationRecordRepository recordRepository,
                           DatabaseAdapterManager adapterManager,
                           DataSource dataSource) {
        this.sqlOptimizerService = sqlOptimizerService;
        this.recordRepository = recordRepository;
        this.adapterManager = adapterManager;
        this.dataSource = dataSource;
    }

    @Override
    public OptimizationRecord submitForReview(String sql, String dataSourceId) {
        long startTime = System.currentTimeMillis();

        // 调用核心服务分析SQL
        OptimizationReport report = sqlOptimizerService.analyze(sql, dataSource);

        long analysisTime = System.currentTimeMillis() - startTime;

        // 创建记录
        OptimizationRecord record = new OptimizationRecord();
        record.setOriginalSql(sql);
        record.setOptimizedSql(report.getOptimizedSql());
        record.setDatabaseType(detectDatabaseType());
        record.setStatus(ReviewStatus.PENDING);
        record.setAnalysisTimeMs(analysisTime);
        record.setIssuesJson(GSON.toJson(report.getIssues()));
        record.setAdviceJson(GSON.toJson(report.getAdvice()));
        record.setSubmittedAt(new Timestamp(System.currentTimeMillis()));

        // 保存记录
        Long id = recordRepository.save(record);
        record.setId(id);

        log.info("Submitted SQL for review: id={}, analysisTime={}ms", id, analysisTime);
        return record;
    }

    @Override
    public List<OptimizationRecord> getPendingRecords(int page, int size) {
        return recordRepository.findPending(page, size);
    }

    @Override
    public long getPendingCount() {
        return recordRepository.countPending();
    }

    @Override
    public OptimizationRecord getRecord(Long id) {
        return recordRepository.findById(id);
    }

    @Override
    public boolean review(Long id, String decision, String comment, String operator) {
        OptimizationRecord record = recordRepository.findById(id);
        if (record == null) {
            log.warn("Record not found: id={}", id);
            return false;
        }

        if (!record.getStatus().canReview()) {
            log.warn("Record cannot be reviewed: id={}, status={}", id, record.getStatus());
            return false;
        }

        ReviewStatus newStatus;
        if ("APPROVE".equalsIgnoreCase(decision)) {
            newStatus = ReviewStatus.APPROVED;
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            newStatus = ReviewStatus.REJECTED;
        } else {
            log.warn("Invalid decision: {}", decision);
            return false;
        }

        recordRepository.updateStatus(id, newStatus, operator, comment);
        log.info("Review decision: id={}, decision={}, operator={}", id, decision, operator);
        return true;
    }

    @Override
    public boolean apply(Long id) {
        OptimizationRecord record = recordRepository.findById(id);
        if (record == null) {
            log.warn("Record not found: id={}", id);
            return false;
        }

        if (!record.getStatus().canApply()) {
            log.warn("Record cannot be applied: id={}, status={}", id, record.getStatus());
            return false;
        }

        String optimizedSql = record.getOptimizedSql();
        if (optimizedSql == null || optimizedSql.isEmpty()) {
            log.warn("No optimized SQL to apply: id={}", id);
            return false;
        }

        // 执行优化SQL
        try (Connection conn = dataSource.getConnection()) {
            // 检测是否为DDL语句
            String upperSql = optimizedSql.trim().toUpperCase();
            boolean isDdl = upperSql.startsWith("CREATE") || upperSql.startsWith("ALTER") ||
                           upperSql.startsWith("DROP") || upperSql.startsWith("TRUNCATE");

            if (isDdl) {
                // DDL语句使用 execute
                conn.createStatement().execute(optimizedSql);
            } else {
                // DML语句使用 executeUpdate
                conn.createStatement().executeUpdate(optimizedSql);
            }

            // 更新状态为已应用
            recordRepository.updateStatus(id, ReviewStatus.APPLIED, null, "Applied automatically");
            log.info("Applied optimized SQL: id={}", id);
            return true;

        } catch (Exception e) {
            log.error("Failed to apply optimized SQL: id={}", id, e);
            return false;
        }
    }

    @Override
    public void delete(Long id) {
        recordRepository.deleteById(id);
        log.info("Deleted record: id={}", id);
    }

    /**
     * 检测数据库类型
     */
    private String detectDatabaseType() {
        try (Connection conn = dataSource.getConnection()) {
            String productName = conn.getMetaData().getDatabaseProductName();
            if (productName != null && productName.toLowerCase().contains("dm")) {
                return "DM";
            } else if (productName != null && productName.toLowerCase().contains("mysql")) {
                return "MySQL";
            } else if (productName != null && productName.toLowerCase().contains("postgresql")) {
                return "PostgreSQL";
            }
            return productName;
        } catch (Exception e) {
            log.warn("Failed to detect database type", e);
            return "UNKNOWN";
        }
    }
}

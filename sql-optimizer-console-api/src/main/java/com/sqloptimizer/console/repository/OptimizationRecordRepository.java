package com.sqloptimizer.console.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sqloptimizer.console.entity.OptimizationRecord;
import com.sqloptimizer.console.workflow.ReviewStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * SQL优化记录仓库
 */
@Repository
public class OptimizationRecordRepository {

    private static final Logger log = LoggerFactory.getLogger(OptimizationRecordRepository.class);
    private static final Gson GSON = new Gson();

    private final JdbcTemplate jdbcTemplate;

    public OptimizationRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<OptimizationRecord> rowMapper = new RowMapper<OptimizationRecord>() {
        @Override
        public OptimizationRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            OptimizationRecord record = new OptimizationRecord();
            record.setId(rs.getLong("ID"));
            record.setOriginalSql(rs.getString("ORIGINAL_SQL"));
            record.setOptimizedSql(rs.getString("OPTIMIZED_SQL"));
            record.setDatabaseType(rs.getString("DATABASE_TYPE"));
            record.setStatus(ReviewStatus.valueOf(rs.getString("STATUS")));
            record.setReviewer(rs.getString("REVIEWER"));
            record.setReviewComment(rs.getString("REVIEW_COMMENT"));
            record.setAnalysisTimeMs(rs.getLong("ANALYSIS_TIME_MS"));
            record.setIssuesJson(rs.getString("ISSUES_JSON"));
            record.setAdviceJson(rs.getString("ADVICE_JSON"));
            record.setSubmittedAt(rs.getTimestamp("SUBMITTED_AT"));
            record.setReviewedAt(rs.getTimestamp("REVIEWED_AT"));
            record.setCreatedAt(rs.getTimestamp("CREATED_AT"));
            record.setUpdatedAt(rs.getTimestamp("UPDATED_AT"));
            return record;
        }
    };

    /**
     * 保存记录
     */
    public Long save(OptimizationRecord record) {
        String sql = """
            INSERT INTO SQL_OPTIMIZATION_RECORD (
                ORIGINAL_SQL, OPTIMIZED_SQL, DATABASE_TYPE, STATUS, ANALYSIS_TIME_MS,
                ISSUES_JSON, ADVICE_JSON, SUBMITTED_AT, CREATED_AT, UPDATED_AT
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        jdbcTemplate.update(sql,
                record.getOriginalSql(),
                record.getOptimizedSql(),
                record.getDatabaseType(),
                record.getStatus().name(),
                record.getAnalysisTimeMs(),
                record.getIssuesJson(),
                record.getAdviceJson(),
                record.getSubmittedAt() != null ? record.getSubmittedAt() : new Timestamp(System.currentTimeMillis())
        );

        // 获取自增ID
        return jdbcTemplate.queryForObject("SELECT LAST_IDENTITY()", Long.class);
    }

    /**
     * 根据ID查询
     */
    public OptimizationRecord findById(Long id) {
        String sql = "SELECT * FROM SQL_OPTIMIZATION_RECORD WHERE ID = ?";
        List<OptimizationRecord> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 查询待审核记录
     */
    public List<OptimizationRecord> findPending(int page, int size) {
        String sql = """
            SELECT * FROM SQL_OPTIMIZATION_RECORD
            WHERE STATUS = 'PENDING'
            ORDER BY SUBMITTED_AT DESC
            LIMIT ? OFFSET ?
            """;
        return jdbcTemplate.query(sql, rowMapper, size, page * size);
    }

    /**
     * 查询待审核记录数
     */
    public long countPending() {
        String sql = "SELECT COUNT(*) FROM SQL_OPTIMIZATION_RECORD WHERE STATUS = 'PENDING'";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    /**
     * 更新审核状态
     */
    public void updateStatus(Long id, ReviewStatus status, String reviewer, String comment) {
        String sql = """
            UPDATE SQL_OPTIMIZATION_RECORD
            SET STATUS = ?, REVIEWER = ?, REVIEW_COMMENT = ?, REVIEWED_AT = CURRENT_TIMESTAMP, UPDATED_AT = CURRENT_TIMESTAMP
            WHERE ID = ?
            """;
        jdbcTemplate.update(sql, status.name(), reviewer, comment, id);
    }

    /**
     * 删除记录
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM SQL_OPTIMIZATION_RECORD WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * 查询最近的分析记录
     */
    public List<OptimizationRecord> findRecent(int limit) {
        String sql = "SELECT * FROM SQL_OPTIMIZATION_RECORD ORDER BY CREATED_AT DESC LIMIT ?";
        return jdbcTemplate.query(sql, rowMapper, limit);
    }
}

package com.sqloptimizer.core.cache;

import com.sqloptimizer.core.model.OptimizationReport;

import java.util.Optional;

/**
 * SQL分析缓存接口
 */
public interface SqlAnalysisCache {

    /**
     * 获取缓存的优化报告
     *
     * @param sql SQL语句
     * @param dbType 数据库类型
     * @return 缓存的报告（如果存在且有效）
     */
    Optional<OptimizationReport> get(String sql, String dbType);

    /**
     * 缓存优化报告
     *
     * @param sql SQL语句
     * @param dbType 数据库类型
     * @param report 优化报告
     */
    void put(String sql, String dbType, OptimizationReport report);

    /**
     * 缓存AI响应
     *
     * @param key 缓存Key
     * @param response AI响应
     */
    void putAiResponse(String key, String response);

    /**
     * 获取AI响应
     *
     * @param key 缓存Key
     * @return AI响应（如果存在）
     */
    Optional<String> getAiResponse(String key);

    /**
     * 使缓存失效
     *
     * @param sql SQL语句
     * @param dbType 数据库类型
     */
    void invalidate(String sql, String dbType);

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 获取缓存命中率
     */
    double getHitRate();

    /**
     * 获取缓存大小
     */
    int size();
}

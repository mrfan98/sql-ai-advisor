package com.sqloptimizer.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sqloptimizer.core.model.OptimizationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于Caffeine的本地缓存实现
 */
public class CaffeineCache implements SqlAnalysisCache {

    private static final Logger log = LoggerFactory.getLogger(CaffeineCache.class);

    private final Cache<String, OptimizationReport> reportCache;
    private final Cache<String, String> aiResponseCache;

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    private final long ttlSeconds;
    private final int maxSize;

    public CaffeineCache() {
        this(3600, 1000);
    }

    public CaffeineCache(long ttlSeconds, int maxSize) {
        this.ttlSeconds = ttlSeconds;
        this.maxSize = maxSize;

        this.reportCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build();

        this.aiResponseCache = Caffeine.newBuilder()
                .maximumSize(maxSize * 2)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build();

        log.info("CaffeineCache initialized: ttl={}s, maxSize={}", ttlSeconds, maxSize);
    }

    @Override
    public Optional<OptimizationReport> get(String sql, String dbType) {
        String key = buildKey(sql, dbType);
        OptimizationReport report = reportCache.getIfPresent(key);

        if (report != null) {
            hitCount.incrementAndGet();
            log.debug("Cache hit for key: {}", key);
            return Optional.of(report);
        }

        missCount.incrementAndGet();
        log.debug("Cache miss for key: {}", key);
        return Optional.empty();
    }

    @Override
    public void put(String sql, String dbType, OptimizationReport report) {
        String key = buildKey(sql, dbType);
        reportCache.put(key, report);
        log.debug("Cached report for key: {}", key);
    }

    @Override
    public void putAiResponse(String key, String response) {
        aiResponseCache.put(key, response);
        log.debug("Cached AI response for key: {}", key);
    }

    @Override
    public Optional<String> getAiResponse(String key) {
        String response = aiResponseCache.getIfPresent(key);

        if (response != null) {
            hitCount.incrementAndGet();
            return Optional.of(response);
        }

        missCount.incrementAndGet();
        return Optional.empty();
    }

    @Override
    public void invalidate(String sql, String dbType) {
        String key = buildKey(sql, dbType);
        reportCache.invalidate(key);
        log.debug("Invalidated cache for key: {}", key);
    }

    @Override
    public void clear() {
        reportCache.invalidateAll();
        aiResponseCache.invalidateAll();
        log.info("Cache cleared");
    }

    @Override
    public double getHitRate() {
        long hits = hitCount.get();
        long total = hits + missCount.get();

        if (total == 0) {
            return 0.0;
        }

        return (double) hits / total;
    }

    @Override
    public int size() {
        return (int) (reportCache.estimatedSize() + aiResponseCache.estimatedSize());
    }

    /**
     * 构建缓存Key
     */
    private String buildKey(String sql, String dbType) {
        String normalizedSql = normalizeSql(sql);
        return hash(normalizedSql + ":" + dbType);
    }

    /**
     * 构建AI响应缓存Key
     */
    public String buildAiResponseKey(String sql, String issues, String model) {
        return hash(sql + ":" + issues + ":" + model);
    }

    /**
     * 标准化SQL（去除多余空白）
     */
    private String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.trim().replaceAll("\\s+", " ");
    }

    /**
     * 计算MD5哈希
     */
    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5总是可用
            return input.hashCode() + "";
        }
    }

    /**
     * 获取缓存统计信息
     */
    public String getStats() {
        return String.format("CaffeineCache{size=%d, hitRate=%.2f%%, reportCache=%d, aiResponseCache=%d}",
                size(),
                getHitRate() * 100,
                reportCache.estimatedSize(),
                aiResponseCache.estimatedSize());
    }
}

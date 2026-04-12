package com.sqloptimizer.console;

import com.sqloptimizer.console.service.ReviewService;
import com.sqloptimizer.console.service.ReviewServiceImpl;
import com.sqloptimizer.console.service.SlowQueryService;
import com.sqloptimizer.console.service.SlowQueryServiceImpl;
import com.sqloptimizer.core.database.DatabaseAdapterManager;
import com.sqloptimizer.core.service.SqlOptimizerService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;

/**
 * 控制台自动配置
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "sql.optimizer.console", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ConsoleProperties.class)
public class ConsoleAutoConfiguration {

    /**
     * 审核服务
     */
    @Bean
    @ConditionalOnMissingBean
    public ReviewService reviewService(SqlOptimizerService sqlOptimizerService,
                                       com.sqloptimizer.console.repository.OptimizationRecordRepository recordRepository,
                                       DatabaseAdapterManager adapterManager,
                                       DataSource dataSource) {
        return new ReviewServiceImpl(sqlOptimizerService, recordRepository, adapterManager, dataSource);
    }

    /**
     * 慢查询服务
     */
    @Bean
    @ConditionalOnMissingBean
    public SlowQueryService slowQueryService(
            com.sqloptimizer.console.repository.SlowQueryRecordRepository slowQueryRepository,
            com.sqloptimizer.console.repository.OptimizationRecordRepository optimizationRepository,
            ReviewService reviewService,
            RedisTemplate<String, Object> redisTemplate,
            DataSource dataSource) {
        return new SlowQueryServiceImpl(slowQueryRepository, optimizationRepository, reviewService, redisTemplate, dataSource);
    }
}

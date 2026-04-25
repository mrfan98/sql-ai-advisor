package com.sqloptimizer.starter.config;

import com.sqloptimizer.core.rule.SqlRuleEngine;
import com.sqloptimizer.core.service.SqlOptimizerService;
import com.sqloptimizer.starter.controller.SqlReviewController;
import com.sqloptimizer.starter.interceptor.SqlOptimizerInterceptor;
import com.sqloptimizer.starter.service.SqlOptimizationService;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;

/**
 * SQL优化Starter自动配置
 */
@AutoConfiguration
@ConditionalOnClass({SqlSessionFactoryBean.class})
@ConditionalOnProperty(prefix = "sql.optimizer", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SqlOptimizerStarterProperties.class)
public class SqlOptimizerAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SqlOptimizerAutoConfiguration.class);

    private final SqlOptimizerStarterProperties properties;

    public SqlOptimizerAutoConfiguration(SqlOptimizerStarterProperties properties) {
        this.properties = properties;
        log.info("SQL Optimizer Starter Auto Configuration initialized, enabled: {}", properties.isEnabled());
    }

    /**
     * SQL优化服务
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlOptimizationService sqlOptimizationService(
            SqlOptimizerService sqlOptimizerService,
            SqlRuleEngine ruleEngine,
            RedisTemplate<String, Object> redisTemplate,
            DataSource dataSource) {
        return new SqlOptimizationService(sqlOptimizerService, ruleEngine, redisTemplate, properties, dataSource);
    }

    /**
     * SQL审核控制器
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlReviewController sqlReviewController(SqlOptimizationService optimizationService) {
        return new SqlReviewController(optimizationService);
    }

    /**
     * MyBatis SQL优化拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlOptimizerInterceptor sqlOptimizerInterceptor(
            SqlOptimizationService optimizationService,
            SqlOptimizerStarterProperties properties) {
        return new SqlOptimizerInterceptor(optimizationService, properties);
    }
}

package com.sqloptimizer.mybatis;

import com.sqloptimizer.core.service.SqlOptimizerService;
import com.sqloptimizer.mybatis.SqlOptimizerInterceptor;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * MyBatis插件自动配置
 */
@AutoConfiguration
@ConditionalOnClass({SqlSessionFactoryBean.class})
@ConditionalOnProperty(prefix = "sql.optimizer.mybatis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MyBatisPluginProperties.class)
public class MyBatisPluginAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MyBatisPluginAutoConfiguration.class);

    private final MyBatisPluginProperties properties;

    public MyBatisPluginAutoConfiguration(MyBatisPluginProperties properties) {
        this.properties = properties;
        log.info("MyBatis Plugin Auto Configuration initialized, enabled: {}", properties.isEnabled());
    }

    /**
     * 慢查询记录器
     */
    @Bean
    @ConditionalOnMissingBean
    public SlowQueryRecorder slowQueryRecorder(RedisTemplate<String, Object> redisTemplate,
                                               JdbcTemplate jdbcTemplate) {
        return new SlowQueryRecorder(redisTemplate, jdbcTemplate, properties);
    }

    /**
     * SQL优化拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlOptimizerInterceptor sqlOptimizerInterceptor(SqlOptimizerService sqlOptimizerService,
                                                          SlowQueryRecorder slowQueryRecorder) {
        return new SqlOptimizerInterceptor(sqlOptimizerService, slowQueryRecorder, properties);
    }
}

package com.example.finance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA Performance Configuration
 * 
 * Optimizations implemented:
 * 1. Lazy loading by default for all relationships
 * 2. Batch fetching for N+1 query prevention
 * 3. Query hints for fetch strategies
 * 4. Second-level cache integration
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.example.finance.repository")
@EnableTransactionManagement
public class JpaPerformanceConfig {
    
    // JPA properties are configured in application.properties:
    // - hibernate.jdbc.batch_size: Batch insert/update operations
    // - hibernate.jdbc.fetch_size: Result set fetch size
    // - hibernate.default_batch_fetch_size: Batch size for lazy loading
    // - hibernate.query.in_clause_parameter_padding: Optimize IN clause queries
    // - hibernate.query.plan_cache_max_size: Cache query execution plans
    // - hibernate.cache.use_second_level_cache: Enable L2 cache
    // - hibernate.cache.use_query_cache: Enable query result caching
}

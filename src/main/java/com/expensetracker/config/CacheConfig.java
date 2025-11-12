package com.expensetracker.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Register all caches used by the application so @Cacheable/@CacheEvict can find them
        return new ConcurrentMapCacheManager("expenseCategories", "users", "incomes", "expenses", "userExpenseCategories");
    }
}

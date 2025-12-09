package com.example.finance.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.caffeine.CaffeineCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

@Component
public class CacheHealthIndicator implements HealthIndicator {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public Health health() {
        try {
            Health.Builder builder = Health.up();
            
            cacheManager.getCacheNames().forEach(cacheName -> {
                CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
                if (cache != null) {
                    CacheStats stats = cache.getNativeCache().stats();
                    
                    builder.withDetail(cacheName + "_hitRate", 
                        String.format("%.2f%%", stats.hitRate() * 100));
                    builder.withDetail(cacheName + "_hitCount", stats.hitCount());
                    builder.withDetail(cacheName + "_missCount", stats.missCount());
                    builder.withDetail(cacheName + "_size", cache.getNativeCache().estimatedSize());
                }
            });
            
            return builder.build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

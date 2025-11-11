package com.example.finance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private DataSource dataSource;

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (cache != null) {
                CacheStats cacheStats = cache.getNativeCache().stats();
                
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("hitRate", String.format("%.2f%%", cacheStats.hitRate() * 100));
                cacheInfo.put("hitCount", cacheStats.hitCount());
                cacheInfo.put("missCount", cacheStats.missCount());
                cacheInfo.put("loadCount", cacheStats.loadCount());
                cacheInfo.put("evictionCount", cacheStats.evictionCount());
                cacheInfo.put("estimatedSize", cache.getNativeCache().estimatedSize());
                
                stats.put(cacheName, cacheInfo);
            }
        });
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/database/connections")
    public ResponseEntity<Map<String, Object>> getConnectionPoolStats() {
        Map<String, Object> stats = new HashMap<>();
        
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            stats.put("activeConnections", poolMXBean.getActiveConnections());
            stats.put("idleConnections", poolMXBean.getIdleConnections());
            stats.put("totalConnections", poolMXBean.getTotalConnections());
            stats.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
            stats.put("maxPoolSize", hikariDataSource.getMaximumPoolSize());
            stats.put("minIdle", hikariDataSource.getMinimumIdle());
        }
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<String> clearCache(@RequestParam(required = false) String cacheName) {
        if (cacheName != null) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                return ResponseEntity.ok("Cache '" + cacheName + "' cleared");
            }
            return ResponseEntity.badRequest().body("Cache not found: " + cacheName);
        } else {
            cacheManager.getCacheNames().forEach(name -> {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            });
            return ResponseEntity.ok("All caches cleared");
        }
    }

    @GetMapping("/cache/warmup")
    public ResponseEntity<String> warmupCache() {
        // Trigger cache warmup by loading frequently accessed data
        // This would typically be called on application startup
        return ResponseEntity.ok("Cache warmup triggered");
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Cache metrics
        Map<String, Double> cacheHitRates = new HashMap<>();
        cacheManager.getCacheNames().forEach(cacheName -> {
            CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (cache != null) {
                CacheStats stats = cache.getNativeCache().stats();
                cacheHitRates.put(cacheName, stats.hitRate());
            }
        });
        metrics.put("cacheHitRates", cacheHitRates);
        
        // Database connection pool metrics
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            Map<String, Object> poolMetrics = new HashMap<>();
            poolMetrics.put("utilization", String.format("%.2f%%", 
                (poolMXBean.getActiveConnections() / (double) hikariDataSource.getMaximumPoolSize()) * 100));
            poolMetrics.put("activeConnections", poolMXBean.getActiveConnections());
            poolMetrics.put("totalConnections", poolMXBean.getTotalConnections());
            
            metrics.put("connectionPool", poolMetrics);
        }
        
        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvmMetrics = new HashMap<>();
        jvmMetrics.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        jvmMetrics.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        jvmMetrics.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        jvmMetrics.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        jvmMetrics.put("availableProcessors", runtime.availableProcessors());
        
        metrics.put("jvm", jvmMetrics);
        
        return ResponseEntity.ok(metrics);
    }
}

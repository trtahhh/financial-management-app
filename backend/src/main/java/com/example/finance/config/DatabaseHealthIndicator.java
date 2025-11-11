package com.example.finance.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import javax.sql.DataSource;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                int activeConnections = poolMXBean.getActiveConnections();
                int idleConnections = poolMXBean.getIdleConnections();
                int totalConnections = poolMXBean.getTotalConnections();
                int threadsAwaitingConnection = poolMXBean.getThreadsAwaitingConnection();
                
                return Health.up()
                    .withDetail("database", "HikariCP")
                    .withDetail("activeConnections", activeConnections)
                    .withDetail("idleConnections", idleConnections)
                    .withDetail("totalConnections", totalConnections)
                    .withDetail("threadsAwaitingConnection", threadsAwaitingConnection)
                    .withDetail("maxPoolSize", hikariDataSource.getMaximumPoolSize())
                    .build();
            }
            
            return Health.up().withDetail("database", "Connected").build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

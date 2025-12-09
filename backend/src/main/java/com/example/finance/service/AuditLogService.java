package com.example.finance.service;

import com.example.finance.entity.AuditLog;
import com.example.finance.entity.User;
import com.example.finance.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Log user action
     */
    @Transactional
    public void logAction(User user, String action, String entityType, Long entityId, 
                          String ipAddress, String userAgent, Map<String, Object> details) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setSuccess(true);
        
        if (details != null && !details.isEmpty()) {
            try {
                log.setDetails(objectMapper.writeValueAsString(details));
            } catch (Exception e) {
                log.setDetails("{}");
            }
        }
        
        auditLogRepository.save(log);
    }

    /**
     * Log failed action
     */
    @Transactional
    public void logFailedAction(User user, String action, String entityType, Long entityId,
                                String ipAddress, String userAgent, String errorMessage) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setSuccess(false);
        log.setErrorMessage(errorMessage);
        
        auditLogRepository.save(log);
    }

    /**
     * Log authentication event
     */
    @Transactional
    public void logAuthentication(User user, String action, String ipAddress, String userAgent, boolean success) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action); // LOGIN, LOGOUT, 2FA_VERIFY, etc.
        log.setEntityType("Authentication");
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setSuccess(success);
        
        auditLogRepository.save(log);
    }

    /**
     * Get user audit logs
     */
    public List<AuditLog> getUserAuditLogs(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get audit logs by date range
     */
    public List<AuditLog> getAuditLogsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * Get failed actions
     */
    public List<AuditLog> getFailedActions(String action) {
        return auditLogRepository.findFailedActionsByType(action);
    }

    /**
     * Get entity history
     */
    public List<AuditLog> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get audit statistics
     */
    public Map<String, Object> getAuditStatistics(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLog> logs = auditLogRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActions", logs.size());
        
        // Count by action type
        Map<String, Long> actionCounts = new HashMap<>();
        for (AuditLog log : logs) {
            actionCounts.put(log.getAction(), actionCounts.getOrDefault(log.getAction(), 0L) + 1);
        }
        stats.put("actionBreakdown", actionCounts);
        
        // Count success vs failure
        long successCount = logs.stream().filter(AuditLog::getSuccess).count();
        long failureCount = logs.size() - successCount;
        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);
        
        // Count by entity type
        Map<String, Long> entityCounts = new HashMap<>();
        for (AuditLog log : logs) {
            if (log.getEntityType() != null) {
                entityCounts.put(log.getEntityType(), entityCounts.getOrDefault(log.getEntityType(), 0L) + 1);
            }
        }
        stats.put("entityBreakdown", entityCounts);
        
        return stats;
    }

    /**
     * Get recent activity
     */
    public List<Map<String, Object>> getRecentActivity(Long userId, int limit) {
        List<AuditLog> logs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        List<Map<String, Object>> activity = new ArrayList<>();
        int count = 0;
        
        for (AuditLog log : logs) {
            if (count >= limit) break;
            
            Map<String, Object> item = new HashMap<>();
            item.put("action", log.getAction());
            item.put("entityType", log.getEntityType());
            item.put("entityId", log.getEntityId());
            item.put("success", log.getSuccess());
            item.put("timestamp", log.getCreatedAt());
            item.put("ipAddress", log.getIpAddress());
            
            activity.add(item);
            count++;
        }
        
        return activity;
    }
}

package com.example.finance.service;

import com.example.finance.entity.AuditLog;
import com.example.finance.entity.User;
import com.example.finance.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("Should log action successfully")
    void testLogAction_Success() {
        // Arrange
        String action = "CREATE";
        String entityType = "Transaction";
        Long entityId = 123L;
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        Map<String, Object> details = new HashMap<>();
        details.put("amount", 10000);
        details.put("category", "Food");
        
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditLogService.logAction(testUser, action, entityType, entityId, ipAddress, userAgent, details);

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        AuditLog log = captor.getValue();
        assertEquals(testUser, log.getUser());
        assertEquals(action, log.getAction());
        assertEquals(entityType, log.getEntityType());
        assertEquals(entityId, log.getEntityId());
        assertEquals(ipAddress, log.getIpAddress());
        assertEquals(userAgent, log.getUserAgent());
        assertTrue(log.getSuccess());
        assertNotNull(log.getDetails());
    }

    @Test
    @DisplayName("Should log action without details")
    void testLogAction_NoDetails() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditLogService.logAction(testUser, "VIEW", "Budget", 1L, "192.168.1.1", "Chrome", null);

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        AuditLog log = captor.getValue();
        assertNull(log.getDetails());
        assertTrue(log.getSuccess());
    }

    @Test
    @DisplayName("Should log action with empty details map")
    void testLogAction_EmptyDetails() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditLogService.logAction(
            testUser, "UPDATE", "Goal", 2L, "192.168.1.1", "Safari", new HashMap<>());

        // Assert
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should log failed action")
    void testLogFailedAction() {
        // Arrange
        String errorMessage = "Insufficient permissions";
        
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditLogService.logFailedAction(
            testUser, "DELETE", "Transaction", 100L, "192.168.1.1", "Firefox", errorMessage);

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        AuditLog log = captor.getValue();
        assertEquals("DELETE", log.getAction());
        assertFalse(log.getSuccess());
        assertEquals(errorMessage, log.getErrorMessage());
    }

    @Test
    @DisplayName("Should log authentication event")
    void testLogAuthentication_Success() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditLogService.logAuthentication(testUser, "LOGIN", "192.168.1.1", "Chrome", true);

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        AuditLog log = captor.getValue();
        assertEquals("LOGIN", log.getAction());
        assertEquals("Authentication", log.getEntityType());
        assertTrue(log.getSuccess());
    }

    @Test
    @DisplayName("Should log failed authentication")
    void testLogAuthentication_Failed() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditLogService.logAuthentication(testUser, "LOGIN", "192.168.1.1", "Safari", false);

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        AuditLog log = captor.getValue();
        assertFalse(log.getSuccess());
    }

    @Test
    @DisplayName("Should get user audit logs")
    void testGetUserAuditLogs() {
        // Arrange
        List<AuditLog> logs = Arrays.asList(
            createAuditLog("CREATE", true),
            createAuditLog("UPDATE", true),
            createAuditLog("DELETE", false)
        );
        
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(logs);

        // Act
        List<AuditLog> result = auditLogService.getUserAuditLogs(1L);

        // Assert
        assertEquals(3, result.size());
        verify(auditLogRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("Should get audit logs by date range")
    void testGetAuditLogsByDateRange() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        List<AuditLog> logs = Arrays.asList(createAuditLog("VIEW", true));
        
        when(auditLogRepository.findByUserIdAndDateRange(1L, startDate, endDate))
            .thenReturn(logs);

        // Act
        List<AuditLog> result = auditLogService.getAuditLogsByDateRange(1L, startDate, endDate);

        // Assert
        assertEquals(1, result.size());
        verify(auditLogRepository).findByUserIdAndDateRange(1L, startDate, endDate);
    }

    @Test
    @DisplayName("Should get failed actions")
    void testGetFailedActions() {
        // Arrange
        List<AuditLog> failedLogs = Arrays.asList(
            createAuditLog("CREATE", false),
            createAuditLog("CREATE", false)
        );
        
        when(auditLogRepository.findFailedActionsByType("CREATE"))
            .thenReturn(failedLogs);

        // Act
        List<AuditLog> result = auditLogService.getFailedActions("CREATE");

        // Assert
        assertEquals(2, result.size());
        verify(auditLogRepository).findFailedActionsByType("CREATE");
    }

    @Test
    @DisplayName("Should get entity history")
    void testGetEntityHistory() {
        // Arrange
        List<AuditLog> history = Arrays.asList(
            createAuditLog("CREATE", true),
            createAuditLog("UPDATE", true),
            createAuditLog("UPDATE", true)
        );
        
        when(auditLogRepository.findByEntityTypeAndEntityId("Transaction", 100L))
            .thenReturn(history);

        // Act
        List<AuditLog> result = auditLogService.getEntityHistory("Transaction", 100L);

        // Assert
        assertEquals(3, result.size());
        verify(auditLogRepository).findByEntityTypeAndEntityId("Transaction", 100L);
    }

    @Test
    @DisplayName("Should get comprehensive audit statistics")
    void testGetAuditStatistics() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        List<AuditLog> logs = new ArrayList<>();
        
        // Create various logs
        AuditLog createLog = createAuditLog("CREATE", true);
        createLog.setEntityType("Transaction");
        logs.add(createLog);
        
        AuditLog updateLog = createAuditLog("UPDATE", true);
        updateLog.setEntityType("Transaction");
        logs.add(updateLog);
        
        AuditLog deleteLog = createAuditLog("DELETE", false);
        deleteLog.setEntityType("Budget");
        logs.add(deleteLog);
        
        AuditLog viewLog = createAuditLog("VIEW", true);
        viewLog.setEntityType("Goal");
        logs.add(viewLog);
        
        when(auditLogRepository.findByUserIdAndDateRange(1L, startDate, endDate))
            .thenReturn(logs);

        // Act
        Map<String, Object> stats = auditLogService.getAuditStatistics(1L, startDate, endDate);

        // Assert
        assertEquals(4, stats.get("totalActions"));
        assertEquals(3L, stats.get("successCount"));
        assertEquals(1L, stats.get("failureCount"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> actionBreakdown = (Map<String, Long>) stats.get("actionBreakdown");
        assertEquals(1L, actionBreakdown.get("CREATE"));
        assertEquals(1L, actionBreakdown.get("UPDATE"));
        assertEquals(1L, actionBreakdown.get("DELETE"));
        assertEquals(1L, actionBreakdown.get("VIEW"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> entityBreakdown = (Map<String, Long>) stats.get("entityBreakdown");
        assertEquals(2L, entityBreakdown.get("Transaction"));
        assertEquals(1L, entityBreakdown.get("Budget"));
        assertEquals(1L, entityBreakdown.get("Goal"));
    }

    @Test
    @DisplayName("Should handle empty statistics")
    void testGetAuditStatistics_Empty() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(auditLogRepository.findByUserIdAndDateRange(1L, startDate, endDate))
            .thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> stats = auditLogService.getAuditStatistics(1L, startDate, endDate);

        // Assert
        assertEquals(0, stats.get("totalActions"));
        assertEquals(0L, stats.get("successCount"));
        assertEquals(0L, stats.get("failureCount"));
    }

    @Test
    @DisplayName("Should get recent activity with limit")
    void testGetRecentActivity() {
        // Arrange
        List<AuditLog> logs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AuditLog log = createAuditLog("ACTION_" + i, true);
            log.setEntityType("Entity_" + i);
            log.setEntityId((long) i);
            logs.add(log);
        }
        
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(logs);

        // Act
        List<Map<String, Object>> activity = auditLogService.getRecentActivity(1L, 5);

        // Assert
        assertEquals(5, activity.size());
        assertEquals("ACTION_0", activity.get(0).get("action"));
        assertTrue(activity.get(0).containsKey("timestamp"));
    }

    @Test
    @DisplayName("Should handle recent activity with no logs")
    void testGetRecentActivity_Empty() {
        // Arrange
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> activity = auditLogService.getRecentActivity(1L, 5);

        // Assert
        assertEquals(0, activity.size());
    }

    @Test
    @DisplayName("Should return all logs when limit exceeds available")
    void testGetRecentActivity_LimitExceedsAvailable() {
        // Arrange
        List<AuditLog> logs = Arrays.asList(
            createAuditLog("CREATE", true),
            createAuditLog("UPDATE", true)
        );
        
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(logs);

        // Act
        List<Map<String, Object>> activity = auditLogService.getRecentActivity(1L, 10);

        // Assert
        assertEquals(2, activity.size());
    }

    private AuditLog createAuditLog(String action, boolean success) {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setUser(testUser);
        log.setAction(action);
        log.setEntityType("TestEntity");
        log.setEntityId(1L);
        log.setIpAddress("192.168.1.1");
        log.setUserAgent("Test Agent");
        log.setSuccess(success);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}

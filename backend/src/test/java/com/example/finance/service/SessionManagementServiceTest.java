package com.example.finance.service;

import com.example.finance.entity.User;
import com.example.finance.entity.UserSession;
import com.example.finance.repository.UserSessionRepository;
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
class SessionManagementServiceTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @InjectMocks
    private SessionManagementService sessionManagementService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("Should create new session successfully")
    void testCreateSession_Success() {
        // Arrange
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0)";
        
        when(sessionRepository.findActiveSessionsByUserId(anyLong(), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(sessionRepository.save(any(UserSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserSession session = sessionManagementService.createSession(testUser, ipAddress, userAgent);

        // Assert
        assertNotNull(session);
        assertEquals(testUser, session.getUser());
        assertEquals(ipAddress, session.getIpAddress());
        assertTrue(session.getActive());
        assertNotNull(session.getSessionToken());
        assertNotNull(session.getRefreshToken());
        assertEquals("Windows PC", session.getDeviceInfo());
        verify(sessionRepository).save(any(UserSession.class));
    }

    @Test
    @DisplayName("Should revoke oldest session when max sessions reached")
    void testCreateSession_MaxSessionsReached() {
        // Arrange
        List<UserSession> activeSessions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UserSession session = new UserSession();
            session.setId((long) i);
            session.setSessionToken("token-" + i);
            session.setLastActivity(LocalDateTime.now().minusHours(i));
            activeSessions.add(session);
        }
        
        when(sessionRepository.findActiveSessionsByUserId(anyLong(), any(LocalDateTime.class)))
            .thenReturn(activeSessions);
        when(sessionRepository.findBySessionToken(anyString()))
            .thenReturn(Optional.of(activeSessions.get(4))); // oldest
        when(sessionRepository.save(any(UserSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserSession newSession = sessionManagementService.createSession(
            testUser, "192.168.1.1", "Chrome");

        // Assert
        assertNotNull(newSession);
        verify(sessionRepository, times(2)).save(any(UserSession.class)); // revoke + create
    }

    @Test
    @DisplayName("Should parse device info from user agent")
    void testCreateSession_ParsesDeviceInfo() {
        // Arrange
        when(sessionRepository.findActiveSessionsByUserId(anyLong(), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(sessionRepository.save(any(UserSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert - Windows
        UserSession windowsSession = sessionManagementService.createSession(
            testUser, "192.168.1.1", "Windows NT 10.0");
        assertEquals("Windows PC", windowsSession.getDeviceInfo());

        // Mac
        UserSession macSession = sessionManagementService.createSession(
            testUser, "192.168.1.1", "Macintosh; Intel Mac OS X");
        assertEquals("Mac", macSession.getDeviceInfo());

        // Android
        UserSession androidSession = sessionManagementService.createSession(
            testUser, "192.168.1.1", "Android 11");
        assertEquals("Android Device", androidSession.getDeviceInfo());

        // iPhone
        UserSession iosSession = sessionManagementService.createSession(
            testUser, "192.168.1.1", "iPhone OS 14");
        assertEquals("iOS Device", iosSession.getDeviceInfo());

        // Unknown
        UserSession unknownSession = sessionManagementService.createSession(
            testUser, "192.168.1.1", null);
        assertEquals("Unknown Device", unknownSession.getDeviceInfo());
    }

    @Test
    @DisplayName("Should validate active session successfully")
    void testValidateSession_Success() {
        // Arrange
        UserSession session = new UserSession();
        session.setSessionToken("valid-token");
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now().plusHours(1));
        session.setLastActivity(LocalDateTime.now().minusMinutes(5));
        
        when(sessionRepository.findBySessionToken("valid-token"))
            .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(UserSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<UserSession> result = sessionManagementService.validateSession("valid-token");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(session, result.get());
        verify(sessionRepository).save(any(UserSession.class)); // updates last activity
    }

    @Test
    @DisplayName("Should reject expired session")
    void testValidateSession_Expired() {
        // Arrange
        UserSession session = new UserSession();
        session.setSessionToken("expired-token");
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now().minusHours(1)); // expired
        
        when(sessionRepository.findBySessionToken("expired-token"))
            .thenReturn(Optional.of(session));

        // Act
        Optional<UserSession> result = sessionManagementService.validateSession("expired-token");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should reject inactive session")
    void testValidateSession_Inactive() {
        // Arrange
        UserSession session = new UserSession();
        session.setSessionToken("inactive-token");
        session.setActive(false);
        session.setExpiresAt(LocalDateTime.now().plusHours(1));
        
        when(sessionRepository.findBySessionToken("inactive-token"))
            .thenReturn(Optional.of(session));

        // Act
        Optional<UserSession> result = sessionManagementService.validateSession("inactive-token");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should reject non-existent session")
    void testValidateSession_NotFound() {
        // Arrange
        when(sessionRepository.findBySessionToken("nonexistent"))
            .thenReturn(Optional.empty());

        // Act
        Optional<UserSession> result = sessionManagementService.validateSession("nonexistent");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should refresh session successfully")
    void testRefreshSession_Success() {
        // Arrange
        UserSession session = new UserSession();
        session.setRefreshToken("refresh-token");
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now().minusHours(1)); // expired but can refresh
        session.setSessionToken("old-token");
        
        when(sessionRepository.findByRefreshToken("refresh-token"))
            .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(UserSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<UserSession> result = sessionManagementService.refreshSession("refresh-token");

        // Assert
        assertTrue(result.isPresent());
        assertNotEquals("old-token", result.get().getSessionToken()); // new token generated
        assertTrue(result.get().getExpiresAt().isAfter(LocalDateTime.now())); // extended
    }

    @Test
    @DisplayName("Should reject refresh for expired refresh token")
    void testRefreshSession_RefreshTokenExpired() {
        // Arrange
        UserSession session = new UserSession();
        session.setRefreshToken("expired-refresh");
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now().minusDays(31)); // refresh token expired
        
        when(sessionRepository.findByRefreshToken("expired-refresh"))
            .thenReturn(Optional.of(session));

        // Act
        Optional<UserSession> result = sessionManagementService.refreshSession("expired-refresh");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should revoke session successfully")
    void testRevokeSession() {
        // Arrange
        UserSession session = new UserSession();
        session.setSessionToken("token-to-revoke");
        session.setActive(true);
        
        when(sessionRepository.findBySessionToken("token-to-revoke"))
            .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(UserSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sessionManagementService.revokeSession("token-to-revoke");

        // Assert
        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(captor.capture());
        assertFalse(captor.getValue().getActive());
        assertNotNull(captor.getValue().getRevokedAt());
    }

    @Test
    @DisplayName("Should revoke all user sessions")
    void testRevokeAllUserSessions() {
        // Arrange
        List<UserSession> sessions = Arrays.asList(
            createActiveSession("token1"),
            createActiveSession("token2"),
            createActiveSession("token3")
        );
        
        when(sessionRepository.findByUserIdAndActiveTrue(1L))
            .thenReturn(sessions);
        when(sessionRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sessionManagementService.revokeAllUserSessions(1L);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserSession>> captor = ArgumentCaptor.forClass(List.class);
        verify(sessionRepository).saveAll(captor.capture());
        List<UserSession> revokedSessions = captor.getValue();
        
        assertEquals(3, revokedSessions.size());
        revokedSessions.forEach(session -> {
            assertFalse(session.getActive());
            assertNotNull(session.getRevokedAt());
        });
    }

    @Test
    @DisplayName("Should get active sessions")
    void testGetActiveSessions() {
        // Arrange
        List<UserSession> sessions = Arrays.asList(
            createActiveSession("token1"),
            createActiveSession("token2")
        );
        sessions.get(0).setDeviceInfo("Windows PC");
        sessions.get(1).setDeviceInfo("Android Device");
        
        when(sessionRepository.findActiveSessionsByUserId(eq(1L), any(LocalDateTime.class)))
            .thenReturn(sessions);

        // Act
        List<Map<String, Object>> result = sessionManagementService.getActiveSessions(1L);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Windows PC", result.get(0).get("deviceInfo"));
        assertEquals("Android Device", result.get(1).get("deviceInfo"));
    }

    @Test
    @DisplayName("Should cleanup expired sessions")
    void testCleanupExpiredSessions() {
        // Arrange
        List<UserSession> expiredSessions = Arrays.asList(
            createActiveSession("expired1"),
            createActiveSession("expired2")
        );
        
        when(sessionRepository.findExpiredSessions(any(LocalDateTime.class)))
            .thenReturn(expiredSessions);
        when(sessionRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sessionManagementService.cleanupExpiredSessions();

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserSession>> captor = ArgumentCaptor.forClass(List.class);
        verify(sessionRepository).saveAll(captor.capture());
        
        captor.getValue().forEach(session -> {
            assertFalse(session.getActive());
            assertNotNull(session.getRevokedAt());
        });
    }

    @Test
    @DisplayName("Should get session statistics")
    void testGetSessionStatistics() {
        // Arrange
        List<UserSession> allSessions = Arrays.asList(
            createActiveSession("token1"),
            createActiveSession("token2"),
            createInactiveSession("token3")
        );
        allSessions.get(0).setDeviceInfo("Windows PC");
        allSessions.get(1).setDeviceInfo("Windows PC");
        allSessions.get(2).setDeviceInfo("Android Device");
        
        List<UserSession> activeSessions = Arrays.asList(
            allSessions.get(0),
            allSessions.get(1)
        );
        
        when(sessionRepository.findByUserId(1L)).thenReturn(allSessions);
        when(sessionRepository.findActiveSessionsByUserId(eq(1L), any(LocalDateTime.class)))
            .thenReturn(activeSessions);

        // Act
        Map<String, Object> stats = sessionManagementService.getSessionStatistics(1L);

        // Assert
        assertEquals(3, stats.get("totalSessions"));
        assertEquals(2, stats.get("activeSessions"));
        assertEquals(1, stats.get("revokedSessions"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> deviceBreakdown = (Map<String, Long>) stats.get("deviceBreakdown");
        assertEquals(2L, deviceBreakdown.get("Windows PC"));
        assertEquals(1L, deviceBreakdown.get("Android Device"));
    }

    private UserSession createActiveSession(String token) {
        UserSession session = new UserSession();
        session.setSessionToken(token);
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now().plusHours(24));
        session.setLastActivity(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        return session;
    }

    private UserSession createInactiveSession(String token) {
        UserSession session = createActiveSession(token);
        session.setActive(false);
        session.setRevokedAt(LocalDateTime.now());
        return session;
    }
}

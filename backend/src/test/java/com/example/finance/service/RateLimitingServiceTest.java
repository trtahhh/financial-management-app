package com.example.finance.service;

import com.example.finance.entity.LoginAttempt;
import com.example.finance.repository.LoginAttemptRepository;
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
class RateLimitingServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private RateLimitingService rateLimitingService;

    @Test
    @DisplayName("Should record successful login attempt")
    void testRecordLoginAttempt_Success() {
        // Arrange
        String username = "testuser";
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        
        when(loginAttemptRepository.save(any(LoginAttempt.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        rateLimitingService.recordLoginAttempt(username, ipAddress, userAgent, true, null);

        // Assert
        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(captor.capture());
        
        LoginAttempt attempt = captor.getValue();
        assertEquals(username, attempt.getUsername());
        assertEquals(ipAddress, attempt.getIpAddress());
        assertEquals(userAgent, attempt.getUserAgent());
        assertTrue(attempt.getSuccess());
        assertNull(attempt.getFailureReason());
    }

    @Test
    @DisplayName("Should record failed login attempt with reason")
    void testRecordLoginAttempt_Failed() {
        // Arrange
        String username = "testuser";
        String failureReason = "Invalid password";
        
        when(loginAttemptRepository.save(any(LoginAttempt.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        rateLimitingService.recordLoginAttempt(
            username, "192.168.1.1", "Chrome", false, failureReason);

        // Assert
        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(captor.capture());
        
        LoginAttempt attempt = captor.getValue();
        assertFalse(attempt.getSuccess());
        assertEquals(failureReason, attempt.getFailureReason());
    }

    @Test
    @DisplayName("Should not lock account with less than 5 failed attempts")
    void testIsAccountLocked_NotLocked() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(3L);

        // Act
        boolean isLocked = rateLimitingService.isAccountLocked("testuser");

        // Assert
        assertFalse(isLocked);
    }

    @Test
    @DisplayName("Should lock account after 5 failed attempts")
    void testIsAccountLocked_Locked() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(5L);

        // Act
        boolean isLocked = rateLimitingService.isAccountLocked("testuser");

        // Assert
        assertTrue(isLocked);
    }

    @Test
    @DisplayName("Should lock account after exceeding max attempts")
    void testIsAccountLocked_ExceedsMax() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(10L);

        // Act
        boolean isLocked = rateLimitingService.isAccountLocked("testuser");

        // Assert
        assertTrue(isLocked);
    }

    @Test
    @DisplayName("Should not block IP with less than 5 failed attempts")
    void testIsIpBlocked_NotBlocked() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsByIpSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(2L);

        // Act
        boolean isBlocked = rateLimitingService.isIpBlocked("192.168.1.1");

        // Assert
        assertFalse(isBlocked);
    }

    @Test
    @DisplayName("Should block IP after 5 failed attempts")
    void testIsIpBlocked_Blocked() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsByIpSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(5L);

        // Act
        boolean isBlocked = rateLimitingService.isIpBlocked("192.168.1.1");

        // Assert
        assertTrue(isBlocked);
    }

    @Test
    @DisplayName("Should get correct failed attempts count")
    void testGetFailedAttemptsCount() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(3L);

        // Act
        int count = rateLimitingService.getFailedAttemptsCount("testuser");

        // Assert
        assertEquals(3, count);
    }

    @Test
    @DisplayName("Should calculate remaining attempts correctly")
    void testGetRemainingAttempts_WithFailures() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(2L);

        // Act
        int remaining = rateLimitingService.getRemainingAttempts("testuser");

        // Assert
        assertEquals(3, remaining); // 5 - 2 = 3
    }

    @Test
    @DisplayName("Should return zero remaining attempts when locked")
    void testGetRemainingAttempts_Locked() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(5L);

        // Act
        int remaining = rateLimitingService.getRemainingAttempts("testuser");

        // Assert
        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("Should return max attempts when no failures")
    void testGetRemainingAttempts_NoFailures() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);

        // Act
        int remaining = rateLimitingService.getRemainingAttempts("testuser");

        // Assert
        assertEquals(5, remaining);
    }

    @Test
    @DisplayName("Should return empty when account not locked")
    void testGetLockoutExpiryTime_NotLocked() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(2L);

        // Act
        Optional<LocalDateTime> expiryTime = rateLimitingService.getLockoutExpiryTime("testuser");

        // Assert
        assertFalse(expiryTime.isPresent());
    }

    @Test
    @DisplayName("Should return lockout expiry time when locked")
    void testGetLockoutExpiryTime_Locked() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(5L);
        
        LocalDateTime firstFailure = LocalDateTime.now().minusMinutes(10);
        List<LoginAttempt> attempts = new ArrayList<>();
        
        // Create 5 failed attempts
        for (int i = 0; i < 5; i++) {
            LoginAttempt attempt = new LoginAttempt();
            attempt.setUsername("testuser");
            attempt.setSuccess(false);
            attempt.setCreatedAt(firstFailure.plusMinutes(i));
            attempts.add(attempt);
        }
        
        when(loginAttemptRepository.findRecentAttemptsByUsername("testuser"))
            .thenReturn(attempts);

        // Act
        Optional<LocalDateTime> expiryTime = rateLimitingService.getLockoutExpiryTime("testuser");

        // Assert
        assertTrue(expiryTime.isPresent());
        assertTrue(expiryTime.get().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should return empty when no recent attempts")
    void testGetLockoutExpiryTime_NoAttempts() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(5L);
        when(loginAttemptRepository.findRecentAttemptsByUsername("testuser"))
            .thenReturn(Collections.emptyList());

        // Act
        Optional<LocalDateTime> expiryTime = rateLimitingService.getLockoutExpiryTime("testuser");

        // Assert
        assertFalse(expiryTime.isPresent());
    }

    @Test
    @DisplayName("Should get comprehensive login statistics")
    void testGetLoginStatistics() {
        // Arrange
        List<LoginAttempt> attempts = new ArrayList<>();
        
        // 3 successful attempts
        for (int i = 0; i < 3; i++) {
            LoginAttempt attempt = new LoginAttempt();
            attempt.setSuccess(true);
            attempt.setCreatedAt(LocalDateTime.now().minusMinutes(i));
            attempts.add(attempt);
        }
        
        // 2 failed attempts
        for (int i = 0; i < 2; i++) {
            LoginAttempt attempt = new LoginAttempt();
            attempt.setSuccess(false);
            attempt.setCreatedAt(LocalDateTime.now().minusMinutes(10 + i));
            attempts.add(attempt);
        }
        
        when(loginAttemptRepository.findRecentAttemptsByUsername("testuser"))
            .thenReturn(attempts);
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(2L);

        // Act
        Map<String, Object> stats = rateLimitingService.getLoginStatistics("testuser");

        // Assert
        assertEquals(5, stats.get("totalAttempts"));
        assertEquals(3L, stats.get("successfulAttempts"));
        assertEquals(2L, stats.get("failedAttempts"));
        assertEquals(false, stats.get("isLocked"));
        assertEquals(3, stats.get("remainingAttempts"));
    }

    @Test
    @DisplayName("Should include lockout expiry in statistics when locked")
    void testGetLoginStatistics_WithLockout() {
        // Arrange
        List<LoginAttempt> attempts = new ArrayList<>();
        LocalDateTime firstFailure = LocalDateTime.now().minusMinutes(10);
        
        // 5 failed attempts
        for (int i = 0; i < 5; i++) {
            LoginAttempt attempt = new LoginAttempt();
            attempt.setSuccess(false);
            attempt.setCreatedAt(firstFailure.plusMinutes(i));
            attempts.add(attempt);
        }
        
        when(loginAttemptRepository.findRecentAttemptsByUsername("testuser"))
            .thenReturn(attempts);
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(5L);

        // Act
        Map<String, Object> stats = rateLimitingService.getLoginStatistics("testuser");

        // Assert
        assertEquals(true, stats.get("isLocked"));
        assertEquals(0, stats.get("remainingAttempts"));
        assertTrue(stats.containsKey("lockoutExpiresAt"));
    }

    @Test
    @DisplayName("Should handle empty statistics")
    void testGetLoginStatistics_NoAttempts() {
        // Arrange
        when(loginAttemptRepository.findRecentAttemptsByUsername("testuser"))
            .thenReturn(Collections.emptyList());
        when(loginAttemptRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);

        // Act
        Map<String, Object> stats = rateLimitingService.getLoginStatistics("testuser");

        // Assert
        assertEquals(0, stats.get("totalAttempts"));
        assertEquals(0L, stats.get("successfulAttempts"));
        assertEquals(0L, stats.get("failedAttempts"));
        assertEquals(false, stats.get("isLocked"));
        assertEquals(5, stats.get("remainingAttempts"));
    }
}

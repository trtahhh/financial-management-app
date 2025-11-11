package com.example.finance.service;

import com.example.finance.entity.LoginAttempt;
import com.example.finance.repository.LoginAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RateLimitingService {

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final int TIME_WINDOW_MINUTES = 15;

    /**
     * Record login attempt
     */
    @Transactional
    public void recordLoginAttempt(String username, String ipAddress, String userAgent, boolean success, String failureReason) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(ipAddress);
        attempt.setUserAgent(userAgent);
        attempt.setSuccess(success);
        attempt.setFailureReason(failureReason);
        
        loginAttemptRepository.save(attempt);
    }

    /**
     * Check if account is locked
     */
    public boolean isAccountLocked(String username) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES);
        Long failedAttempts = loginAttemptRepository.countFailedAttemptsSince(username, since);
        
        return failedAttempts >= MAX_FAILED_ATTEMPTS;
    }

    /**
     * Check if IP is blocked
     */
    public boolean isIpBlocked(String ipAddress) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(TIME_WINDOW_MINUTES);
        Long failedAttempts = loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, since);
        
        return failedAttempts >= MAX_FAILED_ATTEMPTS;
    }

    /**
     * Get failed attempts count
     */
    public int getFailedAttemptsCount(String username) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES);
        Long count = loginAttemptRepository.countFailedAttemptsSince(username, since);
        return count.intValue();
    }

    /**
     * Get remaining attempts
     */
    public int getRemainingAttempts(String username) {
        int failedCount = getFailedAttemptsCount(username);
        return Math.max(0, MAX_FAILED_ATTEMPTS - failedCount);
    }

    /**
     * Get lockout time remaining
     */
    public Optional<LocalDateTime> getLockoutExpiryTime(String username) {
        if (!isAccountLocked(username)) {
            return Optional.empty();
        }
        
        List<LoginAttempt> recentAttempts = loginAttemptRepository.findRecentAttemptsByUsername(username);
        
        if (recentAttempts.isEmpty()) {
            return Optional.empty();
        }
        
        // Find the first failed attempt within lockout window
        LocalDateTime lockoutStart = recentAttempts.stream()
            .filter(attempt -> !attempt.getSuccess())
            .filter(attempt -> attempt.getCreatedAt().isAfter(
                LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES)))
            .map(LoginAttempt::getCreatedAt)
            .min(Comparator.naturalOrder())
            .orElse(null);
        
        if (lockoutStart == null) {
            return Optional.empty();
        }
        
        return Optional.of(lockoutStart.plusMinutes(LOCKOUT_DURATION_MINUTES));
    }

    /**
     * Get login attempt statistics
     */
    public Map<String, Object> getLoginStatistics(String username) {
        List<LoginAttempt> attempts = loginAttemptRepository.findRecentAttemptsByUsername(username);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAttempts", attempts.size());
        
        long successCount = attempts.stream().filter(LoginAttempt::getSuccess).count();
        long failureCount = attempts.size() - successCount;
        
        stats.put("successfulAttempts", successCount);
        stats.put("failedAttempts", failureCount);
        stats.put("isLocked", isAccountLocked(username));
        stats.put("remainingAttempts", getRemainingAttempts(username));
        
        getLockoutExpiryTime(username).ifPresent(expiry -> 
            stats.put("lockoutExpiresAt", expiry));
        
        return stats;
    }
}

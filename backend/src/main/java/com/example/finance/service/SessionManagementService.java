package com.example.finance.service;

import com.example.finance.entity.UserSession;
import com.example.finance.entity.User;
import com.example.finance.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SessionManagementService {

    @Autowired
    private UserSessionRepository sessionRepository;

    private static final int SESSION_DURATION_HOURS = 24;
    private static final int REFRESH_TOKEN_DURATION_DAYS = 30;
    private static final int MAX_ACTIVE_SESSIONS = 5;

    /**
     * Create new session
     */
    @Transactional
    public UserSession createSession(User user, String ipAddress, String userAgent) {
        // Check and limit active sessions
        List<UserSession> activeSessions = sessionRepository.findActiveSessionsByUserId(
            user.getId(), LocalDateTime.now());
        
        if (activeSessions.size() >= MAX_ACTIVE_SESSIONS) {
            // Revoke oldest session
            activeSessions.stream()
                .sorted(Comparator.comparing(UserSession::getLastActivity))
                .findFirst()
                .ifPresent(session -> revokeSession(session.getSessionToken()));
        }

        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionToken(generateToken());
        session.setRefreshToken(generateToken());
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setDeviceInfo(parseDeviceInfo(userAgent));
        session.setActive(true);
        session.setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));
        
        return sessionRepository.save(session);
    }

    /**
     * Generate secure random token
     */
    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Parse device info from user agent
     */
    private String parseDeviceInfo(String userAgent) {
        if (userAgent == null) return "Unknown Device";
        
        if (userAgent.contains("Windows")) return "Windows PC";
        if (userAgent.contains("Mac")) return "Mac";
        if (userAgent.contains("Linux")) return "Linux PC";
        if (userAgent.contains("Android")) return "Android Device";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) return "iOS Device";
        
        return "Unknown Device";
    }

    /**
     * Validate session
     */
    public Optional<UserSession> validateSession(String sessionToken) {
        Optional<UserSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        UserSession session = sessionOpt.get();
        
        // Check if session is active and not expired
        if (!session.getActive() || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        
        // Update last activity
        session.setLastActivity(LocalDateTime.now());
        sessionRepository.save(session);
        
        return Optional.of(session);
    }

    /**
     * Refresh session using refresh token
     */
    @Transactional
    public Optional<UserSession> refreshSession(String refreshToken) {
        Optional<UserSession> sessionOpt = sessionRepository.findByRefreshToken(refreshToken);
        
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        UserSession session = sessionOpt.get();
        
        // Check if refresh token is still valid
        if (!session.getActive() || session.getExpiresAt()
                .plusDays(REFRESH_TOKEN_DURATION_DAYS)
                .isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        
        // Generate new session token and extend expiry
        session.setSessionToken(generateToken());
        session.setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));
        session.setLastActivity(LocalDateTime.now());
        
        return Optional.of(sessionRepository.save(session));
    }

    /**
     * Revoke session
     */
    @Transactional
    public void revokeSession(String sessionToken) {
        sessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            session.setActive(false);
            session.setRevokedAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    /**
     * Revoke all user sessions
     */
    @Transactional
    public void revokeAllUserSessions(Long userId) {
        List<UserSession> sessions = sessionRepository.findByUserIdAndActiveTrue(userId);
        LocalDateTime now = LocalDateTime.now();
        
        sessions.forEach(session -> {
            session.setActive(false);
            session.setRevokedAt(now);
        });
        
        sessionRepository.saveAll(sessions);
    }

    /**
     * Get user's active sessions
     */
    public List<Map<String, Object>> getActiveSessions(Long userId) {
        List<UserSession> sessions = sessionRepository.findActiveSessionsByUserId(
            userId, LocalDateTime.now());
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (UserSession session : sessions) {
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("id", session.getId());
            sessionInfo.put("deviceInfo", session.getDeviceInfo());
            sessionInfo.put("ipAddress", session.getIpAddress());
            sessionInfo.put("lastActivity", session.getLastActivity());
            sessionInfo.put("createdAt", session.getCreatedAt());
            sessionInfo.put("expiresAt", session.getExpiresAt());
            sessionInfo.put("isCurrent", false); // Will be set by controller
            
            result.add(sessionInfo);
        }
        
        return result;
    }

    /**
     * Clean up expired sessions (scheduled task)
     */
    @Transactional
    public void cleanupExpiredSessions() {
        List<UserSession> expiredSessions = sessionRepository.findExpiredSessions(LocalDateTime.now());
        
        expiredSessions.forEach(session -> {
            session.setActive(false);
            session.setRevokedAt(LocalDateTime.now());
        });
        
        sessionRepository.saveAll(expiredSessions);
    }

    /**
     * Get session statistics
     */
    public Map<String, Object> getSessionStatistics(Long userId) {
        List<UserSession> allSessions = sessionRepository.findByUserId(userId);
        List<UserSession> activeSessions = sessionRepository.findActiveSessionsByUserId(
            userId, LocalDateTime.now());
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", allSessions.size());
        stats.put("activeSessions", activeSessions.size());
        stats.put("revokedSessions", allSessions.size() - activeSessions.size());
        
        // Group by device
        Map<String, Long> deviceCounts = new HashMap<>();
        for (UserSession session : allSessions) {
            String device = session.getDeviceInfo();
            deviceCounts.put(device, deviceCounts.getOrDefault(device, 0L) + 1);
        }
        stats.put("deviceBreakdown", deviceCounts);
        
        return stats;
    }
}

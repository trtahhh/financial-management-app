package com.example.finance.repository;

import com.example.finance.entity.UserSession;
import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    Optional<UserSession> findByRefreshToken(String refreshToken);
    
    List<UserSession> findByUserIdAndActiveTrue(Long userId);
    
    List<UserSession> findByUserId(Long userId);
    
    @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId AND s.active = true AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now AND s.active = true")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);
    
    void deleteByUserIdAndActiveTrue(Long userId);
}

package com.example.finance.repository;

import com.example.finance.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    
    List<LoginAttempt> findByUsername(String username);
    
    List<LoginAttempt> findByIpAddress(String ipAddress);
    
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.username = :username AND l.success = false AND l.createdAt > :since")
    Long countFailedAttemptsSince(@Param("username") String username, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.ipAddress = :ipAddress AND l.success = false AND l.createdAt > :since")
    Long countFailedAttemptsByIpSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    @Query("SELECT l FROM LoginAttempt l WHERE l.username = :username ORDER BY l.createdAt DESC")
    List<LoginAttempt> findRecentAttemptsByUsername(@Param("username") String username);
}

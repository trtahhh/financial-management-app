package com.example.finance.repository;

import com.example.finance.entity.UserGamification;
import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserGamificationRepository extends JpaRepository<UserGamification, Long> {
    
    Optional<UserGamification> findByUser(User user);
    
    Optional<UserGamification> findByUserId(Long userId);
    
    @Query("SELECT ug FROM UserGamification ug ORDER BY ug.totalPoints DESC")
    List<UserGamification> findTopByOrderByTotalPointsDesc();
    
    @Query("SELECT ug FROM UserGamification ug WHERE ug.level = :level ORDER BY ug.totalPoints DESC")
    List<UserGamification> findByLevel(Integer level);
    
    @Query("SELECT ug FROM UserGamification ug ORDER BY ug.currentStreak DESC")
    List<UserGamification> findTopByOrderByCurrentStreakDesc();
}

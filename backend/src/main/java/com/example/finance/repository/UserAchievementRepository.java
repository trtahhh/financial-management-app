package com.example.finance.repository;

import com.example.finance.entity.UserAchievement;
import com.example.finance.entity.User;
import com.example.finance.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    
    List<UserAchievement> findByUser(User user);
    
    List<UserAchievement> findByUserId(Long userId);
    
    Optional<UserAchievement> findByUserAndAchievement(User user, Achievement achievement);
    
    List<UserAchievement> findByUserIdAndIsNotifiedFalse(Long userId);
    
    Long countByUserId(Long userId);
}

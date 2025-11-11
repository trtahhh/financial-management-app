package com.example.finance.repository;

import com.example.finance.entity.UserChallenge;
import com.example.finance.entity.User;
import com.example.finance.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {
    
    List<UserChallenge> findByUser(User user);
    
    List<UserChallenge> findByUserId(Long userId);
    
    Optional<UserChallenge> findByUserAndChallenge(User user, Challenge challenge);
    
    List<UserChallenge> findByUserIdAndIsCompletedFalse(Long userId);
    
    List<UserChallenge> findByUserIdAndIsCompletedTrue(Long userId);
    
    @Query("SELECT uc FROM UserChallenge uc WHERE uc.user.id = :userId AND uc.challenge.isActive = true AND uc.isCompleted = false")
    List<UserChallenge> findActiveUserChallenges(Long userId);
    
    Long countByUserIdAndIsCompletedTrue(Long userId);
}

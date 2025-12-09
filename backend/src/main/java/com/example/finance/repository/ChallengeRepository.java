package com.example.finance.repository;

import com.example.finance.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    
    // Challenge entity doesn't have 'code' field
    // Optional<Challenge> findByCode(String code);
    
    List<Challenge> findByType(String type);
    
    List<Challenge> findByCategory(String category);
    
    List<Challenge> findByIsActiveTrue();
    
    @Query("SELECT c FROM Challenge c WHERE c.isActive = true AND c.startDate <= :now AND c.endDate >= :now")
    List<Challenge> findActiveChallenges(LocalDateTime now);
    
    @Query("SELECT c FROM Challenge c WHERE c.isActive = true AND c.type = :type AND c.startDate <= :now AND c.endDate >= :now")
    List<Challenge> findActiveChallengesByType(String type, LocalDateTime now);
}

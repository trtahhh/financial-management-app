package com.example.finance.repository;

import com.example.finance.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    
    Optional<Achievement> findByCode(String code);
    
    List<Achievement> findByCategory(String category);
    
    List<Achievement> findByIsActiveTrue();
    
    List<Achievement> findByTier(String tier);
}

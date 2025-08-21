package com.example.finance.repository;

import com.example.finance.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserId(Long userId);
    List<Goal> findByUserIdAndIsDeletedFalse(Long userId);
    
    /**
     * Tìm mục tiêu theo user và trạng thái
     */
    List<Goal> findByUserIdAndStatusAndIsDeletedFalse(Long userId, String status);
    
    /**
     * Đếm số mục tiêu theo user và trạng thái
     */
    Long countByUserIdAndStatusAndIsDeletedFalse(Long userId, String status);
}

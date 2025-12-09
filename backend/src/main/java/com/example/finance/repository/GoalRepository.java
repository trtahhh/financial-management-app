package com.example.finance.repository;

import com.example.finance.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
 
 Long countByUserIdAndStatus(Long userId, String status);
 
 /**
 * Tìm mục tiêu theo user và trạng thái không nằm trong danh sách
 */
 @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.status NOT IN :statuses AND g.isDeleted = false")
 List<Goal> findByUserIdAndStatusNotInAndIsDeletedFalse(@Param("userId") Long userId, @Param("statuses") List<String> statuses);
}

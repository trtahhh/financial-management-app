package com.example.finance.repository;

import com.example.finance.entity.Budget;
import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
        // Find all active budgets for a user
    List<Budget> findByUserAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(User user, String status);
    
    // Find budget by user and name
    Optional<Budget> findByUserAndNameAndIsDeletedFalse(User user, String name);
    
    // Find active budgets for a user
    List<Budget> findByUserAndStatusAndIsDeletedFalse(User user, String status);
    
    // Find budgets by date range
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.startDate <= :currentDate AND b.endDate >= :currentDate AND b.status = 'active' AND b.isDeleted = false")
    List<Budget> findActiveBudgetsByUserAndCurrentDate(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    // Find budgets that are expired
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.endDate < :currentDate AND b.status = 'active' AND b.isDeleted = false")
    List<Budget> findExpiredBudgets(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    // Find budgets that are upcoming
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.startDate > :currentDate AND b.status = 'active' AND b.isDeleted = false")
    List<Budget> findUpcomingBudgets(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    // Get total budget amount for a user
    @Query("SELECT COALESCE(SUM(b.total), 0) FROM Budget b WHERE b.user = :user AND b.status = 'active' AND b.isDeleted = false")
    BigDecimal getTotalBudgetAmountByUser(@Param("user") User user);
    
    // Get budget summary by status
    @Query("SELECT b.status, COUNT(b), COALESCE(SUM(b.total), 0) FROM Budget b WHERE b.user = :user AND b.isDeleted = false GROUP BY b.status")
    List<Object[]> getBudgetSummaryByStatus(@Param("user") User user);
    
    // Find budgets by date range
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.startDate >= :startDate AND b.endDate <= :endDate AND b.status = 'active' AND b.isDeleted = false")
    List<Budget> findByUserAndDateRange(@Param("user") User user, 
                                       @Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);
    
    // Get budget progress for current month
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.startDate <= :currentDate AND b.endDate >= :currentDate AND b.status = 'active' AND b.isDeleted = false")
    List<Budget> getCurrentMonthBudgets(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    // Get budget progress for current year
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND YEAR(b.startDate) = YEAR(:currentDate) AND b.status = 'active' AND b.isDeleted = false")
    List<Budget> getCurrentYearBudgets(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    // Count active budgets for a user
    @Query("SELECT COUNT(b) FROM Budget b WHERE b.user = :user AND b.status = 'active' AND b.isDeleted = false")
    Long countActiveBudgetsByUser(@Param("user") User user);
    
    // Find budgets that need renewal (ending soon)
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.endDate <= :endDate AND b.status = 'active' AND b.isDeleted = false")
    List<Budget> findBudgetsEndingSoon(@Param("user") User user, @Param("endDate") LocalDate endDate);
    
    // Get budget statistics
    @Query("SELECT b.status, COUNT(b), COALESCE(SUM(b.total), 0) FROM Budget b WHERE b.user = :user AND b.isDeleted = false GROUP BY b.status")
    List<Object[]> getBudgetStatisticsByStatus(@Param("user") User user);
    
    // Find all budgets for a user (not deleted)
    List<Budget> findByUserAndIsDeletedFalseOrderByCreatedAtDesc(User user);
    
    // Delete all budgets for a user
    void deleteByUser(User user);
} 
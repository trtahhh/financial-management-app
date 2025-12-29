package com.example.finance.repository;

import com.example.finance.entity.FinancialHealthHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialHealthHistoryRepository extends JpaRepository<FinancialHealthHistory, Long> {
    
    /**
     * Tìm lịch sử điểm theo user và tháng/năm
     */
    Optional<FinancialHealthHistory> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);
    
    /**
     * Lấy lịch sử điểm của user theo thời gian (mới nhất trước)
     */
    List<FinancialHealthHistory> findByUserIdOrderByYearDescMonthDesc(Long userId);
    
    /**
     * Lấy N tháng gần nhất
     */
    @Query("SELECT h FROM FinancialHealthHistory h WHERE h.userId = :userId " +
           "ORDER BY h.year DESC, h.month DESC")
    List<FinancialHealthHistory> findRecentHistory(Long userId, int limit);
    
    /**
     * Lấy điểm tháng trước
     */
    @Query("SELECT h FROM FinancialHealthHistory h WHERE h.userId = :userId " +
           "AND h.year = :year AND h.month = :month")
    Optional<FinancialHealthHistory> findPreviousMonthScore(Long userId, Integer year, Integer month);
}

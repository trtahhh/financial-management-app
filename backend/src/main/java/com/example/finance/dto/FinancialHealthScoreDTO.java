package com.example.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO cho Điểm Sức khỏe Tài chính
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialHealthScoreDTO {
    
    // Tổng điểm (0-100)
    private Integer totalScore;
    
    // Xếp hạng: EXCELLENT, GOOD, FAIR, POOR, CRITICAL
    private String rating;
    
    // Màu sắc tương ứng với xếp hạng
    private String ratingColor;
    
    // Chi tiết điểm theo từng tiêu chí
    private ScoreBreakdown breakdown;
    
    // Xu hướng so với tháng trước
    private String trend; // UP, DOWN, STABLE
    
    // % thay đổi so với tháng trước
    private Double trendPercentage;
    
    // Thời gian tính toán
    private LocalDateTime calculatedAt;
    
    // Gợi ý cải thiện
    private List<Recommendation> recommendations;
    
    // Thông tin tóm tắt
    private FinancialSummary summary;
    
    /**
     * Chi tiết điểm số theo từng tiêu chí
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdown {
        // Tỷ lệ tiết kiệm (25 điểm)
        private CategoryScore savingsRate;
        
        // Quản lý ngân sách (25 điểm)
        private CategoryScore budgetManagement;
        
        // Nợ & Tín dụng (20 điểm)
        private CategoryScore debtCredit;
        
        // Đa dạng thu nhập (15 điểm)
        private CategoryScore incomeDiversity;
        
        // Thói quen giao dịch (15 điểm)
        private CategoryScore transactionHabits;
    }
    
    /**
     * Điểm số cho mỗi tiêu chí
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryScore {
        private String name;
        private Integer score;
        private Integer maxScore;
        private String status; // EXCELLENT, GOOD, NEEDS_IMPROVEMENT, POOR
        private String description;
        private List<String> details;
    }
    
    /**
     * Gợi ý cải thiện
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String category;
        private String priority; // HIGH, MEDIUM, LOW
        private String title;
        private String description;
        private String actionable;
        private Integer potentialScoreGain;
    }
    
    /**
     * Tóm tắt tình hình tài chính
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialSummary {
        // Thu nhập tháng này
        private BigDecimal monthlyIncome;
        
        // Chi tiêu tháng này
        private BigDecimal monthlyExpense;
        
        // Tiết kiệm tháng này
        private BigDecimal monthlySavings;
        
        // Tỷ lệ tiết kiệm (%)
        private Double savingsRate;
        
        // Số ngân sách đang theo dõi
        private Integer activeBudgets;
        
        // Số ngân sách vượt mức
        private Integer overBudgetCount;
        
        // Số mục tiêu đang có
        private Integer activeGoals;
        
        // Số giao dịch trong tháng
        private Integer transactionCount;
        
        // Số nguồn thu nhập
        private Integer incomeSourceCount;
    }
}

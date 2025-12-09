package com.example.finance.service;

import com.example.finance.dto.FinancialHealthScoreDTO;
import com.example.finance.dto.FinancialHealthScoreDTO.*;
import com.example.finance.entity.*;
import com.example.finance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service tính toán Điểm Sức khỏe Tài chính
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FinancialHealthService {
    
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final CategoryRepository categoryRepository;
    
    /**
     * Tính điểm sức khỏe tài chính cho user
     */
    public FinancialHealthScoreDTO calculateHealthScore(Long userId) {
        log.info("Calculating financial health score for user: {}", userId);
        
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        
        // Lấy dữ liệu cần thiết
        List<Transaction> monthlyTransactions = transactionRepository
            .findByUserIdAndDateBetweenOrderByDateDesc(userId, startOfMonth, endOfMonth);
        
        List<Budget> activeBudgets = budgetRepository.findByUserIdAndIsDeletedFalse(userId);
        List<Goal> activeGoals = goalRepository.findByUserId(userId);
        
        // Tính toán summary
        FinancialSummary summary = calculateSummary(monthlyTransactions, activeBudgets, activeGoals);
        
        // Tính điểm từng tiêu chí
        ScoreBreakdown breakdown = new ScoreBreakdown();
        breakdown.setSavingsRate(calculateSavingsScore(summary));
        breakdown.setBudgetManagement(calculateBudgetScore(monthlyTransactions, activeBudgets));
        breakdown.setDebtCredit(calculateDebtScore(monthlyTransactions));
        breakdown.setIncomeDiversity(calculateIncomeDiversityScore(monthlyTransactions));
        breakdown.setTransactionHabits(calculateHabitsScore(monthlyTransactions, activeGoals));
        
        // Tổng điểm
        int totalScore = breakdown.getSavingsRate().getScore() +
                        breakdown.getBudgetManagement().getScore() +
                        breakdown.getDebtCredit().getScore() +
                        breakdown.getIncomeDiversity().getScore() +
                        breakdown.getTransactionHabits().getScore();
        
        // Xếp hạng
        RatingInfo ratingInfo = getRatingInfo(totalScore);
        
        // Gợi ý cải thiện
        List<Recommendation> recommendations = generateRecommendations(breakdown, summary);
        
        // Xu hướng (so với tháng trước)
        TrendInfo trendInfo = calculateTrend(userId, totalScore);
        
        FinancialHealthScoreDTO result = new FinancialHealthScoreDTO();
        result.setTotalScore(totalScore);
        result.setRating(ratingInfo.getRating());
        result.setRatingColor(ratingInfo.getColor());
        result.setBreakdown(breakdown);
        result.setTrend(trendInfo.getTrend());
        result.setTrendPercentage(trendInfo.getPercentage());
        result.setCalculatedAt(LocalDateTime.now());
        result.setRecommendations(recommendations);
        result.setSummary(summary);
        
        log.info("Health score calculated: {} ({}) for user: {}", totalScore, ratingInfo.getRating(), userId);
        
        return result;
    }
    
    /**
     * Tính tóm tắt tài chính
     */
    private FinancialSummary calculateSummary(List<Transaction> transactions, 
                                             List<Budget> budgets, 
                                             List<Goal> goals) {
        FinancialSummary summary = new FinancialSummary();
        
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        
        for (Transaction t : transactions) {
            String type = t.getType();
            if ("INCOME".equalsIgnoreCase(type) || "THU".equalsIgnoreCase(type) || "income".equalsIgnoreCase(type)) {
                income = income.add(t.getAmount());
            } else if ("EXPENSE".equalsIgnoreCase(type) || "CHI".equalsIgnoreCase(type) || "expense".equalsIgnoreCase(type)) {
                expense = expense.add(t.getAmount());
            }
        }
        
        BigDecimal savings = income.subtract(expense);
        Double savingsRate = income.compareTo(BigDecimal.ZERO) > 0 
            ? savings.divide(income, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).doubleValue()
            : 0.0;
        
        summary.setMonthlyIncome(income);
        summary.setMonthlyExpense(expense);
        summary.setMonthlySavings(savings);
        summary.setSavingsRate(savingsRate);
        summary.setActiveBudgets(budgets.size());
        summary.setActiveGoals(goals.size());
        summary.setTransactionCount(transactions.size());
        
        // Đếm số nguồn thu nhập khác nhau
        long incomeSourceCount = transactions.stream()
            .filter(t -> {
                String type = t.getType();
                return "INCOME".equalsIgnoreCase(type) || "THU".equalsIgnoreCase(type) || "income".equalsIgnoreCase(type);
            })
            .filter(t -> t.getCategory() != null)
            .map(t -> t.getCategory().getId())
            .distinct()
            .count();
        summary.setIncomeSourceCount((int) incomeSourceCount);
        
        // Đếm số ngân sách vượt mức
        int overBudgetCount = 0;
        for (Budget budget : budgets) {
            BigDecimal spent = transactions.stream()
                .filter(t -> {
                    String type = t.getType();
                    return "EXPENSE".equalsIgnoreCase(type) || "CHI".equalsIgnoreCase(type) || "expense".equalsIgnoreCase(type);
                })
                .filter(t -> budget.getCategory() != null && t.getCategory() != null 
                    && budget.getCategory().getId().equals(t.getCategory().getId()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (spent.compareTo(budget.getAmount()) > 0) {
                overBudgetCount++;
            }
        }
        summary.setOverBudgetCount(overBudgetCount);
        
        return summary;
    }
    
    /**
     * Điểm Tỷ lệ Tiết kiệm (25 điểm)
     */
    private CategoryScore calculateSavingsScore(FinancialSummary summary) {
        CategoryScore score = new CategoryScore();
        score.setName("Tỷ lệ Tiết kiệm");
        score.setMaxScore(25);
        
        Double savingsRate = summary.getSavingsRate();
        List<String> details = new ArrayList<>();
        
        if (savingsRate >= 20.0) {
            score.setScore(25);
            score.setStatus("EXCELLENT");
            score.setDescription("Xuất sắc! Bạn đang tiết kiệm rất tốt");
            details.add(String.format("Tỷ lệ tiết kiệm: %.1f%%", savingsRate));
            details.add("Mục tiêu khuyến nghị: >= 20%");
        } else if (savingsRate >= 10.0) {
            score.setScore((int) (15 + (savingsRate - 10) * 1.0));
            score.setStatus("GOOD");
            score.setDescription("Tốt! Bạn đang tiết kiệm được");
            details.add(String.format("Tỷ lệ tiết kiệm: %.1f%%", savingsRate));
            details.add(String.format("Cần tăng thêm %.1f%% để đạt mức xuất sắc", 20.0 - savingsRate));
        } else if (savingsRate >= 0) {
            score.setScore((int) (savingsRate * 1.5));
            score.setStatus("NEEDS_IMPROVEMENT");
            score.setDescription("Cần cải thiện! Hãy cố gắng tiết kiệm nhiều hơn");
            details.add(String.format("Tỷ lệ tiết kiệm: %.1f%%", savingsRate));
            details.add("Khuyến nghị: Cắt giảm chi tiêu không cần thiết");
        } else {
            score.setScore(0);
            score.setStatus("POOR");
            score.setDescription("Chi tiêu vượt thu nhập! Cần hành động ngay");
            details.add("Đang chi tiêu quá mức");
            details.add(String.format("Thiếu hụt: %s", summary.getMonthlySavings().abs()));
        }
        
        score.setDetails(details);
        return score;
    }
    
    /**
     * Điểm Quản lý Ngân sách (25 điểm)
     */
    private CategoryScore calculateBudgetScore(List<Transaction> transactions, List<Budget> budgets) {
        CategoryScore score = new CategoryScore();
        score.setName("Quản lý Ngân sách");
        score.setMaxScore(25);
        List<String> details = new ArrayList<>();
        
        if (budgets.isEmpty()) {
            score.setScore(5);
            score.setStatus("POOR");
            score.setDescription("Chưa thiết lập ngân sách");
            details.add("Hãy tạo ngân sách để quản lý chi tiêu tốt hơn");
        } else {
            int totalBudgets = budgets.size();
            int withinBudget = 0;
            int overBudget = 0;
            
            for (Budget budget : budgets) {
                BigDecimal spent = transactions.stream()
                    .filter(t -> {
                        String type = t.getType();
                        return "EXPENSE".equalsIgnoreCase(type) || "CHI".equalsIgnoreCase(type) || "expense".equalsIgnoreCase(type);
                    })
                    .filter(t -> budget.getCategory() != null && t.getCategory() != null
                        && budget.getCategory().getId().equals(t.getCategory().getId()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (spent.compareTo(budget.getAmount()) <= 0) {
                    withinBudget++;
                } else {
                    overBudget++;
                }
            }
            
            double compliance = (double) withinBudget / totalBudgets * 100;
            
            if (compliance >= 90) {
                score.setScore(25);
                score.setStatus("EXCELLENT");
                score.setDescription("Xuất sắc! Tuân thủ ngân sách rất tốt");
            } else if (compliance >= 70) {
                score.setScore(20);
                score.setStatus("GOOD");
                score.setDescription("Tốt! Quản lý ngân sách ổn định");
            } else if (compliance >= 50) {
                score.setScore(15);
                score.setStatus("NEEDS_IMPROVEMENT");
                score.setDescription("Cần cải thiện! Thường xuyên vượt ngân sách");
            } else {
                score.setScore(10);
                score.setStatus("POOR");
                score.setDescription("Kém! Quản lý ngân sách yếu");
            }
            
            details.add(String.format("Tổng số ngân sách: %d", totalBudgets));
            details.add(String.format("Tuân thủ: %d/%d (%.0f%%)", withinBudget, totalBudgets, compliance));
            if (overBudget > 0) {
                details.add(String.format("Vượt ngân sách: %d", overBudget));
            }
        }
        
        score.setDetails(details);
        return score;
    }
    
    /**
     * Điểm Nợ & Tín dụng (20 điểm)
     */
    private CategoryScore calculateDebtScore(List<Transaction> transactions) {
        CategoryScore score = new CategoryScore();
        score.setName("Nợ & Tín dụng");
        score.setMaxScore(20);
        List<String> details = new ArrayList<>();
        
        // Đếm giao dịch liên quan đến nợ/vay
        long debtTransactions = transactions.stream()
            .filter(t -> t.getNote() != null)
            .filter(t -> {
                String desc = t.getNote().toLowerCase();
                return desc.contains("nợ") || desc.contains("vay") || 
                       desc.contains("debt") || desc.contains("loan") ||
                       desc.contains("credit card");
            })
            .count();
        
        if (debtTransactions == 0) {
            score.setScore(20);
            score.setStatus("EXCELLENT");
            score.setDescription("Tuyệt vời! Không có khoản nợ");
            details.add("Không phát hiện giao dịch nợ");
            details.add("Tiếp tục duy trì tình trạng không nợ");
        } else if (debtTransactions <= 2) {
            score.setScore(15);
            score.setStatus("GOOD");
            score.setDescription("Có một số khoản vay nhỏ");
            details.add(String.format("Phát hiện %d giao dịch liên quan nợ", debtTransactions));
            details.add("Hãy ưu tiên trả nợ sớm");
        } else {
            score.setScore(10);
            score.setStatus("NEEDS_IMPROVEMENT");
            score.setDescription("Nhiều khoản nợ, cần giảm bớt");
            details.add(String.format("Phát hiện %d giao dịch liên quan nợ", debtTransactions));
            details.add("Khuyến nghị: Lên kế hoạch trả nợ");
        }
        
        score.setDetails(details);
        return score;
    }
    
    /**
     * Điểm Đa dạng Thu nhập (15 điểm)
     */
    private CategoryScore calculateIncomeDiversityScore(List<Transaction> transactions) {
        CategoryScore score = new CategoryScore();
        score.setName("Đa dạng Thu nhập");
        score.setMaxScore(15);
        List<String> details = new ArrayList<>();
        
        long incomeSourceCount = transactions.stream()
            .filter(t -> {
                String type = t.getType();
                return "INCOME".equalsIgnoreCase(type) || "THU".equalsIgnoreCase(type) || "income".equalsIgnoreCase(type);
            })
            .filter(t -> t.getCategory() != null)
            .map(t -> t.getCategory().getId())
            .distinct()
            .count();
        
        if (incomeSourceCount >= 3) {
            score.setScore(15);
            score.setStatus("EXCELLENT");
            score.setDescription("Tuyệt vời! Nhiều nguồn thu nhập");
            details.add(String.format("Có %d nguồn thu khác nhau", incomeSourceCount));
            details.add("Danh mục thu nhập đa dạng, rủi ro thấp");
        } else if (incomeSourceCount == 2) {
            score.setScore(10);
            score.setStatus("GOOD");
            score.setDescription("Tốt! Có 2 nguồn thu nhập");
            details.add("Có 2 nguồn thu nhập");
            details.add("Khuyến nghị: Tìm thêm nguồn thu phụ");
        } else if (incomeSourceCount == 1) {
            score.setScore(5);
            score.setStatus("NEEDS_IMPROVEMENT");
            score.setDescription("Chỉ có 1 nguồn thu, cần đa dạng hóa");
            details.add("Chỉ phụ thuộc vào 1 nguồn thu");
            details.add("Rủi ro cao nếu mất nguồn thu chính");
        } else {
            score.setScore(0);
            score.setStatus("POOR");
            score.setDescription("Chưa có thu nhập được ghi nhận");
            details.add("Chưa ghi nhận giao dịch thu nhập");
        }
        
        score.setDetails(details);
        return score;
    }
    
    /**
     * Điểm Thói quen Giao dịch (15 điểm)
     */
    private CategoryScore calculateHabitsScore(List<Transaction> transactions, List<Goal> goals) {
        CategoryScore score = new CategoryScore();
        score.setName("Thói quen Giao dịch");
        score.setMaxScore(15);
        List<String> details = new ArrayList<>();
        
        int baseScore = 0;
        
        // Tần suất ghi chép (10 điểm)
        int transactionCount = transactions.size();
        if (transactionCount >= 20) {
            baseScore += 10;
            details.add("Ghi chép thường xuyên (20+ giao dịch)");
        } else if (transactionCount >= 10) {
            baseScore += 7;
            details.add("Ghi chép khá đều (10-19 giao dịch)");
        } else if (transactionCount >= 5) {
            baseScore += 4;
            details.add("Ghi chép ít (5-9 giao dịch)");
        } else {
            details.add("Ghi chép rất ít (< 5 giao dịch)");
        }
        
        // Có mục tiêu tài chính (5 điểm)
        if (!goals.isEmpty()) {
            baseScore += 5;
            details.add(String.format("Có %d mục tiêu tài chính", goals.size()));
        } else {
            details.add("Chưa có mục tiêu tài chính");
        }
        
        score.setScore(baseScore);
        
        if (baseScore >= 12) {
            score.setStatus("EXCELLENT");
            score.setDescription("Tuyệt vời! Thói quen tài chính tốt");
        } else if (baseScore >= 8) {
            score.setStatus("GOOD");
            score.setDescription("Tốt! Đang xây dựng thói quen");
        } else if (baseScore >= 4) {
            score.setStatus("NEEDS_IMPROVEMENT");
            score.setDescription("Cần cải thiện thói quen ghi chép");
        } else {
            score.setStatus("POOR");
            score.setDescription("Thói quen quản lý yếu");
        }
        
        score.setDetails(details);
        return score;
    }
    
    /**
     * Lấy thông tin xếp hạng
     */
    private RatingInfo getRatingInfo(int score) {
        if (score >= 85) {
            return new RatingInfo("EXCELLENT", "#10b981", "Xuất sắc");
        } else if (score >= 70) {
            return new RatingInfo("GOOD", "#3b82f6", "Tốt");
        } else if (score >= 50) {
            return new RatingInfo("FAIR", "#f59e0b", "Trung bình");
        } else if (score >= 30) {
            return new RatingInfo("POOR", "#ef4444", "Kém");
        } else {
            return new RatingInfo("CRITICAL", "#dc2626", "Nguy hiểm");
        }
    }
    
    /**
     * Tạo gợi ý cải thiện
     */
    private List<Recommendation> generateRecommendations(ScoreBreakdown breakdown, FinancialSummary summary) {
        List<Recommendation> recommendations = new ArrayList<>();
        
        // Gợi ý cho Tiết kiệm
        if (breakdown.getSavingsRate().getScore() < 15) {
            recommendations.add(new Recommendation(
                "Tiết kiệm",
                "HIGH",
                "Tăng tỷ lệ tiết kiệm",
                "Tỷ lệ tiết kiệm của bạn đang thấp. Hãy cố gắng tiết kiệm ít nhất 10-20% thu nhập.",
                "Áp dụng quy tắc 50/30/20: 50% nhu cầu thiết yếu, 30% mong muốn, 20% tiết kiệm",
                10
            ));
        }
        
        // Gợi ý cho Ngân sách
        if (breakdown.getBudgetManagement().getScore() < 15) {
            if (summary.getActiveBudgets() == 0) {
                recommendations.add(new Recommendation(
                    "Ngân sách",
                    "HIGH",
                    "Thiết lập ngân sách",
                    "Bạn chưa có ngân sách nào. Hãy tạo ngân sách cho các danh mục chi tiêu chính.",
                    "Bắt đầu với 3-5 ngân sách cơ bản: Ăn uống, Di chuyển, Giải trí, Mua sắm",
                    15
                ));
            } else if (summary.getOverBudgetCount() > 0) {
                recommendations.add(new Recommendation(
                    "Ngân sách",
                    "MEDIUM",
                    "Kiểm soát chi tiêu",
                    String.format("Bạn đang vượt %d ngân sách. Hãy xem xét điều chỉnh hoặc cắt giảm chi tiêu.", 
                                summary.getOverBudgetCount()),
                    "Xem lại các khoản chi vượt mức và tìm cách tiết kiệm",
                    10
                ));
            }
        }
        
        // Gợi ý cho Thu nhập
        if (breakdown.getIncomeDiversity().getScore() < 10) {
            recommendations.add(new Recommendation(
                "Thu nhập",
                "MEDIUM",
                "Đa dạng hóa thu nhập",
                "Bạn đang phụ thuộc vào ít nguồn thu. Hãy cân nhắc tìm thêm thu nhập phụ.",
                "Tìm kiếm công việc thêm, freelance, đầu tư, hoặc kinh doanh nhỏ",
                10
            ));
        }
        
        // Gợi ý cho Thói quen
        if (breakdown.getTransactionHabits().getScore() < 8) {
            recommendations.add(new Recommendation(
                "Thói quen",
                "LOW",
                "Ghi chép thường xuyên hơn",
                "Hãy ghi lại mọi giao dịch để theo dõi tài chính tốt hơn.",
                "Tạo thói quen ghi chép ngay sau mỗi giao dịch, mục tiêu: 20+ giao dịch/tháng",
                5
            ));
        }
        
        // Gợi ý cho Mục tiêu
        if (summary.getActiveGoals() == 0) {
            recommendations.add(new Recommendation(
                "Mục tiêu",
                "MEDIUM",
                "Đặt mục tiêu tài chính",
                "Bạn chưa có mục tiêu tài chính nào. Hãy đặt mục tiêu để có động lực tiết kiệm.",
                "Bắt đầu với mục tiêu nhỏ: Quỹ khẩn cấp = 3-6 tháng chi tiêu",
                5
            ));
        }
        
        // Sắp xếp theo độ ưu tiên
        recommendations.sort((a, b) -> {
            Map<String, Integer> priorityOrder = Map.of("HIGH", 1, "MEDIUM", 2, "LOW", 3);
            return priorityOrder.get(a.getPriority()).compareTo(priorityOrder.get(b.getPriority()));
        });
        
        return recommendations;
    }
    
    /**
     * Tính xu hướng so với tháng trước
     */
    private TrendInfo calculateTrend(Long userId, int currentScore) {
        // TODO: Lưu điểm vào database để so sánh
        // Tạm thời trả về STABLE
        return new TrendInfo("STABLE", 0.0);
    }
    
    // Helper classes
    private static class RatingInfo {
        private final String rating;
        private final String color;
        private final String label;
        
        RatingInfo(String rating, String color, String label) {
            this.rating = rating;
            this.color = color;
            this.label = label;
        }
        
        String getRating() { return rating; }
        String getColor() { return color; }
        String getLabel() { return label; }
    }
    
    private static class TrendInfo {
        private final String trend;
        private final Double percentage;
        
        TrendInfo(String trend, Double percentage) {
            this.trend = trend;
            this.percentage = percentage;
        }
        
        String getTrend() { return trend; }
        Double getPercentage() { return percentage; }
    }
}

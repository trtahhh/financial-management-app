package com.example.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.BudgetRepository;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service phát hiện chi tiêu quá đà và đưa ra lời khuyên real-time
 * Giống như MoMo - cảnh báo ngay khi chi tiêu vượt mức
 */
@Service
public class OverspendingDetectionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    /**
     * Phát hiện overspending ngay khi tạo transaction mới
     * Trả về cảnh báo và lời khuyên cụ thể như MoMo
     */
    public OverspendingAlert detectOverspending(Transaction newTransaction) {
        Long userId = newTransaction.getUser().getId();
        Category category = newTransaction.getCategory();
        
        if (category == null || !"expense".equalsIgnoreCase(newTransaction.getType())) {
            return OverspendingAlert.noAlert();
        }
        
        // Lấy budget của category này trong tháng hiện tại
        YearMonth currentMonth = YearMonth.now();
        List<Budget> budgets = budgetRepository.findByUserId(userId).stream()
            .filter(b -> b.getCategory() != null && b.getCategory().getId().equals(category.getId()))
            .filter(b -> isCurrentMonth(b))
            .collect(Collectors.toList());
        
        if (budgets.isEmpty()) {
            return OverspendingAlert.noBudgetAlert(category.getName());
        }
        
        Budget budget = budgets.get(0);
        
        // Tính tổng chi tiêu trong tháng (bao gồm transaction mới)
        List<Transaction> monthTransactions = getMonthTransactions(userId, category.getId());
        BigDecimal totalSpent = monthTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(newTransaction.getAmount());
        
        BigDecimal budgetAmount = budget.getAmount();
        double spentPercentage = totalSpent.divide(budgetAmount, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal(100)).doubleValue();
        
        // Phân tích mức độ overspending
        String severity;
        String alertTitle;
        String alertMessage;
        List<String> recommendations;
        
        if (spentPercentage >= 100) {
            severity = "critical";
            alertTitle = "⚠️ Vượt ngân sách " + category.getName();
            alertMessage = String.format(
                "Bạn đã chi %.0f%% ngân sách tháng này (%.0f₫/%.0f₫). Hãy cân nhắc giảm chi tiêu!",
                spentPercentage,
                totalSpent.doubleValue(),
                budgetAmount.doubleValue()
            );
            recommendations = generateCriticalRecommendations(category.getName(), totalSpent, budgetAmount);
            
        } else if (spentPercentage >= 80) {
            severity = "warning";
            alertTitle = "⚡ Sắp vượt ngân sách " + category.getName();
            alertMessage = String.format(
                "Bạn đã chi %.0f%% ngân sách (%.0f₫/%.0f₫). Còn %.0f₫ cho đến cuối tháng.",
                spentPercentage,
                totalSpent.doubleValue(),
                budgetAmount.doubleValue(),
                budgetAmount.subtract(totalSpent).doubleValue()
            );
            recommendations = generateWarningRecommendations(category.getName(), budgetAmount.subtract(totalSpent));
            
        } else if (spentPercentage >= 60) {
            severity = "info";
            alertTitle = "💡 Đang tiêu dùng hợp lý";
            alertMessage = String.format(
                "Bạn đã chi %.0f%% ngân sách. Tiếp tục duy trì nhé!",
                spentPercentage
            );
            recommendations = generateHealthyRecommendations(category.getName());
            
        } else {
            return OverspendingAlert.noAlert();
        }
        
        return new OverspendingAlert(
            severity,
            alertTitle,
            alertMessage,
            category.getName(),
            totalSpent.doubleValue(),
            budgetAmount.doubleValue(),
            spentPercentage,
            budgetAmount.subtract(totalSpent).doubleValue(),
            recommendations,
            getDaysLeftInMonth()
        );
    }
    
    /**
     * Lấy tất cả alerts cho user (dashboard overview)
     */
    public List<OverspendingAlert> getAllUserAlerts(Long userId) {
        List<Budget> budgets = budgetRepository.findByUserId(userId).stream()
            .filter(this::isCurrentMonth)
            .collect(Collectors.toList());
        
        List<OverspendingAlert> alerts = new ArrayList<>();
        
        for (Budget budget : budgets) {
            if (budget.getCategory() == null) continue;
            
            List<Transaction> transactions = getMonthTransactions(userId, budget.getCategory().getId());
            BigDecimal totalSpent = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            double percentage = totalSpent.divide(budget.getAmount(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(100)).doubleValue();
            
            if (percentage >= 60) { // Chỉ hiện alert khi >= 60%
                OverspendingAlert alert = createAlertFromPercentage(
                    budget.getCategory().getName(),
                    totalSpent,
                    budget.getAmount(),
                    percentage
                );
                alerts.add(alert);
            }
        }
        
        // Sort by severity: critical > warning > info
        alerts.sort((a, b) -> {
            int severityOrder = getSeverityOrder(a.getSeverity()) - getSeverityOrder(b.getSeverity());
            if (severityOrder != 0) return severityOrder;
            return Double.compare(b.getSpentPercentage(), a.getSpentPercentage());
        });
        
        return alerts;
    }
    
    /**
     * Lời khuyên tiết kiệm theo category (học từ knowledge base)
     */
    private List<String> generateCriticalRecommendations(String categoryName, BigDecimal spent, BigDecimal budget) {
        List<String> tips = new ArrayList<>();
        double overspent = spent.subtract(budget).doubleValue();
        
        switch (categoryName.toLowerCase()) {
            case "ăn uống":
            case "food":
                tips.add("🏠 Ăn cơm nhà thay vì ăn ngoài (tiết kiệm ~60%)");
                tips.add("🍳 Tự nấu ăn và chuẩn bị cơm trưa mang đi");
                tips.add("📋 Lập kế hoạch menu tuần và mua sắm một lần");
                tips.add("💰 Tránh order đồ ăn online, phí ship tăng 30%");
                break;
                
            case "di chuyển":
            case "transport":
                tips.add("🚴 Đi xe buýt hoặc xe đạp thay vì Grab/taxi");
                tips.add("🚇 Mua vé tháng xe buýt/MRT (tiết kiệm ~50%)");
                tips.add("🤝 Carpool với đồng nghiệp/bạn bè");
                tips.add("🏃 Đi bộ với quãng đường < 2km");
                break;
                
            case "mua sắm":
            case "shopping":
                tips.add("⏸️ Dừng mua sắm không cần thiết đến cuối tháng");
                tips.add("⏰ Áp dụng quy tắc 24h: Suy nghĩ 1 ngày trước khi mua");
                tips.add("📝 Lập danh sách đồ thực sự cần thiết");
                tips.add("🛒 Chỉ mua vào dịp sale/giảm giá");
                break;
                
            case "giải trí":
            case "entertainment":
                tips.add("🎮 Tạm dừng các dịch vụ subscription không dùng");
                tips.add("🏞️ Chuyển sang hoạt động miễn phí (công viên, thư viện)");
                tips.add("🎬 Xem phim tại nhà thay vì rạp");
                tips.add("🎉 Giảm số lần đi cafe/bar xuống 50%");
                break;
                
            default:
                tips.add("📊 Xem lại chi tiết các khoản chi để tìm chỗ cắt giảm");
                tips.add("⏸️ Tạm dừng mua sắm danh mục này đến cuối tháng");
                tips.add("💳 Chỉ chi tiêu những khoản thực sự cần thiết");
                break;
        }
        
        tips.add("🎯 Mục tiêu: Giảm " + String.format("%.0f₫", overspent) + " trong " + getDaysLeftInMonth() + " ngày còn lại");
        
        return tips;
    }
    
    private List<String> generateWarningRecommendations(String categoryName, BigDecimal remaining) {
        List<String> tips = new ArrayList<>();
        
        switch (categoryName.toLowerCase()) {
            case "ăn uống":
            case "food":
                tips.add("🍱 Tăng số bữa ăn nhà lên 70%");
                tips.add("☕ Giảm cafe ngoài, pha cafe tại nhà");
                tips.add("🛒 Mua nguyên liệu tại chợ thay vì siêu thị");
                break;
                
            case "di chuyển":
            case "transport":
                tips.add("🚌 Ưu tiên phương tiện công cộng");
                tips.add("🚗 Hạn chế đi Grab, chỉ khi cần thiết");
                tips.add("🗺️ Lên kế hoạch di chuyển để tối ưu quãng đường");
                break;
                
            case "mua sắm":
            case "shopping":
                tips.add("🛍️ Giảm shopping online xuống 30%");
                tips.add("💰 Chỉ mua items trong danh sách cần thiết");
                tips.add("⏰ Áp dụng quy tắc 24h trước khi mua");
                break;
                
            default:
                tips.add("📉 Kiểm soát chi tiêu, còn " + String.format("%.0f₫", remaining.doubleValue()));
                tips.add("📋 Lập kế hoạch chi tiêu cho " + getDaysLeftInMonth() + " ngày còn lại");
                break;
        }
        
        return tips;
    }
    
    private List<String> generateHealthyRecommendations(String categoryName) {
        List<String> tips = new ArrayList<>();
        tips.add("✅ Bạn đang chi tiêu hợp lý!");
        tips.add("💰 Tiếp tục duy trì thói quen tốt này");
        tips.add("📊 Theo dõi chi tiêu hàng tuần để không vượt mức");
        return tips;
    }
    
    // Helper methods
    private List<Transaction> getMonthTransactions(Long userId, Long categoryId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
        
        return transactionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startOfMonth, endOfMonth)
            .stream()
            .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
            .filter(t -> "expense".equalsIgnoreCase(t.getType()))
            .collect(Collectors.toList());
    }
    
    private boolean isCurrentMonth(Budget budget) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth budgetMonth = YearMonth.of(budget.getYear(), budget.getMonth());
        return budgetMonth.equals(currentMonth);
    }
    
    private int getDaysLeftInMonth() {
        LocalDateTime now = LocalDateTime.now();
        YearMonth yearMonth = YearMonth.of(now.getYear(), now.getMonth());
        return yearMonth.lengthOfMonth() - now.getDayOfMonth() + 1;
    }
    
    private OverspendingAlert createAlertFromPercentage(String categoryName, BigDecimal spent, 
                                                       BigDecimal budget, double percentage) {
        String severity;
        String title;
        String message;
        List<String> recommendations;
        
        if (percentage >= 100) {
            severity = "critical";
            title = "⚠️ Vượt ngân sách " + categoryName;
            message = String.format("Đã chi %.0f%% ngân sách", percentage);
            recommendations = generateCriticalRecommendations(categoryName, spent, budget);
        } else if (percentage >= 80) {
            severity = "warning";
            title = "⚡ Sắp vượt ngân sách " + categoryName;
            message = String.format("Đã chi %.0f%% ngân sách", percentage);
            recommendations = generateWarningRecommendations(categoryName, budget.subtract(spent));
        } else {
            severity = "info";
            title = "💡 " + categoryName + " đang hợp lý";
            message = String.format("Đã chi %.0f%% ngân sách", percentage);
            recommendations = generateHealthyRecommendations(categoryName);
        }
        
        return new OverspendingAlert(
            severity, title, message, categoryName,
            spent.doubleValue(), budget.doubleValue(), percentage,
            budget.subtract(spent).doubleValue(),
            recommendations, getDaysLeftInMonth()
        );
    }
    
    private int getSeverityOrder(String severity) {
        switch (severity) {
            case "critical": return 1;
            case "warning": return 2;
            case "info": return 3;
            default: return 4;
        }
    }
    
    // DTO Classes
    public static class OverspendingAlert {
        private String severity; // critical, warning, info, none
        private String alertTitle;
        private String alertMessage;
        private String categoryName;
        private double totalSpent;
        private double budgetAmount;
        private double spentPercentage;
        private double remaining;
        private List<String> recommendations;
        private int daysLeftInMonth;
        
        public OverspendingAlert(String severity, String alertTitle, String alertMessage,
                               String categoryName, double totalSpent, double budgetAmount,
                               double spentPercentage, double remaining,
                               List<String> recommendations, int daysLeftInMonth) {
            this.severity = severity;
            this.alertTitle = alertTitle;
            this.alertMessage = alertMessage;
            this.categoryName = categoryName;
            this.totalSpent = totalSpent;
            this.budgetAmount = budgetAmount;
            this.spentPercentage = spentPercentage;
            this.remaining = remaining;
            this.recommendations = recommendations;
            this.daysLeftInMonth = daysLeftInMonth;
        }
        
        public static OverspendingAlert noAlert() {
            return new OverspendingAlert("none", "", "", "", 0, 0, 0, 0, new ArrayList<>(), 0);
        }
        
        public static OverspendingAlert noBudgetAlert(String categoryName) {
            return new OverspendingAlert(
                "info",
                "💡 Chưa có ngân sách",
                "Tạo ngân sách cho " + categoryName + " để theo dõi chi tiêu tốt hơn",
                categoryName,
                0, 0, 0, 0,
                Arrays.asList("Tạo ngân sách hàng tháng để kiểm soát chi tiêu"),
                0
            );
        }
        
        // Getters
        public String getSeverity() { return severity; }
        public String getAlertTitle() { return alertTitle; }
        public String getAlertMessage() { return alertMessage; }
        public String getCategoryName() { return categoryName; }
        public double getTotalSpent() { return totalSpent; }
        public double getBudgetAmount() { return budgetAmount; }
        public double getSpentPercentage() { return spentPercentage; }
        public double getRemaining() { return remaining; }
        public List<String> getRecommendations() { return recommendations; }
        public int getDaysLeftInMonth() { return daysLeftInMonth; }
    }
}

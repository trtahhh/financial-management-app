package com.example.finance.service;

import com.example.finance.dto.SmartAnalyticsResponse;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.Category;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmartAnalyticsService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    /**
     * Parse user query and execute analytics
     */
    public SmartAnalyticsResponse analyzeQuery(String query, Long userId) {
        String normalizedQuery = query.toLowerCase().trim();
        
        System.out.println("[SMART ANALYTICS] Query: " + query);
        
        // Detect query type
        if (isLargestExpenseQuery(normalizedQuery)) {
            return getLargestExpense(userId, normalizedQuery);
        } else if (isTopExpensesQuery(normalizedQuery)) {
            return getTopExpenses(userId, normalizedQuery);
        } else if (isCategoryAnalysisQuery(normalizedQuery)) {
            return analyzeCategorySpending(userId, normalizedQuery);
        } else if (isMonthlyTotalQuery(normalizedQuery)) {
            return getMonthlyTotal(userId, normalizedQuery);
        } else if (isSavingAdviceQuery(normalizedQuery)) {
            return getSavingAdvice(userId);
        } else if (isSpendingLimitQuery(normalizedQuery)) {
            return getSpendingLimit(userId);
        }
        
        // Default fallback
        return getGeneralOverview(userId);
    }
    
    // ========== Query Type Detection ==========
    
    private boolean isLargestExpenseQuery(String query) {
        return query.matches(".*(khoản chi|giao dịch|chi tiêu).*(lớn nhất|cao nhất|nhiều nhất).*") ||
               query.matches(".*(lớn nhất|cao nhất|nhiều nhất).*(khoản chi|giao dịch|chi tiêu).*");
    }
    
    private boolean isTopExpensesQuery(String query) {
        return query.matches(".*(top|danh sách|xem).*(chi tiêu|giao dịch|khoản chi).*") ||
               query.matches(".*(chi tiêu|giao dịch).*(lớn|nhiều).*");
    }
    
    private boolean isCategoryAnalysisQuery(String query) {
        return query.matches(".*(category|danh mục|loại).*(nào|gì).*(nhiều|lớn|cao).*") ||
               query.matches(".*(chi|tiêu).*(nhiều nhất|lớn nhất).*category.*");
    }
    
    private boolean isMonthlyTotalQuery(String query) {
        return query.matches(".*(tháng (này|nay|trước)).*(chi|tiêu|tổng).*") ||
               query.matches(".*(chi|tiêu).*(tháng|month).*");
    }
    
    private boolean isSavingAdviceQuery(String query) {
        return query.matches(".*(tiết kiệm|saving|save).*") ||
               query.matches(".*(gợi ý|advice|tip).*");
    }
    
    private boolean isSpendingLimitQuery(String query) {
        return query.matches(".*(nên|hợp lý|reasonable).*(chi|tiêu|spend).*") ||
               query.matches(".*(bao nhiêu|how much).*(ổn|ok|reasonable).*");
    }
    
    // ========== Analytics Implementations ==========
    
    /**
     * Get largest expense in timeframe
     */
    private SmartAnalyticsResponse getLargestExpense(Long userId, String query) {
        LocalDate[] dateRange = extractDateRange(query);
        
        List<Transaction> expenses = transactionRepository.findByUserIdAndTypeBetweenDates(
            userId, "expense", dateRange[0], dateRange[1]
        );
        
        if (expenses.isEmpty()) {
            return SmartAnalyticsResponse.builder()
                .mainMessage("Không tìm thấy giao dịch nào trong khoảng thời gian này 🤔")
                .quickActions(buildDefaultActions())
                .build();
        }
        
        // Find largest expense
        Transaction largest = expenses.stream()
            .max(Comparator.comparing(t -> t.getAmount().doubleValue()))
            .orElse(expenses.get(0));
        
        Category category = largest.getCategory();
        String categoryName = category != null ? category.getName() : "Khác";
        
        // Build response
        String mainMessage = String.format(
            "Khoản chi lớn nhất tháng này của bạn là **%,.0fđ** vào ngày %s cho **%s**.",
            largest.getAmount().doubleValue(),
            largest.getDate().format(DATE_FORMATTER),
            categoryName
        );
        
        // Generate contextual insight
        String insight = generateExpenseInsight(largest, expenses, category);
        
        List<SmartAnalyticsResponse.TransactionSummary> topTransactions = expenses.stream()
            .sorted(Comparator.comparing((Transaction t) -> t.getAmount().doubleValue()).reversed())
            .limit(5)
            .map(t -> buildTransactionSummary(t, t.getId().equals(largest.getId())))
            .collect(Collectors.toList());
        
        List<SmartAnalyticsResponse.InsightItem> insights = new ArrayList<>();
        if (insight != null) {
            insights.add(SmartAnalyticsResponse.InsightItem.builder()
                .icon("💡")
                .text(insight)
                .type("info")
                .build());
        }
        
        return SmartAnalyticsResponse.builder()
            .mainMessage(mainMessage)
            .detailedMessage("Xem chi tiết các giao dịch lớn nhất tháng qua bên dưới nhé!")
            .transactions(topTransactions)
            .insights(insights)
            .quickActions(buildExpenseActions(largest.getCategory() != null ? largest.getCategory().getId() : null))
            .build();
    }
    
    /**
     * Get top N expenses
     */
    private SmartAnalyticsResponse getTopExpenses(Long userId, String query) {
        LocalDate[] dateRange = extractDateRange(query);
        int limit = extractLimit(query, 5);
        
        List<Transaction> expenses = transactionRepository.findByUserIdAndTypeBetweenDates(
            userId, "expense", dateRange[0], dateRange[1]
        );
        
        List<SmartAnalyticsResponse.TransactionSummary> topExpenses = expenses.stream()
            .sorted(Comparator.comparing((Transaction t) -> t.getAmount().doubleValue()).reversed())
            .limit(limit)
            .map(t -> buildTransactionSummary(t, false))
            .collect(Collectors.toList());
        
        double total = expenses.stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();
        double topTotal = topExpenses.stream().mapToDouble(SmartAnalyticsResponse.TransactionSummary::getAmount).sum();
        double percentage = total > 0 ? (topTotal / total * 100) : 0;
        
        String mainMessage = String.format(
            "Top %d giao dịch lớn nhất chiếm **%.1f%%** tổng chi tiêu (**%,.0fđ** / **%,.0fđ**).",
            limit, percentage, topTotal, total
        );
        
        return SmartAnalyticsResponse.builder()
            .mainMessage(mainMessage)
            .transactions(topExpenses)
            .quickActions(buildDefaultActions())
            .build();
    }
    
    /**
     * Analyze category spending
     */
    private SmartAnalyticsResponse analyzeCategorySpending(Long userId, String query) {
        LocalDate[] dateRange = extractDateRange(query);
        
        List<Transaction> expenses = transactionRepository.findByUserIdAndTypeBetweenDates(
            userId, "expense", dateRange[0], dateRange[1]
        );
        
        // Group by category
        Map<Long, Double> categoryTotals = expenses.stream()
            .filter(t -> t.getCategory() != null)
            .collect(Collectors.groupingBy(
                t -> t.getCategory().getId(),
                Collectors.summingDouble(t -> t.getAmount().doubleValue())
            ));
        
        if (categoryTotals.isEmpty()) {
            return SmartAnalyticsResponse.builder()
                .mainMessage("Chưa có giao dịch nào trong khoảng thời gian này.")
                .build();
        }
        
        // Find top category
        Long topCategoryId = categoryTotals.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        Category topCategory = categoryRepository.findById(topCategoryId).orElse(null);
        String categoryName = topCategory != null ? topCategory.getName() : "Khác";
        double categoryTotal = categoryTotals.get(topCategoryId);
        double grandTotal = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();
        double percentage = (categoryTotal / grandTotal * 100);
        
        String mainMessage = String.format(
            "Bạn chi nhiều nhất cho **%s** với **%,.0fđ**, chiếm **%.1f%%** tổng chi tiêu.",
            categoryName, categoryTotal, percentage
        );
        
        // Build insights
        List<SmartAnalyticsResponse.InsightItem> insights = buildCategoryInsights(categoryTotals, grandTotal);
        
        return SmartAnalyticsResponse.builder()
            .mainMessage(mainMessage)
            .insights(insights)
            .quickActions(buildCategoryActions(topCategoryId))
            .build();
    }
    
    /**
     * Get monthly total spending
     */
    private SmartAnalyticsResponse getMonthlyTotal(Long userId, String query) {
        YearMonth targetMonth = extractMonth(query);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();
        
        List<Transaction> expenses = transactionRepository.findByUserIdAndTypeBetweenDates(
            userId, "expense", startDate, endDate
        );
        
        double total = expenses.stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();
        
        // Get previous month for comparison
        YearMonth previousMonth = targetMonth.minusMonths(1);
        List<Transaction> previousExpenses = transactionRepository.findByUserIdAndTypeBetweenDates(
            userId, "expense", previousMonth.atDay(1), previousMonth.atEndOfMonth()
        );
        double previousTotal = previousExpenses.stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();
        
        String mainMessage = String.format(
            "Tháng %d/%d bạn đã chi **%,.0fđ**.",
            targetMonth.getMonthValue(), targetMonth.getYear(), total
        );
        
        List<SmartAnalyticsResponse.InsightItem> insights = new ArrayList<>();
        
        // Comparison with previous month
        if (previousTotal > 0) {
            double changePercent = ((total - previousTotal) / previousTotal) * 100;
            String trend = changePercent > 0 ? "tăng" : "giảm";
            String icon = changePercent > 0 ? "📈" : "📉";
            
            insights.add(SmartAnalyticsResponse.InsightItem.builder()
                .icon(icon)
                .text(String.format("So với tháng trước %s %.1f%% (%,.0fđ → %,.0fđ)", 
                    trend, Math.abs(changePercent), previousTotal, total))
                .type(changePercent > 10 ? "warning" : "info")
                .value(changePercent)
                .build());
        }
        
        return SmartAnalyticsResponse.builder()
            .mainMessage(mainMessage)
            .insights(insights)
            .quickActions(buildMonthlyActions())
            .build();
    }
    
    /**
     * Get saving advice
     */
    private SmartAnalyticsResponse getSavingAdvice(Long userId) {
        // Get last 3 months data
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(3);
        
        List<Transaction> expenses = transactionRepository.findByUserIdAndTypeBetweenDates(
            userId, "expense", startDate, endDate
        );
        
        List<Transaction> incomes = transactionRepository.findByUserIdAndTypeBetweenDates(
            userId, "income", startDate, endDate
        );
        
        double totalExpense = expenses.stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();
        double totalIncome = incomes.stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();
        double monthlyExpense = totalExpense / 3;
        double monthlyIncome = totalIncome / 3;
        
        double savingRate = totalIncome > 0 ? ((totalIncome - totalExpense) / totalIncome * 100) : 0;
        
        String mainMessage;
        if (savingRate >= 20) {
            mainMessage = "🎉 Tuyệt vời! Bạn đang tiết kiệm rất tốt!";
        } else if (savingRate >= 10) {
            mainMessage = "👍 Bạn đang tiết kiệm khá ổn, nhưng có thể cải thiện thêm.";
        } else if (savingRate > 0) {
            mainMessage = "⚠️ Tỷ lệ tiết kiệm của bạn hơi thấp, nên cải thiện nhé!";
        } else {
            mainMessage = "🚨 Bạn đang chi tiêu vượt thu nhập! Cần điều chỉnh gấp.";
        }
        
        List<SmartAnalyticsResponse.InsightItem> insights = generateSavingInsights(
            monthlyIncome, monthlyExpense, savingRate, expenses
        );
        
        return SmartAnalyticsResponse.builder()
            .mainMessage(mainMessage)
            .insights(insights)
            .quickActions(buildSavingActions())
            .build();
    }
    
    /**
     * Get recommended spending limit
     */
    private SmartAnalyticsResponse getSpendingLimit(Long userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(1);
        
        List<Transaction> incomes = transactionRepository.findByUserIdAndTypeBetweenDates(
            userId, "income", startDate, endDate
        );
        
        double monthlyIncome = incomes.stream().mapToDouble(t -> t.getAmount().doubleValue()).sum();
        
        if (monthlyIncome == 0) {
            return SmartAnalyticsResponse.builder()
                .mainMessage("Chưa có dữ liệu thu nhập để tính toán 😅")
                .build();
        }
        
        // 50/30/20 rule
        double needs = monthlyIncome * 0.50; // Nhu cầu thiết yếu
        double wants = monthlyIncome * 0.30; // Mong muốn
        double savings = monthlyIncome * 0.20; // Tiết kiệm
        
        String mainMessage = String.format(
            "Với thu nhập **%,.0fđ**/tháng, bạn nên phân bổ:",
            monthlyIncome
        );
        
        List<SmartAnalyticsResponse.InsightItem> insights = Arrays.asList(
            SmartAnalyticsResponse.InsightItem.builder()
                .icon("🏠")
                .text(String.format("**50%%** cho nhu cầu thiết yếu: %,.0fđ (ăn uống, nhà ở, đi lại)", needs))
                .type("info")
                .build(),
            SmartAnalyticsResponse.InsightItem.builder()
                .icon("🎮")
                .text(String.format("**30%%** cho giải trí: %,.0fđ (mua sắm, du lịch, sở thích)", wants))
                .type("info")
                .build(),
            SmartAnalyticsResponse.InsightItem.builder()
                .icon("💰")
                .text(String.format("**20%%** để tiết kiệm: %,.0fđ (dành dụm, đầu tư)", savings))
                .type("success")
                .build()
        );
        
        return SmartAnalyticsResponse.builder()
            .mainMessage(mainMessage)
            .detailedMessage("Áp dụng quy tắc 50/30/20 để quản lý tài chính hiệu quả!")
            .insights(insights)
            .quickActions(buildDefaultActions())
            .build();
    }
    
    /**
     * General overview fallback
     */
    private SmartAnalyticsResponse getGeneralOverview(Long userId) {
        return SmartAnalyticsResponse.builder()
            .mainMessage("Moni chưa hiểu câu hỏi của bạn 🤔")
            .detailedMessage("Bạn có thể hỏi Moni về:")
            .insights(Arrays.asList(
                SmartAnalyticsResponse.InsightItem.builder()
                    .icon("💰")
                    .text("Khoản chi lớn nhất tháng này")
                    .type("info")
                    .build(),
                SmartAnalyticsResponse.InsightItem.builder()
                    .icon("📊")
                    .text("Tháng này tôi chi bao nhiêu?")
                    .type("info")
                    .build(),
                SmartAnalyticsResponse.InsightItem.builder()
                    .icon("💡")
                    .text("Tôi nên tiết kiệm như thế nào?")
                    .type("info")
                    .build()
            ))
            .quickActions(buildDefaultActions())
            .build();
    }
    
    // ========== Helper Methods ==========
    
    private LocalDate[] extractDateRange(String query) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        
        if (query.contains("tháng này") || query.contains("tháng nay")) {
            startDate = YearMonth.now().atDay(1);
        } else if (query.contains("tháng trước") || query.contains("tháng qua")) {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            startDate = lastMonth.atDay(1);
            endDate = lastMonth.atEndOfMonth();
        } else if (query.contains("tuần này")) {
            startDate = endDate.minusDays(7);
        } else {
            // Default: last 30 days
            startDate = endDate.minusDays(30);
        }
        
        return new LocalDate[]{startDate, endDate};
    }
    
    private YearMonth extractMonth(String query) {
        if (query.contains("tháng trước") || query.contains("tháng qua")) {
            return YearMonth.now().minusMonths(1);
        }
        return YearMonth.now();
    }
    
    private int extractLimit(String query, int defaultLimit) {
        if (query.matches(".*top\\s+(\\d+).*")) {
            try {
                return Integer.parseInt(query.replaceAll(".*top\\s+(\\d+).*", "$1"));
            } catch (Exception e) {
                return defaultLimit;
            }
        }
        return defaultLimit;
    }
    
    private SmartAnalyticsResponse.TransactionSummary buildTransactionSummary(Transaction t, boolean isHighlight) {
        Category category = t.getCategory();
        return SmartAnalyticsResponse.TransactionSummary.builder()
            .description(t.getNote() != null ? t.getNote() : category != null ? category.getName() : "Giao dịch")
            .category(category != null ? category.getName() : "Khác")
            .date(t.getDate().format(DATE_FORMATTER))
            .amount(t.getAmount().doubleValue())
            .highlight(isHighlight ? "largest" : null)
            .build();
    }
    
    private String generateExpenseInsight(Transaction largest, List<Transaction> allExpenses, Category category) {
        if (category == null) return null;
        
        String categoryName = category.getName().toLowerCase();
        
        if (categoryName.contains("điện") || categoryName.contains("hóa đơn")) {
            return "Có vẻ như tiền điện tháng này hơi 'chặt' nhi, chắc nhà bạn bật điều hòa cả ngày rồi! ❄️";
        } else if (categoryName.contains("ăn uống")) {
            return "Chi tiêu ăn uống khá nhiều đấy! Thỉnh thoảng nấu ăn tại nhà để tiết kiệm nhé 🍳";
        } else if (categoryName.contains("mua sắm")) {
            return "Mua sắm nhiều quá rồi! Nhớ kiểm tra xem có thứ gì thực sự cần thiết không nhé 🛍️";
        }
        
        return null;
    }
    
    private List<SmartAnalyticsResponse.InsightItem> buildCategoryInsights(Map<Long, Double> categoryTotals, double total) {
        return categoryTotals.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(3)
            .map(entry -> {
                Category cat = categoryRepository.findById(entry.getKey()).orElse(null);
                String name = cat != null ? cat.getName() : "Khác";
                double percentage = (entry.getValue() / total) * 100;
                
                return SmartAnalyticsResponse.InsightItem.builder()
                    .icon(getIconForCategory(name))
                    .text(String.format("%s: %,.0fđ (%.1f%%)", name, entry.getValue(), percentage))
                    .type("info")
                    .value(percentage)
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    private List<SmartAnalyticsResponse.InsightItem> generateSavingInsights(
            double monthlyIncome, double monthlyExpense, double savingRate, List<Transaction> expenses) {
        
        List<SmartAnalyticsResponse.InsightItem> insights = new ArrayList<>();
        
        insights.add(SmartAnalyticsResponse.InsightItem.builder()
            .icon("📊")
            .text(String.format("Thu nhập trung bình: %,.0fđ/tháng", monthlyIncome))
            .type("info")
            .build());
        
        insights.add(SmartAnalyticsResponse.InsightItem.builder()
            .icon("💸")
            .text(String.format("Chi tiêu trung bình: %,.0fđ/tháng", monthlyExpense))
            .type("info")
            .build());
        
        insights.add(SmartAnalyticsResponse.InsightItem.builder()
            .icon(savingRate >= 20 ? "✅" : "⚠️")
            .text(String.format("Tỷ lệ tiết kiệm: %.1f%% (khuyến nghị: ≥20%%)", savingRate))
            .type(savingRate >= 20 ? "success" : "warning")
            .value(savingRate)
            .build());
        
        if (savingRate < 20) {
            double targetSaving = monthlyIncome * 0.20;
            double needToReduce = monthlyExpense - (monthlyIncome - targetSaving);
            
            insights.add(SmartAnalyticsResponse.InsightItem.builder()
                .icon("💡")
                .text(String.format("Nên giảm chi tiêu %,.0fđ/tháng để đạt mục tiêu tiết kiệm 20%%", needToReduce))
                .type("tip")
                .build());
        }
        
        return insights;
    }
    
    private String getIconForCategory(String categoryName) {
        String lower = categoryName.toLowerCase();
        if (lower.contains("ăn") || lower.contains("uống")) return "🍴";
        if (lower.contains("giao thông")) return "🚗";
        if (lower.contains("giải trí")) return "🎮";
        if (lower.contains("sức khỏe")) return "💊";
        if (lower.contains("giáo dục")) return "📚";
        if (lower.contains("mua sắm")) return "🛍️";
        if (lower.contains("tiện ích") || lower.contains("điện") || lower.contains("nước")) return "⚡";
        return "📌";
    }
    
    // ========== Quick Actions Builders ==========
    
    private List<SmartAnalyticsResponse.QuickAction> buildDefaultActions() {
        return Arrays.asList(
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Xem chi tiết các giao dịch")
                .action("view_transactions")
                .build(),
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Phân tích chi tiêu theo category")
                .action("analyze_categories")
                .build(),
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Gợi ý tiết kiệm")
                .action("get_saving_tips")
                .build()
        );
    }
    
    private List<SmartAnalyticsResponse.QuickAction> buildExpenseActions(Long categoryId) {
        return Arrays.asList(
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Xem chi tiết category này")
                .action("view_category_details")
                .categoryId(categoryId.toString())
                .build(),
            SmartAnalyticsResponse.QuickAction.builder()
                .label("So sánh với tháng trước")
                .action("compare_months")
                .build(),
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Gợi ý tiết kiệm")
                .action("get_saving_tips")
                .build()
        );
    }
    
    private List<SmartAnalyticsResponse.QuickAction> buildCategoryActions(Long categoryId) {
        return Arrays.asList(
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Xem lịch sử category này")
                .action("view_category_history")
                .categoryId(categoryId.toString())
                .build(),
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Đặt budget cho category")
                .action("set_category_budget")
                .categoryId(categoryId.toString())
                .build()
        );
    }
    
    private List<SmartAnalyticsResponse.QuickAction> buildMonthlyActions() {
        return Arrays.asList(
            SmartAnalyticsResponse.QuickAction.builder()
                .label("So sánh với tháng trước")
                .action("compare_months")
                .build(),
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Phân tích theo category")
                .action("analyze_categories")
                .build()
        );
    }
    
    private List<SmartAnalyticsResponse.QuickAction> buildSavingActions() {
        return Arrays.asList(
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Đặt mục tiêu tiết kiệm")
                .action("set_saving_goal")
                .build(),
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Xem chi tiết chi tiêu")
                .action("view_spending_details")
                .build(),
            SmartAnalyticsResponse.QuickAction.builder()
                .label("Tạo budget cho tháng sau")
                .action("create_monthly_budget")
                .build()
        );
    }
}

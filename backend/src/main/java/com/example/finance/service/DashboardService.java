package com.example.finance.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final WalletService walletService;
    private final NotificationService notificationService;

    @Cacheable(value = "dashboard", key = "#userId + '_' + #month + '_' + #year")
    public Map<String, Object> getDashboardData(Long userId, Integer month, Integer year) {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 1. Thống kê tài chính tháng hiện tại
        Map<String, Object> monthlyStats = getMonthlyStats(userId, month, year);
        dashboard.put("monthlyStats", monthlyStats);
        
        // 2. Tổng số dư tất cả ví
        BigDecimal totalBalance = walletService.getTotalBalance(userId);
        dashboard.put("totalBalance", totalBalance);
        
        // 3. Tiến độ ngân sách
        List<Map<String, Object>> budgetProgress = budgetService.getBudgetVsActual(userId, month, year);
        dashboard.put("budgetProgress", budgetProgress);
        
        // 4. Cảnh báo ngân sách
        List<Map<String, Object>> budgetWarnings = budgetService.getBudgetWarnings(userId, month, year);
        dashboard.put("budgetWarnings", budgetWarnings);
        
        // 5. Tiến độ mục tiêu
        List<Map<String, Object>> goalProgress = goalService.getGoalProgress(userId);
        dashboard.put("goalProgress", goalProgress);
        
        // 6. Chi tiêu theo danh mục
        List<Map<String, Object>> expensesByCategory = transactionService.getExpensesByCategory(userId, month, year);
        dashboard.put("expensesByCategory", expensesByCategory);
        
        // 7. Giao dịch gần đây
        List<Map<String, Object>> recentTransactions = transactionService.getRecentTransactions(userId, 10);
        dashboard.put("recentTransactions", recentTransactions);
        
        // 8. Thông báo chưa đọc
        List<Map<String, Object>> unreadNotifications = notificationService.getUnreadNotifications(userId);
        dashboard.put("notifications", unreadNotifications);
        
        // 9. Xu hướng chi tiêu 6 tháng gần đây
        List<Map<String, Object>> spendingTrend = getSpendingTrend(userId, 6);
        dashboard.put("spendingTrend", spendingTrend);
        
        // 10. Thống kê tổng quan
        Map<String, Object> overview = getOverviewStats(userId);
        dashboard.put("overview", overview);
        
        return dashboard;
    }

    private Map<String, Object> getMonthlyStats(Long userId, Integer month, Integer year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        BigDecimal monthlyIncome = transactionService.getTotalByTypeAndDateRange(userId, "income", startDate, endDate);
        BigDecimal monthlyExpense = transactionService.getTotalByTypeAndDateRange(userId, "expense", startDate, endDate);
        BigDecimal netIncome = monthlyIncome.subtract(monthlyExpense);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("monthlyIncome", monthlyIncome);
        stats.put("monthlyExpense", monthlyExpense);
        stats.put("netIncome", netIncome);
        stats.put("savingsRate", monthlyIncome.compareTo(BigDecimal.ZERO) > 0 ? 
                netIncome.divide(monthlyIncome, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO);
        
        return stats;
    }

    private List<Map<String, Object>> getSpendingTrend(Long userId, int months) {
        // Implementation để lấy xu hướng chi tiêu
        return transactionService.getMonthlySpendingTrend(userId, months);
    }

    private Map<String, Object> getOverviewStats(Long userId) {
        Map<String, Object> overview = new HashMap<>();
        
        // Tổng số giao dịch
        Long totalTransactions = transactionService.countByUserId(userId);
        overview.put("totalTransactions", totalTransactions);
        
        // Số mục tiêu đang hoạt động
        Long activeGoals = goalService.countActiveGoals(userId);
        overview.put("activeGoals", activeGoals);
        
        // Số ngân sách tháng này
        Long activeBudgets = budgetService.countActiveBudgets(userId, LocalDate.now().getMonthValue(), LocalDate.now().getYear());
        overview.put("activeBudgets", activeBudgets);
        
        // Số ví
        Long totalWallets = walletService.countByUserId(userId);
        overview.put("totalWallets", totalWallets);
        
        return overview;
    }

    /**
     * API riêng cho mobile hoặc lightweight requests
     */
    public Map<String, Object> getBasicStats(Long userId, Integer month, Integer year) {
        Map<String, Object> basicStats = new HashMap<>();
        
        // Chỉ trả về stats cơ bản
        Map<String, Object> monthlyStats = getMonthlyStats(userId, month, year);
        basicStats.put("monthlyStats", monthlyStats);
        
        BigDecimal totalBalance = walletService.getTotalBalance(userId);
        basicStats.put("totalBalance", totalBalance);
        
        Long totalTransactions = transactionService.countByUserId(userId);
        basicStats.put("totalTransactions", totalTransactions);
        
        Long activeGoals = goalService.countActiveGoals(userId);
        basicStats.put("activeGoals", activeGoals);
        
        return basicStats;
    }

    public Map<String, Object> getDashboardDataByDate(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> dashboard = new HashMap<>();

        BigDecimal totalIncome = transactionService.getTotalByTypeAndDateRange(userId, "income", startDate, endDate);
        BigDecimal totalExpense = transactionService.getTotalByTypeAndDateRange(userId, "expense", startDate, endDate);
        BigDecimal netIncome = totalIncome.subtract(totalExpense);

        dashboard.put("totalIncome", totalIncome);
        dashboard.put("totalExpense", totalExpense);
        dashboard.put("netIncome", netIncome);

        dashboard.put("expensesByCategory", transactionService.getExpensesByCategoryByDate(userId, startDate, endDate));
        dashboard.put("budgetProgress", budgetService.getBudgetVsActualByDate(userId, startDate, endDate));
        dashboard.put("goalProgress", goalService.getGoalProgress(userId));
        dashboard.put("totalBalance", walletService.getTotalBalance(userId));
        dashboard.put("recentTransactions", transactionService.getRecentTransactionsByDate(userId, startDate, endDate, 10));
        dashboard.put("notifications", notificationService.getUnreadNotifications(userId));

        return dashboard;
    }
}
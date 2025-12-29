package com.example.finance.service;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.Wallet;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

 private final TransactionRepository transactionRepository;
 private final WalletRepository walletRepository;
 private final BudgetService budgetService;
 private final GoalService goalService;
 private final CategoryColorService categoryColorService;

 public Map<String, Object> getDashboardData(Long userId, LocalDate dateFrom, LocalDate dateTo) {
 log.info("Getting dashboard data for user: {} from {} to {}", userId, dateFrom, dateTo);
 
 Map<String, Object> dashboard = new HashMap<>();
 
 try {
 // Lấy giao dịch theo khoảng thời gian
 List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(userId, dateFrom, dateTo);
 log.info("Retrieved {} transactions for user {}", transactions.size(), userId);
 
 // Tính tổng thu chi
 BigDecimal totalIncome = transactions.stream()
 .filter(t -> "income".equals(t.getType()))
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 BigDecimal totalExpense = transactions.stream()
 .filter(t -> "expense".equals(t.getType()))
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 // Lấy ví tiền để tính tổng số dư thực tế
 List<Wallet> wallets = walletRepository.findByUserId(userId);
 log.info("Found {} wallets for user {}", wallets.size(), userId);
 
 // Tính tổng số dư từ tất cả các ví
 BigDecimal totalBalance = wallets.stream()
 .map(Wallet::getBalance)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 log.info("Total balance calculated from wallets: {} for user {}", totalBalance, userId);
 
 dashboard.put("totalIncome", totalIncome);
 dashboard.put("totalExpense", totalExpense);
 dashboard.put("totalBalance", totalBalance);
 dashboard.put("wallets", wallets);
 dashboard.put("recentTransactions", transactions.stream().limit(5).collect(Collectors.toList()));
 
 log.info("Dashboard data calculated successfully for user: {} - Income: {}, Expense: {}, Total Balance from Wallets: {}", 
 userId, totalIncome, totalExpense, totalBalance);
 return dashboard;
 
 } catch (Exception e) {
 log.error("Error calculating dashboard data for user: {}", userId, e);
 // Trả về dữ liệu mặc định nếu có lỗi
 dashboard.put("totalIncome", BigDecimal.ZERO);
 dashboard.put("totalExpense", BigDecimal.ZERO);
 dashboard.put("totalBalance", BigDecimal.ZERO);
 dashboard.put("wallets", new ArrayList<>());
 dashboard.put("recentTransactions", new ArrayList<>());
 return dashboard;
 }
 }

 /**
 * Lấy dữ liệu dashboard theo tháng/năm (giữ lại method cũ để tương thích)
 */
 public Map<String, Object> getDashboardDataByMonth(Long userId, Integer month, Integer year) {
 Map<String, Object> dashboard = new HashMap<>();
 
 try {
 // Lấy tất cả giao dịch trong tháng
 List<Transaction> monthTransactions = transactionRepository.findByUserIdAndMonthAndYear(userId, month, year);
 log.info("Retrieved {} transactions for user {} in month {}/{}", monthTransactions.size(), userId, month, year);
 
 // Tính tổng thu chi từ transactions
 BigDecimal totalIncome = monthTransactions.stream()
 .filter(t -> "income".equals(t.getType()))
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 BigDecimal totalExpense = monthTransactions.stream()
 .filter(t -> "expense".equals(t.getType()))
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 // Lấy ví tiền để hiển thị
 List<Wallet> wallets = walletRepository.findByUserId(userId);
 
 // Tính tổng số dư từ tất cả các ví
 BigDecimal totalBalance = wallets.stream()
 .map(Wallet::getBalance)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 log.info("Total balance calculated from wallets: {} for user {}", totalBalance, userId);
 
 // Lấy giao dịch gần đây (5 giao dịch cuối cùng)
 List<Transaction> recentTransactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
 .stream()
 .limit(5)
 .collect(Collectors.toList());
 
 // Chuyển đổi giao dịch thành format phù hợp cho frontend
 List<Map<String, Object>> formattedTransactions = recentTransactions.stream()
 .map(tx -> {
 Map<String, Object> txMap = new HashMap<>();
 txMap.put("id", tx.getId());
 txMap.put("type", tx.getType());
 txMap.put("amount", tx.getAmount());
 txMap.put("date", tx.getDate());
 txMap.put("note", tx.getNote());
 txMap.put("categoryName", tx.getCategory() != null ? tx.getCategory().getName() : "Khác");
 txMap.put("walletName", tx.getWallet() != null ? tx.getWallet().getName() : "Không xác định");
 return txMap;
 })
 .collect(Collectors.toList());
 
 dashboard.put("totalIncome", totalIncome);
 dashboard.put("totalExpense", totalExpense);
 dashboard.put("netIncome", totalIncome.subtract(totalExpense));
 dashboard.put("totalBalance", totalBalance); // Tổng số dư từ các ví
 dashboard.put("recentTransactions", formattedTransactions);
 dashboard.put("wallets", wallets);
 
 // Thêm dữ liệu cho biểu đồ
 dashboard.put("expensesByCategory", getExpensesByCategory(userId, month, year));
 dashboard.put("spendingTrend", getWeeklyTrend(userId, month, year));
 
 // Tích hợp dữ liệu thực từ BudgetService và GoalService
 try {
 // Lấy tiến độ ngân sách
 List<Map<String, Object>> budgetProgress = budgetService.getBudgetVsActual(userId, month, year);
 dashboard.put("budgetProgress", budgetProgress);
 
 // Lấy cảnh báo ngân sách
 List<Map<String, Object>> budgetWarnings = budgetService.getBudgetWarnings(userId, month, year);
 dashboard.put("budgetWarnings", budgetWarnings);
 
 log.info("Budget data integrated: {} budgets, {} warnings", budgetProgress.size(), budgetWarnings.size());
 } catch (Exception e) {
 log.warn("Failed to get budget data: {}", e.getMessage());
 dashboard.put("budgetProgress", new ArrayList<>());
 dashboard.put("budgetWarnings", new ArrayList<>());
 }
 
 try {
 // Lấy tiến độ mục tiêu
 List<Map<String, Object>> goalProgress = goalService.getGoalProgress(userId);
 dashboard.put("goalProgress", goalProgress);
 dashboard.put("activeGoalsCount", (long) goalProgress.size());
 
 log.info("Goal data integrated: {} active goals", goalProgress.size());
 } catch (Exception e) {
 log.warn("Failed to get goal data: {}", e.getMessage());
 dashboard.put("goalProgress", new ArrayList<>());
 dashboard.put("activeGoalsCount", 0L);
 }
 
 log.info("Dashboard data by month calculated successfully for user: {} - Income: {}, Expense: {}, Calculated Balance: {}", 
 userId, totalIncome, totalExpense, totalBalance);
 
 } catch (Exception e) {
 log.error("Error calculating dashboard data by month for user: {}", userId, e);
 // Trả về dữ liệu mặc định nếu có lỗi
 dashboard.put("totalIncome", BigDecimal.ZERO);
 dashboard.put("totalExpense", BigDecimal.ZERO);
 dashboard.put("netIncome", BigDecimal.ZERO);
 dashboard.put("totalBalance", BigDecimal.ZERO);
 dashboard.put("expensesByCategory", new ArrayList<>());
 dashboard.put("spendingTrend", new ArrayList<>());
 dashboard.put("goalProgress", new ArrayList<>());
 dashboard.put("activeGoalsCount", 0L);
 dashboard.put("recentTransactions", new ArrayList<>());
 dashboard.put("wallets", new ArrayList<>());
 dashboard.put("budgetProgress", new ArrayList<>());
 dashboard.put("budgetWarnings", new ArrayList<>());
 }
 
 return dashboard;
 }

 /**
 * Lấy dữ liệu dashboard theo khoảng thời gian (để hỗ trợ endpoint data-by-date)
 */
 // Getter methods để test
 public GoalService getGoalService() {
 return goalService;
 }
 
 public BudgetService getBudgetService() {
 return budgetService;
 }

 public Map<String, Object> getDashboardDataByDate(Long userId, LocalDate dateFrom, LocalDate dateTo) {
 Map<String, Object> dashboard = new HashMap<>();
 
 try {
 // ✅ FIX: Use eager load query to avoid LazyInitializationException
 List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenWithEagerLoad(userId, dateFrom, dateTo);
 log.info("Retrieved {} transactions for user {} from {} to {}", transactions.size(), userId, dateFrom, dateTo);
 
 // Tính tổng thu chi từ transactions
 BigDecimal totalIncome = transactions.stream()
 .filter(t -> "income".equals(t.getType()))
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 BigDecimal totalExpense = transactions.stream()
 .filter(t -> "expense".equals(t.getType()))
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 // Lấy ví tiền để hiển thị
 List<Wallet> wallets = walletRepository.findByUserId(userId);
 
 // Tính tổng số dư từ tất cả các ví
 BigDecimal totalBalance = wallets.stream()
 .map(Wallet::getBalance)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 log.info("Total balance calculated from wallets: {} for user {}", totalBalance, userId);
 
 // ✅ FIX: Lấy giao dịch gần đây TRONG KHOẢNG THỜI GIAN (không lấy toàn bộ)
 List<Transaction> recentTransactions = transactions
 .stream()
 .limit(5)
 .collect(Collectors.toList());
 
 // Chuyển đổi giao dịch thành format phù hợp cho frontend
 List<Map<String, Object>> formattedTransactions = recentTransactions.stream()
 .map(tx -> {
 Map<String, Object> txMap = new HashMap<>();
 txMap.put("id", tx.getId());
 txMap.put("type", tx.getType());
 txMap.put("amount", tx.getAmount());
 txMap.put("date", tx.getDate());
 txMap.put("note", tx.getNote());
 // ✅ FIX: Handle lazy loading - use try-catch or check if category is null
 try {
 String categoryName = tx.getCategory() != null ? tx.getCategory().getName() : "Khác";
 txMap.put("categoryName", categoryName);
 } catch (org.hibernate.LazyInitializationException e) {
 txMap.put("categoryName", "Khác");
 }
 String walletName = tx.getWallet() != null ? tx.getWallet().getName() : "Không xác định";
 txMap.put("walletName", walletName);
 return txMap;
 })
 .collect(Collectors.toList());
 
 dashboard.put("totalIncome", totalIncome);
 dashboard.put("totalExpense", totalExpense);
 dashboard.put("netIncome", totalIncome.subtract(totalExpense));
 dashboard.put("totalBalance", totalBalance); // Tổng số dư từ các ví
 dashboard.put("recentTransactions", formattedTransactions);
 dashboard.put("wallets", wallets);
 
 // Thêm dữ liệu cho biểu đồ
 dashboard.put("expensesByCategory", getExpensesByCategoryByDate(userId, dateFrom, dateTo));
 dashboard.put("spendingTrend", getWeeklyTrendByDate(userId, dateFrom, dateTo));
 
 // Tích hợp dữ liệu thực từ BudgetService và GoalService
 try {
 // Lấy tiến độ ngân sách theo khoảng thời gian
 List<Map<String, Object>> budgetProgress = budgetService.getBudgetVsActualByDate(userId, dateFrom, dateTo);
 dashboard.put("budgetProgress", budgetProgress);
 
 // Tính tổng ngân sách và số tiền đã sử dụng cho dashboard từ budgetProgress
 BigDecimal totalBudgetAmount = BigDecimal.ZERO;
 BigDecimal totalBudgetSpent = BigDecimal.ZERO;
 
 for (Map<String, Object> budget : budgetProgress) {
 BigDecimal budgetAmount = (BigDecimal) budget.get("budgetAmount");
 BigDecimal spentAmount = (BigDecimal) budget.get("spentAmount");
 
 if (budgetAmount != null) {
 totalBudgetAmount = totalBudgetAmount.add(budgetAmount);
 }
 if (spentAmount != null) {
 totalBudgetSpent = totalBudgetSpent.add(spentAmount);
 }
 }
 
 log.info("Budget totals: totalAmount={}, totalSpent={}", totalBudgetAmount, totalBudgetSpent);
 
 // Tính phần trăm sử dụng ngân sách tổng
 BigDecimal budgetUsagePercent = BigDecimal.ZERO;
 if (totalBudgetAmount.compareTo(BigDecimal.ZERO) > 0) {
 budgetUsagePercent = totalBudgetSpent.divide(totalBudgetAmount, 4, java.math.RoundingMode.HALF_UP)
 .multiply(BigDecimal.valueOf(100));
 }
 
 // Thêm thông tin ngân sách tổng vào dashboard
 Map<String, Object> totalBudgetInfo = new HashMap<>();
 totalBudgetInfo.put("totalBudgetAmount", totalBudgetAmount);
 totalBudgetInfo.put("totalBudgetSpent", totalBudgetSpent);
 totalBudgetInfo.put("budgetUsagePercent", budgetUsagePercent.doubleValue());
 dashboard.put("totalBudgetInfo", totalBudgetInfo);
 
 log.info("Total budget calculated: Amount={}, Spent={}, Usage={}%", 
 totalBudgetAmount, totalBudgetSpent, budgetUsagePercent);
 
 // Lấy cảnh báo ngân sách từ budgetProgress (lọc những cái có WARNING hoặc EXCEEDED)
 List<Map<String, Object>> budgetWarnings = budgetProgress.stream()
 .filter(budget -> {
 String status = (String) budget.get("status");
 return "WARNING".equals(status) || "EXCEEDED".equals(status);
 })
 .toList();
 dashboard.put("budgetWarnings", budgetWarnings);
 
 log.info("Budget data integrated: {} budgets, {} warnings", budgetProgress.size(), budgetWarnings.size());
 } catch (Exception e) {
 log.warn("Failed to get budget data: {}", e.getMessage());
 dashboard.put("budgetProgress", new ArrayList<>());
 dashboard.put("budgetWarnings", new ArrayList<>());
 
 // Thêm thông tin ngân sách mặc định
 Map<String, Object> totalBudgetInfo = new HashMap<>();
 totalBudgetInfo.put("totalBudgetAmount", BigDecimal.ZERO);
 totalBudgetInfo.put("totalBudgetSpent", BigDecimal.ZERO);
 totalBudgetInfo.put("budgetUsagePercent", 0.0);
 dashboard.put("totalBudgetInfo", totalBudgetInfo);
 }
 
 try {
 // Lấy tiến độ mục tiêu
 log.info("=== GETTING GOAL PROGRESS FOR USER: {} ===", userId);
 List<Map<String, Object>> goalProgress = goalService.getGoalProgress(userId);
 log.info("Goal progress data: {}", goalProgress);
 dashboard.put("goalProgress", goalProgress);
 dashboard.put("activeGoalsCount", (long) goalProgress.size());
 
 log.info("Goal data integrated: {} active goals", goalProgress.size());
 } catch (Exception e) {
 log.error("Failed to get goal data for user {}: {}", userId, e.getMessage(), e);
 dashboard.put("goalProgress", new ArrayList<>());
 dashboard.put("activeGoalsCount", 0L);
 }
 
 log.info("Dashboard data by date calculated successfully for user: {} - Income: {}, Expense: {}, Calculated Balance: {}", 
 userId, totalIncome, totalExpense, totalBalance);
 
 } catch (Exception e) {
 log.error("Error calculating dashboard data by date for user: {}", userId, e);
 // Trả về dữ liệu mặc định nếu có lỗi
 dashboard.put("totalIncome", BigDecimal.ZERO);
 dashboard.put("totalExpense", BigDecimal.ZERO);
 dashboard.put("netIncome", BigDecimal.ZERO);
 dashboard.put("totalBalance", BigDecimal.ZERO);
 dashboard.put("recentTransactions", new ArrayList<>());
 dashboard.put("wallets", new ArrayList<>());
 dashboard.put("expensesByCategory", new ArrayList<>());
 dashboard.put("spendingTrend", new ArrayList<>());
 dashboard.put("budgetProgress", new ArrayList<>());
 dashboard.put("budgetWarnings", new ArrayList<>());
 dashboard.put("goalProgress", new ArrayList<>());
 dashboard.put("activeGoalsCount", 0L);
 
 // Thêm thông tin ngân sách mặc định
 Map<String, Object> totalBudgetInfo = new HashMap<>();
 totalBudgetInfo.put("totalBudgetAmount", BigDecimal.ZERO);
 totalBudgetInfo.put("totalBudgetSpent", BigDecimal.ZERO);
 totalBudgetInfo.put("budgetUsagePercent", 0.0);
 dashboard.put("totalBudgetInfo", totalBudgetInfo);
 }
 
 return dashboard;
 }

 /**
 * Lấy dữ liệu chi tiêu theo category cho biểu đồ tròn
 */
 private List<Map<String, Object>> getExpensesByCategory(Long userId, Integer month, Integer year) {
 try {
 List<Object[]> results = transactionRepository.findExpensesByCategory(userId, month, year);
 return results.stream().map(result -> {
 String categoryName = (String) result[0];
 String categoryColor = (String) result[1];
 
 // Sử dụng CategoryColorService để đảm bảo màu không trùng lặp
 String finalColor = categoryColorService.getCategoryColor(categoryName);
 
 Map<String, Object> map = new HashMap<>();
 map.put("categoryName", categoryName);
 map.put("categoryColor", finalColor);
 map.put("totalAmount", result[2]);
 map.put("transactionCount", result[3]);
 
 log.info("🎨 Category: {}, Original Color: {}, Final Color: {}", 
 categoryName, categoryColor, finalColor);
 
 return map;
 }).collect(Collectors.toList());
 } catch (Exception e) {
 log.error("Error getting expenses by category for user: {}", userId, e);
 return new ArrayList<>();
 }
 }

 /**
 * Lấy dữ liệu chi tiêu theo category theo date range
 */
 private List<Map<String, Object>> getExpensesByCategoryByDate(Long userId, LocalDate dateFrom, LocalDate dateTo) {
 try {
 List<Object[]> results = transactionRepository.findExpensesByCategoryByDate(userId, dateFrom, dateTo);
 return results.stream().map(result -> {
 String categoryName = (String) result[0];
 String categoryColor = (String) result[1];
 
 // Sử dụng CategoryColorService để đảm bảo màu không trùng lặp
 String finalColor = categoryColorService.getCategoryColor(categoryName);
 
 Map<String, Object> map = new HashMap<>();
 map.put("categoryName", categoryName);
 map.put("categoryColor", finalColor);
 map.put("totalAmount", result[2]);
 map.put("transactionCount", result[3]);
 
 log.info("🎨 Category by date: {}, Original Color: {}, Final Color: {}", 
 categoryName, categoryColor, finalColor);
 
 return map;
 }).collect(Collectors.toList());
 } catch (Exception e) {
 log.error("Error getting expenses by category by date for user: {}", userId, e);
 return new ArrayList<>();
 }
 }

 /**
 * Lấy dữ liệu xu hướng theo tuần trong tháng
 */
 private List<Map<String, Object>> getWeeklyTrend(Long userId, Integer month, Integer year) {
 try {
 List<Map<String, Object>> trend = new ArrayList<>();
 YearMonth yearMonth = YearMonth.of(year, month);
 
 // Lấy tất cả giao dịch trong tháng
 List<Transaction> monthTransactions = transactionRepository.findByUserIdAndMonthAndYear(userId, month, year);
 
 // Chia tháng thành 4 tuần
 int daysInMonth = yearMonth.lengthOfMonth();
 int weekSize = (int) Math.ceil(daysInMonth / 4.0);
 
 for (int week = 0; week < 4; week++) {
 int weekStart = week * weekSize + 1;
 int weekEnd = Math.min((week + 1) * weekSize, daysInMonth);
 
 LocalDate weekStartDate = LocalDate.of(year, month, weekStart);
 LocalDate weekEndDate = LocalDate.of(year, month, weekEnd);
 
 // Tính thu chi theo tuần từ transactions
 BigDecimal weeklyIncome = monthTransactions.stream()
 .filter(t -> "income".equals(t.getType()) && 
 !t.getDate().isBefore(weekStartDate) && 
 !t.getDate().isAfter(weekEndDate))
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 BigDecimal weeklyExpense = monthTransactions.stream()
 .filter(t -> "expense".equals(t.getType()) && 
 !t.getDate().isBefore(weekStartDate) && 
 !t.getDate().isAfter(weekEndDate))
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 Map<String, Object> weekData = new HashMap<>();
 weekData.put("period", "Tuần " + (week + 1));
 weekData.put("income", weeklyIncome);
 weekData.put("expense", weeklyExpense);
 weekData.put("net", weeklyIncome.subtract(weeklyExpense));
 
 trend.add(weekData);
 }
 
 log.info("Weekly trend calculated for user {} in {}/{}: {} weeks", userId, month, year, trend.size());
 return trend;
 
 } catch (Exception e) {
 log.error("Error getting weekly trend for user: {}", userId, e);
 return new ArrayList<>();
 }
 }

 /**
 * Lấy dữ liệu xu hướng theo tuần theo date range
 */
 private List<Map<String, Object>> getWeeklyTrendByDate(Long userId, LocalDate dateFrom, LocalDate dateTo) {
 try {
 List<Map<String, Object>> trend = new ArrayList<>();
 
 // Lấy tất cả giao dịch trong khoảng thời gian
 List<Transaction> rangeTransactions = transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(userId, dateFrom, dateTo);
 
 // Tính số tuần trong khoảng thời gian
 long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dateFrom, dateTo);
 int weeks = (int) Math.ceil(daysBetween / 7.0);
 
 for (int week = 0; week < weeks; week++) {
 LocalDate weekStartDate = dateFrom.plusDays(week * 7);
 LocalDate weekEndDate = weekStartDate.plusDays(6);
 if (weekEndDate.isAfter(dateTo)) {
 weekEndDate = dateTo;
 }
 
 // Tạo biến final cho lambda
 final LocalDate finalWeekStartDate = weekStartDate;
 final LocalDate finalWeekEndDate = weekEndDate;
 
 // Tính thu chi theo tuần từ transactions
 BigDecimal weeklyIncome = rangeTransactions.stream()
 .filter(t -> "income".equals(t.getType()) && 
 !t.getDate().isBefore(finalWeekStartDate) && 
 !t.getDate().isAfter(finalWeekEndDate))
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 BigDecimal weeklyExpense = rangeTransactions.stream()
 .filter(t -> "expense".equals(t.getType()) && 
 !t.getDate().isBefore(finalWeekStartDate) && 
 !t.getDate().isAfter(finalWeekEndDate))
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 Map<String, Object> weekData = new HashMap<>();
 weekData.put("period", "Tuần " + (week + 1) + " (" + weekStartDate.getDayOfMonth() + "/" + weekStartDate.getMonthValue() + ")");
 weekData.put("income", weeklyIncome);
 weekData.put("expense", weeklyExpense);
 weekData.put("amount", weeklyExpense); // Thêm key "amount" để phù hợp với frontend
 weekData.put("net", weeklyIncome.subtract(weeklyExpense));
 
 trend.add(weekData);
 }
 
 log.info("Weekly trend by date calculated for user {} from {} to {}: {} weeks", userId, dateFrom, dateTo, trend.size());
 return trend;
 } catch (Exception e) {
 log.error("Error getting weekly trend by date for user: {}", userId, e);
 return new ArrayList<>();
 }
 }

 /**
  * Lấy tổng số dư hiện tại từ tất cả các ví của user
  * Không phụ thuộc vào khoảng ngày - luôn trả về số dư thực tế
  */
 public Map<String, Object> getCurrentWalletBalance(Long userId) {
 Map<String, Object> result = new HashMap<>();
 
 try {
 // Lấy tất cả ví của user
 List<Wallet> wallets = walletRepository.findByUserId(userId);
 log.info("Found {} wallets for user {}", wallets.size(), userId);
 
 // Tính tổng số dư từ tất cả ví
 BigDecimal totalBalance = wallets.stream()
 .map(Wallet::getBalance)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 log.info("Current total balance for user {}: {}", userId, totalBalance);
 
 result.put("totalBalance", totalBalance);
 result.put("walletCount", wallets.size());
 result.put("wallets", wallets.stream()
 .map(w -> {
 Map<String, Object> walletMap = new HashMap<>();
 walletMap.put("id", w.getId());
 walletMap.put("name", w.getName());
 walletMap.put("balance", w.getBalance());
 walletMap.put("type", w.getType());
 return walletMap;
 })
 .collect(Collectors.toList()));
 result.put("success", true);
 
 } catch (Exception e) {
 log.error("Error getting current wallet balance for user {}: {}", userId, e.getMessage(), e);
 result.put("totalBalance", BigDecimal.ZERO);
 result.put("walletCount", 0);
 result.put("wallets", new ArrayList<>());
 result.put("success", false);
 result.put("error", e.getMessage());
 }
 
 return result;
 }
}

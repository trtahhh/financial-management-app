package com.example.finance.controller;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.Wallet;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Goal;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.WalletRepository;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8081"})
public class DashboardController {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @GetMapping("/data")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            HttpServletRequest request) {
        
        try {
            // THAM THỜI: Hardcode userId = 1 để test
            Long userId = 1L;
            
            // TODO: Sau này sẽ lấy từ session
            // Object userIdObj = request.getSession().getAttribute("userId");
            // if (userIdObj == null) {
            //     Map<String, Object> errorResponse = new HashMap<>();
            //     errorResponse.put("error", "User not authenticated");
            //     return ResponseEntity.status(401).body(errorResponse);
            // }
            // Long userId = (Long) userIdObj;
            
            // Validate and clean parameters
            if (dateFrom != null && dateFrom.equals("undefined")) dateFrom = null;
            if (dateTo != null && dateTo.equals("undefined")) dateTo = null;
            
            // Nếu không truyền month/year, sử dụng tháng hiện tại
            LocalDate startDate, endDate;
            
            if (dateFrom != null && !dateFrom.isEmpty() && dateTo != null && !dateTo.isEmpty()) {
                // Sử dụng khoảng ngày được chỉ định
                startDate = LocalDate.parse(dateFrom);
                endDate = LocalDate.parse(dateTo);
                month = startDate.getMonthValue(); // Để hiển thị
                year = startDate.getYear();
                System.out.println("🔍 DEBUG: Using date range: " + startDate + " to " + endDate);
            } else {
                // Sử dụng tháng/năm
                if (month == null || year == null) {
                    LocalDate now = LocalDate.now();
                    month = now.getMonthValue();
                    year = now.getYear();
                }
                startDate = LocalDate.of(year, month, 1);
                endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
                System.out.println("🔍 DEBUG: Using month/year: " + month + "/" + year + " (" + startDate + " to " + endDate + ")");
            }
            
            Map<String, Object> dashboardData = new HashMap<>();
            
            // 1. Tổng số dư từ tất cả ví (tính dựa trên giao dịch: Thu nhập - Chi tiêu)
            BigDecimal totalIncome = transactionRepository.sumByUserIdAndType(userId, "income");
            BigDecimal totalExpense = transactionRepository.sumByUserIdAndType(userId, "expense");
            totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
            totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
            BigDecimal totalBalance = totalIncome.subtract(totalExpense);
            dashboardData.put("totalBalance", totalBalance.doubleValue());
            
            // 2. Thu nhập và chi tiêu theo khoảng thời gian
            BigDecimal periodIncome, periodExpense;
            
            if (dateFrom != null && !dateFrom.isEmpty() && dateTo != null && !dateTo.isEmpty()) {
                System.out.println("🔍 DEBUG: Calculating for date range: " + startDate + " to " + endDate);
                
                // Sử dụng khoảng ngày để tính tổng
                List<Transaction> incomeTransactions = transactionRepository
                    .findByUserIdAndTypeAndDateBetween(userId, "income", startDate, endDate);
                List<Transaction> expenseTransactions = transactionRepository
                    .findByUserIdAndTypeAndDateBetween(userId, "expense", startDate, endDate);
                
                System.out.println("🔍 DEBUG: Found " + incomeTransactions.size() + " income transactions");
                System.out.println("🔍 DEBUG: Found " + expenseTransactions.size() + " expense transactions");
                
                // Debug: in ra từng transaction
                incomeTransactions.forEach(t -> 
                    System.out.println("  Income: " + t.getDate() + " - " + t.getAmount() + " - " + t.getNote()));
                expenseTransactions.forEach(t -> 
                    System.out.println("  Expense: " + t.getDate() + " - " + t.getAmount() + " - " + t.getNote()));
                
                periodIncome = incomeTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                periodExpense = expenseTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                System.out.println("🔍 DEBUG: Calculated periodIncome: " + periodIncome);
                System.out.println("🔍 DEBUG: Calculated periodExpense: " + periodExpense);
            } else {
                // Sử dụng tháng/năm
                periodIncome = transactionRepository.sumAmountByUserAndType(userId, "income", month, year);
                periodExpense = transactionRepository.sumAmountByUserAndType(userId, "expense", month, year);
                System.out.println("🔍 DEBUG: Using month/year calculation - Income: " + periodIncome + ", Expense: " + periodExpense);
            }
            
            dashboardData.put("monthlyIncome", periodIncome != null ? periodIncome.doubleValue() : 0.0);
            dashboardData.put("monthlyExpense", periodExpense != null ? periodExpense.doubleValue() : 0.0);
            
            // 3. Lấy transactions để phân tích category  
            List<Transaction> monthlyTransactions = transactionRepository
                .findByUserIdAndTypeAndDateBetween(userId, "expense", startDate, endDate);
            
            // 4. Group by category để tạo pie chart data
            Map<String, Double> categoryExpenses = monthlyTransactions.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                    t -> t.getCategory().getName(),
                    Collectors.summingDouble(t -> t.getAmount().doubleValue())
                ));
            
            dashboardData.put("categoryExpenses", categoryExpenses);
            
            // 5. Thông tin tháng hiện tại
            dashboardData.put("currentMonth", month);
            dashboardData.put("currentYear", year);
            
            // 6. Danh sách ví để hiển thị
            List<Wallet> userWallets = walletRepository.findByUserId(userId);
            List<Map<String, Object>> walletData = userWallets.stream()
                .map(w -> {
                    Map<String, Object> walletInfo = new HashMap<>();
                    walletInfo.put("id", w.getId());
                    walletInfo.put("name", w.getName());
                    walletInfo.put("balance", w.getBalance() != null ? w.getBalance().doubleValue() : 0.0);
                    walletInfo.put("type", w.getType());
                    return walletInfo;
                })
                .collect(Collectors.toList());
            
            dashboardData.put("wallets", walletData);
            
            // 7. Giao dịch gần đây (tất cả loại, sắp xếp theo ngày mới nhất)
            System.out.println("🔍 DEBUG: Fetching transactions for userId=" + userId + " in date range " + startDate + " to " + endDate);
            
            // Filter transactions by date range
            List<Transaction> allUserTransactions = transactionRepository.findAllWithDetails()
                .stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .filter(t -> !t.isDeleted())
                .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
                .collect(Collectors.toList());
            
            System.out.println("🔍 DEBUG: Found " + allUserTransactions.size() + " transactions");
            
            List<Map<String, Object>> transactionData = allUserTransactions.stream()
                .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate())) // Sắp xếp theo ngày mới nhất
                .limit(10) // Chỉ lấy 10 giao dịch gần nhất
                .map(t -> {
                    Map<String, Object> txInfo = new HashMap<>();
                    txInfo.put("id", t.getId());
                    txInfo.put("amount", t.getAmount() != null ? t.getAmount().doubleValue() : 0.0);
                    txInfo.put("type", t.getType());
                    txInfo.put("note", t.getNote());
                    txInfo.put("date", t.getDate().toString());
                    txInfo.put("categoryName", t.getCategory() != null ? t.getCategory().getName() : "Không phân loại");
                    txInfo.put("walletName", t.getWallet() != null ? t.getWallet().getName() : "Không xác định");
                    System.out.println("🔍 DEBUG: Transaction - " + t.getNote() + " (" + t.getDate() + ")");
                    return txInfo;
                })
                .collect(Collectors.toList());
            
            System.out.println("🔍 DEBUG: Processed " + transactionData.size() + " transaction data objects");
            dashboardData.put("transactions", transactionData);
            
            // 8. Cảnh báo ngân sách
            List<Map<String, Object>> budgetWarnings = checkBudgetWarnings(userId, month, year, allUserTransactions);
            dashboardData.put("budgetWarnings", budgetWarnings);
            
            // 9. Tiến độ mục tiêu tài chính
            List<Map<String, Object>> goalProgress = calculateGoalProgress(userId, totalBalance);
            dashboardData.put("goalProgress", goalProgress);
            
            // 10. Thêm thông tin về khoảng thời gian
            dashboardData.put("dateFrom", startDate.toString());
            dashboardData.put("dateTo", endDate.toString());
            
            return ResponseEntity.ok(dashboardData);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Kiểm tra cảnh báo ngân sách
     */
    private List<Map<String, Object>> checkBudgetWarnings(Long userId, Integer month, Integer year, List<Transaction> monthlyTransactions) {
        List<Map<String, Object>> warnings = new ArrayList<>();
        
        try {
            // Lấy tất cả ngân sách của user trong tháng/năm này
            List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
            System.out.println("🔍 DEBUG: Found " + budgets.size() + " budgets for month " + month + "/" + year);
            
            for (Budget budget : budgets) {
                if (budget.getIsDeleted()) continue;
                
                // Tính tổng chi tiêu của category này trong tháng
                BigDecimal totalSpent = monthlyTransactions.stream()
                    .filter(t -> t.getType().equals("expense"))
                    .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(budget.getCategory().getId()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                System.out.println("🔍 DEBUG: Category " + budget.getCategory().getId() + " - Budget: " + budget.getAmount() + ", Spent: " + totalSpent);
                
                // Tính phần trăm sử dụng
                double percentage = budget.getAmount().compareTo(BigDecimal.ZERO) > 0 ? 
                    totalSpent.divide(budget.getAmount(), 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;
                
                // Cảnh báo nếu vượt quá 80% hoặc 100%
                if (percentage >= 80) {
                    Map<String, Object> warning = new HashMap<>();
                    warning.put("categoryId", budget.getCategory().getId());
                    warning.put("categoryName", budget.getCategory() != null ? budget.getCategory().getName() : "Không xác định");
                    warning.put("budgetAmount", budget.getAmount().doubleValue());
                    warning.put("spentAmount", totalSpent.doubleValue());
                    warning.put("percentage", Math.round(percentage * 100.0) / 100.0);
                    warning.put("status", percentage >= 100 ? "exceeded" : "warning");
                    warning.put("message", percentage >= 100 ? "Đã vượt ngân sách!" : "Sắp vượt ngân sách!");
                    
                    warnings.add(warning);
                    System.out.println("🚨 DEBUG: Budget warning - " + warning.get("categoryName") + ": " + percentage + "%");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error checking budget warnings: " + e.getMessage());
            e.printStackTrace();
        }
        
        return warnings;
    }
    
    /**
     * Tính tiến độ mục tiêu tài chính dựa trên current_amount của từng mục tiêu
     */
    private List<Map<String, Object>> calculateGoalProgress(Long userId, BigDecimal currentBalance) {
        List<Map<String, Object>> goalProgressList = new ArrayList<>();
        
        try {
            // Lấy tất cả mục tiêu đang hoạt động của user
            List<Goal> activeGoals = goalRepository.findByUserIdAndIsDeletedFalse(userId);
            System.out.println("🎯 DEBUG: Found " + activeGoals.size() + " active goals for user " + userId + " with balance: " + currentBalance);
            
            for (Goal goal : activeGoals) {
                Map<String, Object> goalData = new HashMap<>();
                goalData.put("goalId", goal.getId());
                goalData.put("goalName", goal.getName());
                goalData.put("targetAmount", goal.getTargetAmount());
                
                // Sử dụng currentAmount từ goal entity thay vì currentBalance
                BigDecimal currentAmount = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
                goalData.put("currentAmount", currentAmount);
                goalData.put("currentBalance", currentBalance); // Vẫn giữ để hiển thị thông tin
                
                // Tính phần trăm tiến độ dựa trên currentAmount của mục tiêu
                BigDecimal progressPercentage = BigDecimal.ZERO;
                if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                    progressPercentage = currentAmount
                        .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                }
                
                goalData.put("progressPercentage", progressPercentage);
                
                // Xác định trạng thái dựa trên currentAmount
                String status = "in-progress";
                if (currentAmount.compareTo(goal.getTargetAmount()) >= 0) {
                    status = "completed";
                } else if (progressPercentage.compareTo(new BigDecimal("80")) >= 0) {
                    status = "near-completion";
                }
                goalData.put("status", status);
                
                // Tính số tiền còn thiếu dựa trên currentAmount
                BigDecimal remainingAmount = goal.getTargetAmount().subtract(currentAmount);
                if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
                    remainingAmount = BigDecimal.ZERO;
                }
                goalData.put("remainingAmount", remainingAmount);
                
                System.out.println("🎯 DEBUG: Goal '" + goal.getName() + "' - Target: " + goal.getTargetAmount() + 
                                 ", Current: " + currentAmount + ", Progress: " + progressPercentage + "%, Status: " + status);
                
                goalProgressList.add(goalData);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error calculating goal progress: " + e.getMessage());
            e.printStackTrace();
        }
        
        return goalProgressList;
    }
}

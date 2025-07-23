package com.example.finance.service;

import com.example.finance.entity.User;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.WalletRepository;
import com.example.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Get dashboard data
    public Map<String, Object> getDashboardData(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        
        // Get financial summary for current month
        BigDecimal monthlyIncome = transactionRepository.getTotalIncomeByUserAndDateRange(user, startOfMonth, endOfMonth);
        BigDecimal monthlyExpense = transactionRepository.getTotalExpenseByUserAndDateRange(user, startOfMonth, endOfMonth);
        BigDecimal monthlyBalance = monthlyIncome.subtract(monthlyExpense);
        
        // Get recent transactions
        List<Object[]> recentTransactions = transactionRepository.findTop10ByUserAndIsDeletedFalseOrderByTransactionDateDesc(user)
                .stream()
                .map(transaction -> new Object[]{
                    transaction.getId(),
                    transaction.getTransType(),
                    transaction.getAmount(),
                    transaction.getDescription(),
                    transaction.getTransactionDate(),
                    transaction.getCategory().getName(),
                    transaction.getWallet().getName()
                })
                .toList();
        
        // Get category totals for current month
        List<Object[]> categoryTotals = transactionRepository.getTotalAmountByCategoryForUserAndDateRange(user, startOfMonth, endOfMonth);
        
        // Get budget summary
        BigDecimal totalBudget = budgetRepository.getTotalBudgetAmountByUser(user);
        Long activeBudgetCount = budgetRepository.countActiveBudgetsByUser(user);
        
        // Get wallet summary
        Double totalBalance = walletRepository.getTotalBalanceByUser(user);
        Long walletCount = walletRepository.countWalletsByUser(user);
        
        // Get type totals for current month
        List<Object[]> typeTotals = transactionRepository.getTotalAmountByTypeForUserAndDateRange(user, startOfMonth, endOfMonth);
        
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("monthlyIncome", monthlyIncome);
        dashboardData.put("monthlyExpense", monthlyExpense);
        dashboardData.put("monthlyBalance", monthlyBalance);
        dashboardData.put("recentTransactions", recentTransactions);
        dashboardData.put("categoryTotals", categoryTotals);
        dashboardData.put("totalBudget", totalBudget);
        dashboardData.put("activeBudgetCount", activeBudgetCount);
        dashboardData.put("totalBalance", totalBalance);
        dashboardData.put("walletCount", walletCount);
        dashboardData.put("typeTotals", typeTotals);
        
        return dashboardData;
    }
    
    // Get financial summary for date range
    public Map<String, Object> getFinancialSummary(String username, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        BigDecimal totalIncome = transactionRepository.getTotalIncomeByUserAndDateRange(user, startDate, endDate);
        BigDecimal totalExpense = transactionRepository.getTotalExpenseByUserAndDateRange(user, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);
        
        List<Object[]> categoryTotals = transactionRepository.getTotalAmountByCategoryForUserAndDateRange(user, startDate, endDate);
        List<Object[]> typeTotals = transactionRepository.getTotalAmountByTypeForUserAndDateRange(user, startDate, endDate);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("balance", balance);
        summary.put("categoryTotals", categoryTotals);
        summary.put("typeTotals", typeTotals);
        
        return summary;
    }
    
    // Get monthly statistics
    public Map<String, Object> getMonthlyStatistics(String username, int year, int month) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        BigDecimal monthlyIncome = transactionRepository.getTotalIncomeByUserAndDateRange(user, startDate, endDate);
        BigDecimal monthlyExpense = transactionRepository.getTotalExpenseByUserAndDateRange(user, startDate, endDate);
        BigDecimal monthlyBalance = monthlyIncome.subtract(monthlyExpense);
        
        List<Object[]> categoryTotals = transactionRepository.getTotalAmountByCategoryForUserAndDateRange(user, startDate, endDate);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("year", year);
        statistics.put("month", month);
        statistics.put("income", monthlyIncome);
        statistics.put("expense", monthlyExpense);
        statistics.put("balance", monthlyBalance);
        statistics.put("categoryTotals", categoryTotals);
        
        return statistics;
    }
    
    // Get yearly statistics
    public Map<String, Object> getYearlyStatistics(String username, int year) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        
        BigDecimal yearlyIncome = transactionRepository.getTotalIncomeByUserAndDateRange(user, startDate, endDate);
        BigDecimal yearlyExpense = transactionRepository.getTotalExpenseByUserAndDateRange(user, startDate, endDate);
        BigDecimal yearlyBalance = yearlyIncome.subtract(yearlyExpense);
        
        List<Object[]> categoryTotals = transactionRepository.getTotalAmountByCategoryForUserAndDateRange(user, startDate, endDate);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("year", year);
        statistics.put("income", yearlyIncome);
        statistics.put("expense", yearlyExpense);
        statistics.put("balance", yearlyBalance);
        statistics.put("categoryTotals", categoryTotals);
        
        return statistics;
    }
} 
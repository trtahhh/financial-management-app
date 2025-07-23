package com.example.finance.controller;

import com.example.finance.entity.User;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Autowired 
    UserRepository userRepo;
    
    @Autowired
    TransactionRepository transactionRepo;
    
    @Autowired
    BudgetRepository budgetRepo;

    // Lấy dữ liệu dashboard
    @GetMapping
    public ResponseEntity<?> getDashboardData() {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        try {
            // Tính toán tháng hiện tại
            LocalDate now = LocalDate.now();
            LocalDate startOfMonth = now.withDayOfMonth(1);
            LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
            
            // Lấy tổng thu nhập tháng này
            BigDecimal monthlyIncome = transactionRepo.getTotalIncomeByUserAndDateRange(user, startOfMonth, endOfMonth);
            
            // Lấy tổng chi tiêu tháng này
            BigDecimal monthlyExpense = transactionRepo.getTotalExpenseByUserAndDateRange(user, startOfMonth, endOfMonth);
            
            // Lấy tổng theo danh mục
            List<Object[]> categoryTotals = transactionRepo.getTotalAmountByCategoryForUserAndDateRange(user, startOfMonth, endOfMonth);
            
            // Lấy giao dịch gần đây
            List<Object[]> recentTransactions = transactionRepo.findTop10ByUserAndIsDeletedFalseOrderByTransactionDateDesc(user)
                .stream()
                .map(t -> new Object[]{
                    t.getId(),
                    t.getTransType(),
                    t.getAmount(),
                    t.getTransactionDate(),
                    t.getDescription(),
                    t.getCategory() != null ? t.getCategory().getName() : "Khác"
                })
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("monthlyIncome", monthlyIncome);
            response.put("monthlyExpense", monthlyExpense);
            response.put("categoryTotals", categoryTotals);
            response.put("recentTransactions", recentTransactions);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tải dữ liệu dashboard: " + e.getMessage()));
        }
    }

    private String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
} 
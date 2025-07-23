package com.example.finance.controller;

import com.example.finance.entity.Budget;
import com.example.finance.entity.User;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.TransactionRepository;
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
@RequestMapping("/api/budgets")
public class BudgetController {
    @Autowired 
    BudgetRepository budgetRepo;
    
    @Autowired
    UserRepository userRepo;
    
    @Autowired
    TransactionRepository transactionRepo;

    // Lấy danh sách ngân sách
    @GetMapping
    public ResponseEntity<?> getBudgets() {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        try {
            List<Budget> budgets = budgetRepo.findByUserAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(user, "active");
            
            // Tính toán số tiền đã chi cho mỗi ngân sách
            List<Map<String, Object>> budgetsWithSpent = budgets.stream().map(budget -> {
                Map<String, Object> budgetMap = new HashMap<>();
                budgetMap.put("id", budget.getId());
                budgetMap.put("name", budget.getName());
                budgetMap.put("total", budget.getTotal());
                budgetMap.put("startDate", budget.getStartDate());
                budgetMap.put("endDate", budget.getEndDate());
                budgetMap.put("status", budget.getStatus());
                budgetMap.put("createdAt", budget.getCreatedAt());
                
                // Tính số tiền đã chi trong khoảng thời gian của ngân sách
                BigDecimal spent = transactionRepo.getTotalExpenseByUserAndDateRange(
                    user, budget.getStartDate(), budget.getEndDate());
                budgetMap.put("spent", spent);
                
                return budgetMap;
            }).toList();
            
            return ResponseEntity.ok(budgetsWithSpent);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tải ngân sách: " + e.getMessage()));
        }
    }

    // Lấy ngân sách theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getBudget(@PathVariable Long id) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        Budget budget = budgetRepo.findById(id).orElse(null);
        if (budget == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu
        if (!budget.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        return ResponseEntity.ok(budget);
    }

    // Tạo ngân sách mới
    @PostMapping
    public ResponseEntity<?> createBudget(@RequestBody Map<String, Object> request) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        try {
            Budget budget = new Budget();
            budget.setUser(user);
            budget.setName((String) request.get("name"));
            budget.setTotal(new BigDecimal(request.get("total").toString()));
            budget.setStartDate(LocalDate.parse((String) request.get("startDate")));
            budget.setEndDate(LocalDate.parse((String) request.get("endDate")));
            budget.setStatus("active");
            
            Budget savedBudget = budgetRepo.save(budget);
            return ResponseEntity.ok(savedBudget);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tạo ngân sách: " + e.getMessage()));
        }
    }

    // Cập nhật ngân sách
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBudget(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        Budget budget = budgetRepo.findById(id).orElse(null);
        if (budget == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu
        if (!budget.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        try {
            if (request.get("name") != null) {
                budget.setName((String) request.get("name"));
            }
            if (request.get("total") != null) {
                budget.setTotal(new BigDecimal(request.get("total").toString()));
            }
            if (request.get("startDate") != null) {
                budget.setStartDate(LocalDate.parse((String) request.get("startDate")));
            }
            if (request.get("endDate") != null) {
                budget.setEndDate(LocalDate.parse((String) request.get("endDate")));
            }
            if (request.get("status") != null) {
                budget.setStatus((String) request.get("status"));
            }
            
            Budget updatedBudget = budgetRepo.save(budget);
            return ResponseEntity.ok(updatedBudget);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi cập nhật ngân sách: " + e.getMessage()));
        }
    }

    // Xóa ngân sách
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(@PathVariable Long id) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        Budget budget = budgetRepo.findById(id).orElse(null);
        if (budget == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu
        if (!budget.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        budgetRepo.delete(budget);
        return ResponseEntity.ok(Map.of("message", "Đã xóa ngân sách thành công"));
    }

    private String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
} 
package com.example.finance.service;

import com.example.finance.dto.BudgetDto;
import com.example.finance.entity.Budget;
import com.example.finance.entity.User;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BudgetService {
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    // Create new budget
    public BudgetDto createBudget(BudgetDto budgetDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setName(budgetDto.getName());
        budget.setTotal(budgetDto.getTotal());
        budget.setStartDate(budgetDto.getStartDate());
        budget.setEndDate(budgetDto.getEndDate());
        budget.setStatus("active");
        budget.setCreatedAt(LocalDateTime.now());
        
        Budget savedBudget = budgetRepository.save(budget);
        
        return BudgetDto.fromEntity(savedBudget);
    }
    
    // Get budget by ID
    public BudgetDto getBudgetById(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        return BudgetDto.fromEntity(budget);
    }
    
    // Calculate spent amount for a budget
    private BigDecimal calculateSpentAmount(Budget budget) {
        // Tính tổng chi tiêu thực tế cho ngân sách này
        return transactionRepository.getTotalExpenseByUserAndDateRange(
            budget.getUser(),
            budget.getStartDate(),
            budget.getEndDate()
        );
    }
    
    // Get all budgets for user with spent amounts
    public List<Map<String, Object>> getUserBudgetsWithSpent(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Budget> budgets = budgetRepository.findByUserAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(user, "active");
        
        return budgets.stream()
                .map(budget -> {
                    BigDecimal spent = calculateSpentAmount(budget);
                    Map<String, Object> budgetMap = new HashMap<>();
                    budgetMap.put("id", budget.getId());
                    budgetMap.put("name", budget.getName());
                    budgetMap.put("total", budget.getTotal());
                    budgetMap.put("spent", spent);
                    budgetMap.put("startDate", budget.getStartDate());
                    budgetMap.put("endDate", budget.getEndDate());
                    budgetMap.put("status", budget.getStatus());
                    return budgetMap;
                })
                .collect(Collectors.toList());
    }
    
    // Get all budgets for user
    public List<BudgetDto> getUserBudgets(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Budget> budgets = budgetRepository.findByUserAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(user, "active");
        
        return budgets.stream()
                .map(BudgetDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get active budgets
    public List<BudgetDto> getActiveBudgets(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate now = LocalDate.now();
        List<Budget> budgets = budgetRepository.findActiveBudgetsByUserAndCurrentDate(user, now);
        
        return budgets.stream()
                .map(BudgetDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get expired budgets
    public List<BudgetDto> getExpiredBudgets(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate now = LocalDate.now();
        List<Budget> budgets = budgetRepository.findExpiredBudgets(user, now);
        
        return budgets.stream()
                .map(BudgetDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get upcoming budgets
    public List<BudgetDto> getUpcomingBudgets(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate now = LocalDate.now();
        List<Budget> budgets = budgetRepository.findUpcomingBudgets(user, now);
        
        return budgets.stream()
                .map(BudgetDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Update budget
    public BudgetDto updateBudget(Long id, BudgetDto budgetDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        budget.setName(budgetDto.getName());
        budget.setTotal(budgetDto.getTotal());
        budget.setStartDate(budgetDto.getStartDate());
        budget.setEndDate(budgetDto.getEndDate());
        budget.setStatus(budgetDto.getStatus());
        
        Budget savedBudget = budgetRepository.save(budget);
        
        return BudgetDto.fromEntity(savedBudget);
    }
    
    // Delete budget (soft delete)
    public void deleteBudget(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        budget.setIsDeleted(true);
        budgetRepository.save(budget);
    }
    
    // Get budget summary
    public Map<String, Object> getBudgetSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        BigDecimal totalBudget = budgetRepository.getTotalBudgetAmountByUser(user);
        Long activeBudgetCount = budgetRepository.countActiveBudgetsByUser(user);
        
        List<Object[]> statusSummary = budgetRepository.getBudgetSummaryByStatus(user);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalBudget", totalBudget);
        summary.put("activeBudgetCount", activeBudgetCount);
        summary.put("statusSummary", statusSummary);
        
        return summary;
    }
    
    // Get current month budgets
    public List<BudgetDto> getCurrentMonthBudgets(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate now = LocalDate.now();
        List<Budget> budgets = budgetRepository.getCurrentMonthBudgets(user, now);
        
        return budgets.stream()
                .map(BudgetDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get current year budgets
    public List<BudgetDto> getCurrentYearBudgets(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate now = LocalDate.now();
        List<Budget> budgets = budgetRepository.getCurrentYearBudgets(user, now);
        
        return budgets.stream()
                .map(BudgetDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get budgets ending soon
    public List<BudgetDto> getBudgetsEndingSoon(String username, int days) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate endDate = LocalDate.now().plusDays(days);
        List<Budget> budgets = budgetRepository.findBudgetsEndingSoon(user, endDate);
        
        return budgets.stream()
                .map(BudgetDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get budget statistics
    public Map<String, Object> getBudgetStatistics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Object[]> statusStatistics = budgetRepository.getBudgetStatisticsByStatus(user);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("statusStatistics", statusStatistics);
        
        return statistics;
    }
} 
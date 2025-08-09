package com.example.finance.service;

import com.example.finance.dto.BudgetDTO;
import com.example.finance.entity.Budget;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;


    @Cacheable("budgets")
    public List<BudgetDTO> getAllBudgets() {
        List<Budget> budgets = budgetRepository.findAllByIsDeletedFalse();
        List<BudgetDTO> budgetDTOs = budgetMapper.toDTOs(budgets);
        
        // Calculate progress for each budget
        for (BudgetDTO budget : budgetDTOs) {
            BigDecimal usedAmount = transactionService.sumByCategoryAndMonth(
                budget.getCategoryId(), budget.getMonth(), budget.getYear());
            
            // Set spent amount
            budget.setSpentAmount(usedAmount);
            
            if (budget.getAmount() != null && budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal progress = usedAmount.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                budget.setProgress(progress.intValue()); // Không giới hạn tối đa để hiển thị vượt ngân sách
            } else {
                budget.setProgress(0);
            }
        }
        
        return budgetDTOs;
    }

    @Cacheable(value = "budgets", key = "#id")
    public BudgetDTO getBudgetById(Long id) {
        Budget budget = budgetRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + id));
        return budgetMapper.toDTO(budget);
    }

    @CacheEvict(value = "budgets", allEntries = true)
    public BudgetDTO createBudget(BudgetDTO dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget amount must be greater than 0");
        }
        if (dto.getCategoryId() == null) {
            throw new IllegalArgumentException("Category is required");
        }
        if (dto.getMonth() <= 0 || dto.getMonth() > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (dto.getYear() <= 0) {
            throw new IllegalArgumentException("Year must be greater than 0");
        }
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("UserId is required");
        }

        Budget budget = budgetMapper.toEntity(dto);
        budget.setCreatedAt(LocalDateTime.now());
        budget.setIsDeleted(false);
        Budget saved = budgetRepository.save(budget);
        return budgetMapper.toDTO(saved);
    }

    @CacheEvict(value = "budgets", allEntries = true)
        public BudgetDTO updateBudget(Long id, BudgetDTO dto) {
            Budget budget = budgetRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + id));

            budget.setAmount(dto.getAmount());
            budget.setMonth(dto.getMonth());
            budget.setYear(dto.getYear());
            // Currency code removed from entity
            budget.setUpdatedAt(LocalDateTime.now());

            var category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            budget.setCategory(category);

            Budget updated = budgetRepository.save(budget);
            return budgetMapper.toDTO(updated);
    }

    @CacheEvict(value = "budgets", allEntries = true)
    public void deleteBudget(Long id) {
        Budget budget = budgetRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + id));
        budget.setIsDeleted(true); // Fixed: should be true to mark as deleted
        budget.setDeletedAt(LocalDateTime.now());
        budgetRepository.save(budget);
    }

    // Thêm các method này vào BudgetService:

    /**
     * Lấy budget vs actual comparison
     */
    public List<Map<String, Object>> getBudgetVsActual(Long userId, Integer month, Integer year) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(userId, month, year);
        
        return budgets.stream().map(budget -> {
            // Tính tổng chi tiêu thực tế
            BigDecimal actualSpent = transactionService.getTotalSpentByCategory(
                    userId, budget.getCategory().getId(), month, year);
            
            if (actualSpent == null) actualSpent = BigDecimal.ZERO;
            
            // Tính phần trăm sử dụng
            BigDecimal usagePercent = budget.getAmount().compareTo(BigDecimal.ZERO) > 0 ? 
                    actualSpent.divide(budget.getAmount(), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
            
            // Xác định status
            String status = "OK";
            if (usagePercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
                status = "EXCEEDED";
            } else if (usagePercent.compareTo(BigDecimal.valueOf(80)) >= 0) {
                status = "WARNING";
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("budgetId", budget.getId());
            result.put("categoryName", budget.getCategory().getName());
            result.put("categoryColor", budget.getCategory().getColor());
            result.put("budgetAmount", budget.getAmount());
            result.put("spentAmount", actualSpent);
            result.put("usagePercent", usagePercent.doubleValue());
            result.put("alertThreshold", 80.0);
            result.put("status", status);
            
            return result;
        }).toList();
    }

    /**
     * Lấy cảnh báo ngân sách
     */
    public List<Map<String, Object>> getBudgetWarnings(Long userId, Integer month, Integer year) {
        List<Map<String, Object>> budgetVsActual = getBudgetVsActual(userId, month, year);
        
        // Chỉ lấy những budget có warning hoặc exceeded
        return budgetVsActual.stream()
                .filter(budget -> {
                    String status = (String) budget.get("status");
                    return "WARNING".equals(status) || "EXCEEDED".equals(status);
                })
                .map(budget -> {
                    Map<String, Object> warning = new HashMap<>();
                    warning.put("categoryId", ((Budget) budget).getCategory().getId());
                    warning.put("categoryName", budget.get("categoryName"));
                    warning.put("budgetAmount", budget.get("budgetAmount"));
                    warning.put("spentAmount", budget.get("spentAmount"));
                    warning.put("percentage", budget.get("usagePercent"));
                    warning.put("status", budget.get("status"));
                    
                    if ("EXCEEDED".equals(budget.get("status"))) {
                        warning.put("message", "Đã vượt ngân sách!");
                    } else {
                        warning.put("message", "Sắp vượt ngân sách!");
                    }
                    
                    return warning;
                })
                .toList();
    }

    /**
     * Đếm số budget đang hoạt động
     */
    public Long countActiveBudgets(Long userId, int month, int year) {
        return (long) budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(userId, month, year).size();
    }

    /**
     * Lấy tiến độ ngân sách trong khoảng ngày (date range)
     */
    public List<Map<String, Object>> getBudgetVsActualByDate(Long userId, LocalDate startDate, LocalDate endDate) {
        int startMonth = startDate.getMonthValue();
        int startYear = startDate.getYear();
        int endMonth = endDate.getMonthValue();
        int endYear = endDate.getYear();
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthYearRangeAndIsDeletedFalse(userId, startMonth, startYear, endMonth, endYear);
        
        return budgets.stream().map(budget -> {
            // Tính tổng chi tiêu thực tế trong khoảng ngày
            BigDecimal actualSpent = transactionService.getTotalSpentByCategoryAndDateRange(
                    userId, budget.getCategory().getId(), startDate, endDate);

            if (actualSpent == null) actualSpent = BigDecimal.ZERO;

            // Tính phần trăm sử dụng
            BigDecimal usagePercent = budget.getAmount().compareTo(BigDecimal.ZERO) > 0 ?
                    actualSpent.divide(budget.getAmount(), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

            // Xác định status
            String status = "OK";
            if (usagePercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
                status = "EXCEEDED";
            } else if (usagePercent.compareTo(BigDecimal.valueOf(80)) >= 0) {
                status = "WARNING";
            }

            Map<String, Object> result = new HashMap<>();
            result.put("budgetId", budget.getId());
            result.put("categoryName", budget.getCategory().getName());
            result.put("categoryColor", budget.getCategory().getColor());
            result.put("budgetAmount", budget.getAmount());
            result.put("spentAmount", actualSpent);
            result.put("usagePercent", usagePercent.doubleValue());
            result.put("alertThreshold", 80.0);
            result.put("status", status);

            return result;
        }).toList();
    }
}

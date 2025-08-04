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
import java.time.LocalDateTime;
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
            
            // Set used amount
            budget.setUsedAmount(usedAmount);
            
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
        // Validation
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
        
        // TEMPORARY: Set userId = 1 for testing
        if (dto.getUserId() == null) {
            dto.setUserId(1L);
        }
        
        Budget budget = budgetMapper.toEntity(dto);
        budget.setCreatedAt(LocalDateTime.now());
        // Explicitly set isDeleted to false to avoid NullPointerException
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
        budget.setCurrencyCode(dto.getCurrencyCode());
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
}

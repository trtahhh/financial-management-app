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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final CategoryRepository categoryRepository;


    @Cacheable("budgets")
    public List<BudgetDTO> getAllBudgets() {
        List<Budget> budgets = budgetRepository.findAllByIsDeletedFalse();
        return budgetMapper.toDTOs(budgets);
    }

    @Cacheable(value = "budgets", key = "#id")
    public BudgetDTO getBudgetById(Long id) {
        Budget budget = budgetRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + id));
        return budgetMapper.toDTO(budget);
    }

    @CacheEvict(value = "budgets", allEntries = true)
    public BudgetDTO createBudget(BudgetDTO dto) {
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
        budget.setIsDeleted(false);
        budget.setDeletedAt(LocalDateTime.now());
        budgetRepository.save(budget);
    }
}

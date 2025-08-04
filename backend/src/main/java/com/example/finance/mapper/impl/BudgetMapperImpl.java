package com.example.finance.mapper.impl;

import com.example.finance.dto.BudgetDTO;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import com.example.finance.mapper.BudgetMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component("budgetMapper")
public class BudgetMapperImpl implements BudgetMapper {

    @Override
    public BudgetDTO toDTO(Budget budget) {
        if (budget == null) {
            return null;
        }

        BudgetDTO dto = new BudgetDTO();
        dto.setId(budget.getId());
        dto.setUserId(budget.getUser() != null ? budget.getUser().getId() : null);
        dto.setCategoryId(budget.getCategory() != null ? budget.getCategory().getId() : null);
        dto.setAmount(budget.getAmount());
        dto.setMonth(budget.getMonth());
        dto.setYear(budget.getYear());
        dto.setIsDeleted(budget.getIsDeleted());
        
        // Initialize default values
        dto.setUsedAmount(BigDecimal.ZERO);
        dto.setCurrencyCode("VND");
        dto.setProgress(0);

        return dto;
    }

    @Override
    public Budget toEntity(BudgetDTO dto) {
        if (dto == null) {
            return null;
        }

        Budget budget = new Budget();
        budget.setId(dto.getId());
        budget.setAmount(dto.getAmount());
        budget.setMonth(dto.getMonth());
        budget.setYear(dto.getYear());
        budget.setIsDeleted(dto.getIsDeleted());

        if (dto.getUserId() != null) {
            User user = new User();
            user.setId(dto.getUserId());
            budget.setUser(user);
        }

        if (dto.getCategoryId() != null) {
            Category category = new Category();
            category.setId(dto.getCategoryId());
            budget.setCategory(category);
        }

        return budget;
    }

    @Override
    public List<BudgetDTO> toDTOs(List<Budget> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

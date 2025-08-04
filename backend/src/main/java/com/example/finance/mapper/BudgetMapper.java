package com.example.finance.mapper;

import com.example.finance.dto.BudgetDTO;
import com.example.finance.entity.Budget;
import java.util.List;

public interface BudgetMapper {
    BudgetDTO toDTO(Budget budget);
    Budget toEntity(BudgetDTO dto);
    List<BudgetDTO> toDTOs(List<Budget> entities);
}

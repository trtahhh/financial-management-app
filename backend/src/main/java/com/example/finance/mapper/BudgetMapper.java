package com.example.finance.mapper;

import com.example.finance.dto.BudgetDTO;
import com.example.finance.entity.Budget;
import java.util.List;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BudgetMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "isDeleted", target = "isDeleted")
    BudgetDTO toDTO(Budget budget);

    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "category.id", source = "categoryId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "isDeleted", source = "isDeleted")
    Budget toEntity(BudgetDTO dto);

    List<BudgetDTO> toDTOs(List<Budget> entities);
}

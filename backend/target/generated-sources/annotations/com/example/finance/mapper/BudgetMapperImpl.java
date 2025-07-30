package com.example.finance.mapper;

import com.example.finance.dto.BudgetDTO;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-30T19:03:33+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250628-1110, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class BudgetMapperImpl implements BudgetMapper {

    @Override
    public BudgetDTO toDTO(Budget budget) {
        if ( budget == null ) {
            return null;
        }

        BudgetDTO budgetDTO = new BudgetDTO();

        budgetDTO.setUserId( budgetUserId( budget ) );
        budgetDTO.setCategoryId( budgetCategoryId( budget ) );
        budgetDTO.setIsDeleted( budget.getIsDeleted() );
        budgetDTO.setAmount( budget.getAmount() );
        budgetDTO.setCurrencyCode( budget.getCurrencyCode() );
        budgetDTO.setId( budget.getId() );
        budgetDTO.setMonth( budget.getMonth() );
        budgetDTO.setYear( budget.getYear() );

        return budgetDTO;
    }

    @Override
    public Budget toEntity(BudgetDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Budget budget = new Budget();

        budget.setUser( budgetDTOToUser( dto ) );
        budget.setCategory( budgetDTOToCategory( dto ) );
        budget.setIsDeleted( dto.getIsDeleted() );
        budget.setAmount( dto.getAmount() );
        budget.setCurrencyCode( dto.getCurrencyCode() );
        budget.setId( dto.getId() );
        budget.setMonth( dto.getMonth() );
        budget.setYear( dto.getYear() );

        return budget;
    }

    @Override
    public List<BudgetDTO> toDTOs(List<Budget> entities) {
        if ( entities == null ) {
            return null;
        }

        List<BudgetDTO> list = new ArrayList<BudgetDTO>( entities.size() );
        for ( Budget budget : entities ) {
            list.add( toDTO( budget ) );
        }

        return list;
    }

    private Long budgetUserId(Budget budget) {
        if ( budget == null ) {
            return null;
        }
        User user = budget.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long budgetCategoryId(Budget budget) {
        if ( budget == null ) {
            return null;
        }
        Category category = budget.getCategory();
        if ( category == null ) {
            return null;
        }
        Long id = category.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected User budgetDTOToUser(BudgetDTO budgetDTO) {
        if ( budgetDTO == null ) {
            return null;
        }

        User user = new User();

        user.setId( budgetDTO.getUserId() );

        return user;
    }

    protected Category budgetDTOToCategory(BudgetDTO budgetDTO) {
        if ( budgetDTO == null ) {
            return null;
        }

        Category category = new Category();

        category.setId( budgetDTO.getCategoryId() );

        return category;
    }
}

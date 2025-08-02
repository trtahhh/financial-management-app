package com.example.finance.mapper;

import com.example.finance.dto.CategoryDTO;
import com.example.finance.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDTO toDto(Category e);
    
    @Mapping(target = "goals", ignore = true)
    Category toEntity(CategoryDTO d);
}

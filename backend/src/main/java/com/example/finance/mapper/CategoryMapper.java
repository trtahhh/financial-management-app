package com.example.finance.mapper;

import com.example.finance.dto.CategoryDTO;
import com.example.finance.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
 
 CategoryDTO toDto(Category entity);
 
 Category toEntity(CategoryDTO dto);
}

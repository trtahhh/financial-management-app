package com.example.finance.mapper;

import com.example.finance.dto.CategoryDTO;
import com.example.finance.entity.Category;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-01T02:48:51+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.15 (Eclipse Adoptium)"
)
@Component
public class CategoryMapperImpl implements CategoryMapper {

    @Override
    public CategoryDTO toDto(Category e) {
        if ( e == null ) {
            return null;
        }

        CategoryDTO categoryDTO = new CategoryDTO();

        categoryDTO.setId( e.getId() );
        categoryDTO.setName( e.getName() );
        categoryDTO.setType( e.getType() );

        return categoryDTO;
    }

    @Override
    public Category toEntity(CategoryDTO d) {
        if ( d == null ) {
            return null;
        }

        Category category = new Category();

        category.setId( d.getId() );
        category.setName( d.getName() );
        category.setType( d.getType() );

        return category;
    }
}

package com.example.finance.mapper;

import com.example.finance.dto.CategoryDTO;
import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    
    @Mapping(target = "userId", source = "user.id")
    CategoryDTO toDto(Category entity);
    
    @Mapping(target = "user", source = "userId", qualifiedByName = "userFromId")
    Category toEntity(CategoryDTO dto);
    
    @Named("userFromId")
    default User userFromId(Long userId) {
        if (userId == null) return null;
        User user = new User();
        user.setId(userId);
        return user;
    }
}
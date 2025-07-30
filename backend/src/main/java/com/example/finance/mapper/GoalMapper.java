package com.example.finance.mapper;

import com.example.finance.dto.GoalDTO;
import com.example.finance.entity.Goal;
import com.example.finance.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GoalMapper {
    @Mapping(target = "userId", source = "user.id")
    GoalDTO toDto(Goal e);

    @Mapping(target = "user", source = "userId", qualifiedByName = "userFromId")
    Goal toEntity(GoalDTO d);

    @Named("userFromId")
    default User userFromId(Long id) {
        if (id == null) return null;
        User u = new User();
        u.setId(id);
        return u;
    }
}



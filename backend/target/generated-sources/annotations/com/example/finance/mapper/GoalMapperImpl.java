package com.example.finance.mapper;

import com.example.finance.dto.GoalDTO;
import com.example.finance.entity.Goal;
import com.example.finance.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-30T19:03:33+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250628-1110, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class GoalMapperImpl implements GoalMapper {

    @Override
    public GoalDTO toDto(Goal e) {
        if ( e == null ) {
            return null;
        }

        GoalDTO goalDTO = new GoalDTO();

        goalDTO.setUserId( eUserId( e ) );
        goalDTO.setCompletedAt( e.getCompletedAt() );
        goalDTO.setCurrentAmount( e.getCurrentAmount() );
        goalDTO.setDueDate( e.getDueDate() );
        goalDTO.setId( e.getId() );
        goalDTO.setName( e.getName() );
        goalDTO.setStatus( e.getStatus() );
        goalDTO.setTargetAmount( e.getTargetAmount() );

        return goalDTO;
    }

    @Override
    public Goal toEntity(GoalDTO d) {
        if ( d == null ) {
            return null;
        }

        Goal goal = new Goal();

        goal.setUser( userFromId( d.getUserId() ) );
        goal.setCompletedAt( d.getCompletedAt() );
        goal.setCurrentAmount( d.getCurrentAmount() );
        goal.setDueDate( d.getDueDate() );
        goal.setId( d.getId() );
        goal.setName( d.getName() );
        goal.setStatus( d.getStatus() );
        goal.setTargetAmount( d.getTargetAmount() );

        return goal;
    }

    private Long eUserId(Goal goal) {
        if ( goal == null ) {
            return null;
        }
        User user = goal.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}

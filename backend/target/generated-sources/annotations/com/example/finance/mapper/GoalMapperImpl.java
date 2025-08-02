package com.example.finance.mapper;

import com.example.finance.dto.GoalDTO;
import com.example.finance.entity.Goal;
import com.example.finance.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-01T02:48:50+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.15 (Eclipse Adoptium)"
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
        goalDTO.setId( e.getId() );
        goalDTO.setName( e.getName() );
        goalDTO.setTargetAmount( e.getTargetAmount() );
        goalDTO.setCurrentAmount( e.getCurrentAmount() );
        goalDTO.setDueDate( e.getDueDate() );
        goalDTO.setStatus( e.getStatus() );
        goalDTO.setCompletedAt( e.getCompletedAt() );

        return goalDTO;
    }

    @Override
    public Goal toEntity(GoalDTO d) {
        if ( d == null ) {
            return null;
        }

        Goal goal = new Goal();

        goal.setUser( userFromId( d.getUserId() ) );
        goal.setId( d.getId() );
        goal.setName( d.getName() );
        goal.setTargetAmount( d.getTargetAmount() );
        goal.setCurrentAmount( d.getCurrentAmount() );
        goal.setDueDate( d.getDueDate() );
        goal.setStatus( d.getStatus() );
        goal.setCompletedAt( d.getCompletedAt() );

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

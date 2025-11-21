package com.example.finance.repository;

import com.example.finance.entity.UserCategorizationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserCategorizationPreferenceRepository extends JpaRepository<UserCategorizationPreference, Long> {
    
    Optional<UserCategorizationPreference> findByUserIdAndDescriptionPattern(Long userId, String descriptionPattern);
    
}

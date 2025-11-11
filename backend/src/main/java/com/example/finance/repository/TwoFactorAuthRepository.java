package com.example.finance.repository;

import com.example.finance.entity.TwoFactorAuth;
import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, Long> {
    
    Optional<TwoFactorAuth> findByUser(User user);
    
    Optional<TwoFactorAuth> findByUserId(Long userId);
    
    Optional<TwoFactorAuth> findByUserIdAndEnabledTrue(Long userId);
}

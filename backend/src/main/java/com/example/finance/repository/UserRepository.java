package com.example.finance.repository;

import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
 Optional<User> findByUsername(String username);
 Optional<User> findByEmail(String email);
 Optional<User> findByEmailVerificationToken(String token);
 Optional<User> findByPasswordResetToken(String token);
 boolean existsByUsername(String username);
 boolean existsByEmail(String email);
 
 /**
 * Tìm user có mục tiêu đang hoạt động
 */
 @Query("SELECT DISTINCT u FROM User u JOIN u.goals g WHERE g.isDeleted = false AND g.status != 'COMPLETED'")
 List<User> findUsersWithActiveGoals();
}

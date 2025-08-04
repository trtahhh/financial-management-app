package com.example.finance.repository;

import com.example.finance.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserIdAndType(Long userId, String type);
    Optional<Wallet> findFirstByUserId(Long userId);
    
    // Thêm query method để tính tổng balance theo user
    @Query("SELECT SUM(w.balance) FROM Wallet w WHERE w.user.id = :userId")
    BigDecimal sumBalanceByUserId(@Param("userId") Long userId);
    
    // Lấy danh sách wallet theo user
    List<Wallet> findByUserId(Long userId);
}

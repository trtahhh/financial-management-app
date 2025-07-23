package com.example.finance.repository;

import com.example.finance.entity.Wallet;
import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    // Find all wallets for a user (not deleted)
    List<Wallet> findByUserAndIsDeletedFalseOrderByCreatedAtDesc(User user);
    
    // Find wallet by user and name
    Optional<Wallet> findByUserAndNameAndIsDeletedFalse(User user, String name);
    
    // Find wallets by type
    List<Wallet> findByUserAndTypeAndIsDeletedFalse(User user, String type);
    
    // Find active wallets for a user
    @Query("SELECT w FROM Wallet w WHERE w.user = :user AND w.isDeleted = false ORDER BY w.balance DESC")
    List<Wallet> findActiveWalletsByUser(@Param("user") User user);
    
    // Get total balance for a user
    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w WHERE w.user = :user AND w.isDeleted = false")
    Double getTotalBalanceByUser(@Param("user") User user);
    
    // Count wallets for a user
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.user = :user AND w.isDeleted = false")
    Long countWalletsByUser(@Param("user") User user);
    
    // Get wallet statistics by type
    @Query("SELECT w.type, COUNT(w), COALESCE(SUM(w.balance), 0) FROM Wallet w WHERE w.user = :user AND w.isDeleted = false GROUP BY w.type")
    List<Object[]> getWalletStatisticsByType(@Param("user") User user);
    
    // Delete all wallets for a user
    void deleteByUser(User user);
} 
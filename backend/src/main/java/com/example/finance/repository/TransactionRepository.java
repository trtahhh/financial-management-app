package com.example.finance.repository;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.entity.Category;
import com.example.finance.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Find all transactions for a user (not deleted)
    Page<Transaction> findByUserAndIsDeletedFalseOrderByTransactionDateDesc(User user, Pageable pageable);
    
    // Find all transactions for a user (not deleted) without pagination
    List<Transaction> findByUserAndIsDeletedFalseOrderByTransactionDateDesc(User user);
    
    // Find transactions by type
    List<Transaction> findByUserAndTransTypeAndIsDeletedFalseOrderByTransactionDateDesc(User user, String transType);
    
    // Find transactions by category
    List<Transaction> findByUserAndCategoryAndIsDeletedFalseOrderByTransactionDateDesc(User user, Category category);
    
    // Find transactions by wallet
    List<Transaction> findByUserAndWalletAndIsDeletedFalseOrderByTransactionDateDesc(User user, Wallet wallet);
    
    // Find transactions by date range
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isDeleted = false ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserAndDateRange(@Param("user") User user, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
    
    // Find transactions by type and date range
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.transType = :transType AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isDeleted = false ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserAndTransTypeAndDateRange(@Param("user") User user, 
                                                        @Param("transType") String transType,
                                                        @Param("startDate") LocalDate startDate, 
                                                        @Param("endDate") LocalDate endDate);
    
    // Search transactions by description
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND t.isDeleted = false ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserAndDescriptionContainingIgnoreCase(@Param("user") User user, 
                                                                 @Param("searchTerm") String searchTerm);
    
    // Get total income for a user in date range
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.transType = 'income' AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isDeleted = false")
    BigDecimal getTotalIncomeByUserAndDateRange(@Param("user") User user, 
                                               @Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);
    
    // Get total expense for a user in date range
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.transType = 'expense' AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isDeleted = false")
    BigDecimal getTotalExpenseByUserAndDateRange(@Param("user") User user, 
                                                @Param("startDate") LocalDate startDate, 
                                                @Param("endDate") LocalDate endDate);
    
    // Get total amount by category for a user in date range
    @Query("SELECT t.category.name, COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.transType = 'expense' AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isDeleted = false GROUP BY t.category.name")
    List<Object[]> getTotalAmountByCategoryForUserAndDateRange(@Param("user") User user, 
                                                              @Param("startDate") LocalDate startDate, 
                                                              @Param("endDate") LocalDate endDate);
    
    // Get recent transactions (last 10)
    List<Transaction> findTop10ByUserAndIsDeletedFalseOrderByTransactionDateDesc(User user);
    
    // Get transactions for current month
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND YEAR(t.transactionDate) = YEAR(:currentDate) AND MONTH(t.transactionDate) = MONTH(:currentDate) AND t.isDeleted = false ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserAndCurrentMonth(@Param("user") User user, 
                                               @Param("currentDate") LocalDate currentDate);
    
    // Get transactions for current year
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND YEAR(t.transactionDate) = YEAR(:currentDate) AND t.isDeleted = false ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserAndCurrentYear(@Param("user") User user, 
                                              @Param("currentDate") LocalDate currentDate);
    
    // Count transactions by type for a user
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user = :user AND t.transType = :transType AND t.isDeleted = false")
    Long countByUserAndTransType(@Param("user") User user, @Param("transType") String transType);
    
    // Get total amount by type for a user in date range
    @Query("SELECT t.transType, COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isDeleted = false GROUP BY t.transType")
    List<Object[]> getTotalAmountByTypeForUserAndDateRange(@Param("user") User user, 
                                                           @Param("startDate") LocalDate startDate, 
                                                           @Param("endDate") LocalDate endDate);
    
    // Delete all transactions for a user
    void deleteByUser(User user);
} 
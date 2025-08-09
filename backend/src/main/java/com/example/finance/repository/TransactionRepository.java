package com.example.finance.repository;

import com.example.finance.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import com.example.finance.dto.CategoryStatisticDTO;
import com.example.finance.dto.WalletStatDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByDateBetween(LocalDate from, LocalDate to);
    
    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.category " +
           "LEFT JOIN FETCH t.wallet " +
           "LEFT JOIN FETCH t.user " +
           "WHERE t.isDeleted = false")
    List<Transaction> findAllWithDetails();
    
    // For dashboard queries
    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.category " +
           "LEFT JOIN FETCH t.wallet " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :startDate AND :endDate AND t.isDeleted = false")
    List<Transaction> findByUserIdAndTypeAndDateBetween(
        @Param("userId") Long userId, 
        @Param("type") String type, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type AND t.isDeleted = false"
            + " AND (:month IS NULL OR FUNCTION('MONTH', t.date) = :month)"
            + " AND (:year IS NULL OR FUNCTION('YEAR', t.date) = :year)")
    BigDecimal sumAmountByUserAndType(
        @Param("userId") Long userId,
        @Param("type") String type,
        @Param("month") Integer month,
        @Param("year") Integer year
    );

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId "
        + "AND (:month IS NULL OR FUNCTION('MONTH', t.date) = :month) "
        + "AND (:year IS NULL OR FUNCTION('YEAR', t.date) = :year) "
        + "AND t.isDeleted = false")
    List<Transaction> findByUserIdAndMonthAndYear(
        @Param("userId") Long userId,
        @Param("month") Integer month,
        @Param("year") Integer year
    );
    
    @Query("SELECT t.category.id, SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId " +
           "AND (:month IS NULL OR FUNCTION('MONTH', t.date) = :month) " +
           "AND (:year IS NULL OR FUNCTION('YEAR', t.date) = :year) " +
           "AND t.isDeleted = false GROUP BY t.category.id")
    List<Object[]> sumAmountByCategory(
        @Param("userId") Long userId,
        @Param("month") Integer month,
        @Param("year") Integer year
    );

    @Query("SELECT t.wallet.id, SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId "
        + "AND (:walletId IS NULL OR t.wallet.id = :walletId) "
        + "AND (:type IS NULL OR t.type = :type) "
        + "AND (:month IS NULL OR FUNCTION('MONTH', t.date) = :month) "
        + "AND (:year IS NULL OR FUNCTION('YEAR', t.date) = :year) "
        + "AND t.isDeleted = false GROUP BY t.wallet.id")
    List<Object[]> sumAmountByWallet(
        @Param("userId") Long userId,
        @Param("walletId") Long walletId,
        @Param("type") String type,
        @Param("month") Integer month,
        @Param("year") Integer year
    );

    @Query("SELECT t.wallet.id, COUNT(t.id) FROM Transaction t WHERE t.user.id = :userId "
        + "AND (:walletId IS NULL OR t.wallet.id = :walletId) "
        + "AND (:type IS NULL OR t.type = :type) "
        + "AND (:month IS NULL OR FUNCTION('MONTH', t.date) = :month) "
        + "AND (:year IS NULL OR FUNCTION('YEAR', t.date) = :year) "
        + "AND t.isDeleted = false GROUP BY t.wallet.id")
    List<Object[]> countTransactionsByWallet(
        @Param("userId") Long userId,
        @Param("walletId") Long walletId,
        @Param("type") String type,
        @Param("month") Integer month,
        @Param("year") Integer year
    );

    // Check if wallet has any transactions
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.wallet.id = :walletId AND t.isDeleted = false")
    boolean existsByWalletId(@Param("walletId") Long walletId);

    // Sum transactions by wallet and type for balance calculation
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.wallet.id = :walletId AND t.type = :type AND t.isDeleted = false")
    BigDecimal sumByWalletIdAndType(@Param("walletId") Long walletId, @Param("type") String type);

    // Sum transactions by user and type for total balance calculation  
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type AND t.isDeleted = false")
    BigDecimal sumByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId AND MONTH(t.date) = :month AND YEAR(t.date) = :year AND t.type = 'expense' AND t.isDeleted = false")
    BigDecimal sumByUserCategoryMonth(@Param("userId") Long userId, @Param("categoryId") Long categoryId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT c.name, c.color, SUM(t.amount), COUNT(t) FROM Transaction t JOIN t.category c WHERE t.user.id = :userId AND t.type = 'expense' AND MONTH(t.date) = :month AND YEAR(t.date) = :year AND t.isDeleted = false GROUP BY c.id, c.name, c.color ORDER BY SUM(t.amount) DESC")
    List<Object[]> findExpensesByCategory(@Param("userId") Long userId, @Param("month") Integer month, @Param("year") Integer year);

    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.category " +
           "LEFT JOIN FETCH t.wallet " +
           "WHERE t.user.id = :userId AND t.isDeleted = false " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :startDate AND :endDate AND t.isDeleted = false")
    BigDecimal sumByUserTypeAndDateRange(@Param("userId") Long userId, @Param("type") String type, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    Long countByUserIdAndIsDeletedFalse(Long userId);

    @Query("SELECT t.category.name, t.category.color, SUM(t.amount), COUNT(t) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'expense' AND t.date BETWEEN :startDate AND :endDate GROUP BY t.category.name, t.category.color")
    List<Object[]> findExpensesByCategoryByDate(@Param("userId") Long userId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.date BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdAndDateBetweenOrderByCreatedAtDesc(@Param("userId") Long userId,
                                                                    @Param("startDate") LocalDate startDate,
                                                                    @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId AND t.type = 'expense' AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumByUserCategoryAndDateRange(@Param("userId") Long userId,
                                            @Param("categoryId") Long categoryId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    // Optimized query for recent transactions with pagination
    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.category " +
           "LEFT JOIN FETCH t.wallet " +
           "WHERE t.user.id = :userId AND t.isDeleted = false " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactionsByUserId(@Param("userId") Long userId, Pageable pageable);

    // Optimized query for statistics with better performance
    @Query("SELECT NEW com.example.finance.dto.CategoryStatisticDTO(" +
           "c.name, c.color, SUM(t.amount), COUNT(t)) " +
           "FROM Transaction t JOIN t.category c " +
           "WHERE t.user.id = :userId AND t.type = 'expense' " +
           "AND (:month IS NULL OR FUNCTION('MONTH', t.date) = :month) " +
           "AND (:year IS NULL OR FUNCTION('YEAR', t.date) = :year) " +
           "AND t.isDeleted = false " +
           "GROUP BY c.id, c.name, c.color " +
           "ORDER BY SUM(t.amount) DESC")
    List<CategoryStatisticDTO> findExpenseStatisticsByCategory(
        @Param("userId") Long userId, 
        @Param("month") Integer month, 
        @Param("year") Integer year
    );

    // Optimized query for wallet statistics
    @Query("SELECT NEW com.example.finance.dto.WalletStatDTO(" +
           "w.id, w.name, SUM(t.amount), COUNT(t)) " +
           "FROM Transaction t JOIN t.wallet w " +
           "WHERE t.user.id = :userId " +
           "AND (:walletId IS NULL OR w.id = :walletId) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:month IS NULL OR FUNCTION('MONTH', t.date) = :month) " +
           "AND (:year IS NULL OR FUNCTION('YEAR', t.date) = :year) " +
           "AND t.isDeleted = false " +
           "GROUP BY w.id, w.name " +
           "ORDER BY SUM(t.amount) DESC")
    List<WalletStatDTO> findWalletStatistics(
        @Param("userId") Long userId,
        @Param("walletId") Long walletId,
        @Param("type") String type,
        @Param("month") Integer month,
        @Param("year") Integer year
    );

    // Find transaction by ID with eager loading
    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.category " +
           "LEFT JOIN FETCH t.wallet " +
           "LEFT JOIN FETCH t.user " +
           "WHERE t.id = :id")
    Optional<Transaction> findByIdWithDetails(@Param("id") Long id);
}

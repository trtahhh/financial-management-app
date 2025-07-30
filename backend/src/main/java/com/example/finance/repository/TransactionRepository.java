package com.example.finance.repository;

import com.example.finance.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByDateBetween(LocalDate from, LocalDate to);
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type AND t.isDeleted = false"
            + " AND (:month IS NULL OR FUNCTION('MONTH', t.date) = :month)"
            + " AND (:year IS NULL OR FUNCTION('YEAR', t.date) = :year)")
    Double sumAmountByUserAndType(
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

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId AND MONTH(t.date) = :month AND YEAR(t.date) = :year AND t.isDeleted = false")
    BigDecimal sumByUserCategoryMonth(Long userId, Long categoryId, int month, int year);

}

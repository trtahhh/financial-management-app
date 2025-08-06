package com.example.finance.repository;

import com.example.finance.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    
    List<RecurringTransaction> findByUserIdAndIsActiveTrue(Long userId);
    
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.nextExecution <= :date AND rt.isActive = true")
    List<RecurringTransaction> findDueTransactions(@Param("date") LocalDate date);
    
    @Query("SELECT rt FROM RecurringTransaction rt WHERE rt.user.id = :userId AND rt.isActive = true")
    List<RecurringTransaction> findActiveByUserId(@Param("userId") Long userId);
}
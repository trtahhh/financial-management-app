package com.example.finance.repository;

import com.example.finance.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
 
 @Query("SELECT rt FROM RecurringTransaction rt LEFT JOIN FETCH rt.user LEFT JOIN FETCH rt.wallet LEFT JOIN FETCH rt.category WHERE rt.id = :id")
 @NonNull
 Optional<RecurringTransaction> findById(@NonNull @Param("id") Long id);
 
 @Query("SELECT rt FROM RecurringTransaction rt LEFT JOIN FETCH rt.user LEFT JOIN FETCH rt.wallet LEFT JOIN FETCH rt.category WHERE rt.user.id = :userId AND rt.isActive = true")
 List<RecurringTransaction> findByUser_IdAndIsActiveTrue(@Param("userId") Long userId);
 
 @Query("SELECT rt FROM RecurringTransaction rt LEFT JOIN FETCH rt.user LEFT JOIN FETCH rt.wallet LEFT JOIN FETCH rt.category WHERE rt.nextExecution <= :date AND rt.isActive = true")
 List<RecurringTransaction> findDueTransactions(@Param("date") LocalDate date);
 
 @Query("SELECT rt FROM RecurringTransaction rt LEFT JOIN FETCH rt.user LEFT JOIN FETCH rt.wallet LEFT JOIN FETCH rt.category WHERE rt.user.id = :userId AND rt.isActive = true")
 List<RecurringTransaction> findActiveByUserId(@Param("userId") Long userId);
 
 @Query("SELECT rt FROM RecurringTransaction rt LEFT JOIN FETCH rt.user LEFT JOIN FETCH rt.wallet LEFT JOIN FETCH rt.category WHERE rt.isActive = true")
 List<RecurringTransaction> findByIsActiveTrue();
}

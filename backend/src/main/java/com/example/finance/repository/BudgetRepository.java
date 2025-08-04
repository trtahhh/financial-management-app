package com.example.finance.repository;

import com.example.finance.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findAllByIsDeletedFalse();

    Optional<Budget> findByIdAndIsDeletedFalse(Long id); 

    List<Budget> findByUser_IdAndCategory_IdAndMonthAndYear(Long userId, Long categoryId, int month, int year);

    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.category WHERE b.user.id = :userId AND b.month = :month AND b.year = :year AND b.isDeleted = false")
    List<Budget> findByUserIdAndMonthAndYear(@Param("userId") Long userId, @Param("month") Integer month, @Param("year") Integer year);
}
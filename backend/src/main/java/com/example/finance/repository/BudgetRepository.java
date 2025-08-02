package com.example.finance.repository;

import com.example.finance.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findAllByIsDeletedFalse();

    Optional<Budget> findByIdAndIsDeletedFalse(Long id); 

    List<Budget> findByUserIdAndCategoryIdAndMonthAndYear(Long userId, Long categoryId, int month, int year);

}
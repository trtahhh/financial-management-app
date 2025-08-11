package com.example.finance.repository;

import com.example.finance.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    List<Budget> findAllByIsDeletedFalse();

    List<Budget> findByUserIdAndIsDeletedFalse(Long userId);
    
    @Query("SELECT b FROM Budget b JOIN FETCH b.category WHERE b.user.id = :userId AND b.month = :month AND b.year = :year AND b.isDeleted = false")
    List<Budget> findByUserIdAndMonthAndYearAndIsDeletedFalse(@Param("userId") Long userId, @Param("month") Integer month, @Param("year") Integer year);
    
    Optional<Budget> findByIdAndIsDeletedFalse(Long id);
    
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.month = :month AND b.year = :year AND b.category.id = :categoryId AND b.isDeleted = false")
    Optional<Budget> findByUserAndMonthAndCategoryAndIsDeletedFalse(
            @Param("userId") Long userId, 
            @Param("month") Integer month, 
            @Param("year") Integer year,
            @Param("categoryId") Long categoryId);
    
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.month = :month AND b.year = :year")
    List<Budget> findByUserIdAndMonthAndYear(@Param("userId") Long userId, @Param("month") Integer month, @Param("year") Integer year);

    @Query("SELECT b FROM Budget b JOIN FETCH b.category WHERE b.user.id = :userId AND b.isDeleted = false AND " +
        "(b.year > :startYear OR (b.year = :startYear AND b.month >= :startMonth)) AND " +
        "(b.year < :endYear OR (b.year = :endYear AND b.month <= :endMonth))")
        List<Budget> findByUserIdAndMonthYearRangeAndIsDeletedFalse(@Param("userId") Long userId,
                                                                @Param("startMonth") int startMonth,
                                                                @Param("startYear") int startYear,
                                                                @Param("endMonth") int endMonth,
                                                                @Param("endYear") int endYear);
}
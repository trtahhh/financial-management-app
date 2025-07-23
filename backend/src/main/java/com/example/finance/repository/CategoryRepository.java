package com.example.finance.repository;

import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // Find all categories for a user (including default ones)
    @Query("SELECT c FROM Category c WHERE (c.user = :user OR c.user IS NULL) AND c.isDeleted = false ORDER BY c.isDefault DESC, c.name ASC")
    List<Category> findByUserOrDefault(@Param("user") User user);
    
    // Find categories by type
    @Query("SELECT c FROM Category c WHERE (c.user = :user OR c.user IS NULL) AND c.type = :type AND c.isDeleted = false ORDER BY c.isDefault DESC, c.name ASC")
    List<Category> findByUserAndType(@Param("user") User user, @Param("type") String type);
    
    // Find default categories
    List<Category> findByUserIsNullAndIsDefaultTrueAndIsDeletedFalse();
    
    // Find custom categories for a user
    List<Category> findByUserAndIsDeletedFalseOrderByNameAsc(User user);
    
    // Find category by name and type
    @Query("SELECT c FROM Category c WHERE (c.user = :user OR c.user IS NULL) AND c.name = :name AND c.type = :type AND c.isDeleted = false")
    Optional<Category> findByNameAndType(@Param("user") User user, @Param("name") String name, @Param("type") String type);
    
    // Find categories by parent
    List<Category> findByParentAndIsDeletedFalseOrderByNameAsc(Category parent);
    
    // Count categories by type for a user
    @Query("SELECT COUNT(c) FROM Category c WHERE (c.user = :user OR c.user IS NULL) AND c.type = :type AND c.isDeleted = false")
    Long countByUserAndType(@Param("user") User user, @Param("type") String type);
    
    // Get category statistics
    @Query("SELECT c.type, COUNT(c) FROM Category c WHERE (c.user = :user OR c.user IS NULL) AND c.isDeleted = false GROUP BY c.type")
    List<Object[]> getCategoryStatistics(@Param("user") User user);
    
    // Find income categories
    @Query("SELECT c FROM Category c WHERE (c.user = :user OR c.user IS NULL) AND c.type = 'income' AND c.isDeleted = false ORDER BY c.isDefault DESC, c.name ASC")
    List<Category> findIncomeCategories(@Param("user") User user);
    
    // Find expense categories
    @Query("SELECT c FROM Category c WHERE (c.user = :user OR c.user IS NULL) AND c.type = 'expense' AND c.isDeleted = false ORDER BY c.isDefault DESC, c.name ASC")
    List<Category> findExpenseCategories(@Param("user") User user);
    
    // Delete all categories for a user
    void deleteByUser(User user);
} 
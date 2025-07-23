package com.example.finance.service;

import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Create new category
    public Category createCategory(Category category, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        category.setUser(user);
        category.setCreatedAt(java.time.LocalDateTime.now());
        
        return categoryRepository.save(category);
    }
    
    // Get category by ID
    public Category getCategoryById(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        if (category.getUser() != null && !category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        return category;
    }
    
    // Get all categories for user (including defaults)
    public List<Category> getUserCategories(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return categoryRepository.findByUserOrDefault(user);
    }
    
    // Get categories by type
    public List<Category> getCategoriesByType(String username, String type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return categoryRepository.findByUserAndType(user, type);
    }
    
    // Get income categories
    public List<Category> getIncomeCategories(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return categoryRepository.findIncomeCategories(user);
    }
    
    // Get expense categories
    public List<Category> getExpenseCategories(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return categoryRepository.findExpenseCategories(user);
    }
    
    // Get default categories
    public List<Category> getDefaultCategories() {
        return categoryRepository.findByUserIsNullAndIsDefaultTrueAndIsDeletedFalse();
    }
    
    // Get custom categories for user
    public List<Category> getCustomCategories(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return categoryRepository.findByUserAndIsDeletedFalseOrderByNameAsc(user);
    }
    
    // Update category
    public Category updateCategory(Long id, Category categoryDetails, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        if (category.getUser() != null && !category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        category.setName(categoryDetails.getName());
        category.setType(categoryDetails.getType());
        category.setIcon(categoryDetails.getIcon());
        category.setParent(categoryDetails.getParent());
        
        return categoryRepository.save(category);
    }
    
    // Delete category (soft delete)
    public void deleteCategory(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        if (category.getUser() != null && !category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        category.setIsDeleted(true);
        categoryRepository.save(category);
    }
    
    // Get category summary
    public Map<String, Object> getCategorySummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Object[]> categoryStatistics = categoryRepository.getCategoryStatistics(user);
        
        Map<String, Object> summary = Map.of(
            "categoryStatistics", categoryStatistics
        );
        
        return summary;
    }
} 
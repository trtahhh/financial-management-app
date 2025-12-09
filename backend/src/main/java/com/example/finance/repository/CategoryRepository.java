package com.example.finance.repository;

import com.example.finance.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // Categories are global (shared), not user-specific
    List<Category> findAllByIsActiveTrue();
    
    List<Category> findByType(String type);
    
    List<Category> findByTypeAndIsActiveTrue(String type);
}
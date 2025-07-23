package com.example.finance.controller;

import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    @Autowired 
    CategoryRepository categoryRepo;
    
    @Autowired
    UserRepository userRepo;

    // Lấy danh sách danh mục
    @GetMapping
    public ResponseEntity<?> getCategories() {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<Category> categories = categoryRepo.findByUserOrDefault(user);
        return ResponseEntity.ok(categories);
    }

    // Lấy danh mục theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategory(@PathVariable Long id) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Category category = categoryRepo.findById(id).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu (chỉ cho custom categories)
        if (category.getUser() != null && !category.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        return ResponseEntity.ok(category);
    }

    // Tạo danh mục mới
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Map<String, Object> request) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Category category = new Category();
            category.setUser(user);
            category.setName((String) request.get("name"));
            category.setType((String) request.get("type"));
            category.setIcon((String) request.get("icon"));
            category.setIsDefault(false); // Custom categories are not default
            
            Category savedCategory = categoryRepo.save(category);
            return ResponseEntity.ok(savedCategory);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tạo danh mục: " + e.getMessage()));
        }
    }

    // Cập nhật danh mục
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Category category = categoryRepo.findById(id).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu (chỉ cho custom categories)
        if (category.getUser() == null || !category.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        try {
            if (request.get("name") != null) {
                category.setName((String) request.get("name"));
            }
            if (request.get("type") != null) {
                category.setType((String) request.get("type"));
            }
            if (request.get("icon") != null) {
                category.setIcon((String) request.get("icon"));
            }
            
            Category updatedCategory = categoryRepo.save(category);
            return ResponseEntity.ok(updatedCategory);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi cập nhật danh mục: " + e.getMessage()));
        }
    }

    // Xóa danh mục
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Category category = categoryRepo.findById(id).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu (chỉ cho custom categories)
        if (category.getUser() == null || !category.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        categoryRepo.delete(category);
        return ResponseEntity.ok(Map.of("message", "Đã xóa danh mục thành công"));
    }

    private String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
} 
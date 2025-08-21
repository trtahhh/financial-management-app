package com.example.finance.controller;

import com.example.finance.dto.CategoryDTO;
import com.example.finance.service.CategoryService;
import com.example.finance.service.CategoryColorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService service;
    private final CategoryColorService categoryColorService;

    @GetMapping
    public ResponseEntity<?> list() {
        try {
            List<CategoryDTO> categories = service.findAll();
            log.info("Retrieved {} categories", categories.size());
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error getting categories", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi lấy danh sách danh mục: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CategoryDTO dto) {
        try {
            log.info("Creating category with data: {}", dto);
            CategoryDTO result = service.save(dto);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tạo danh mục thành công",
                "data", result
            ));
        } catch (Exception e) {
            log.error("Error creating category", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi tạo danh mục: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id) {
        try {
            CategoryDTO category = service.findById(id);
            if (category == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Không tìm thấy danh mục"));
            }
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            log.error("Error getting category by id: {}", id, e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi lấy thông tin danh mục: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody CategoryDTO dto) {
        try {
            dto.setId(id);
            CategoryDTO result = service.update(dto);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật danh mục thành công",
                "data", result
            ));
        } catch (Exception e) {
            log.error("Error updating category: {}", id, e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi cập nhật danh mục: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        try {
            service.deleteById(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xóa danh mục thành công"
            ));
        } catch (Exception e) {
            log.error("Error deleting category: {}", id, e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi xóa danh mục: " + e.getMessage()));
        }
    }

    /**
     * Test endpoint để xem màu của các category
     */
    @GetMapping("/test-colors")
    public ResponseEntity<Map<String, Object>> testCategoryColors() {
        try {
            Map<String, String> categoryColors = categoryColorService.getAllCategoryColors();
            
            // Test một số category cụ thể
            Map<String, String> testColors = new HashMap<>();
            String[] testCategories = {
                "Ăn uống", "Giao thông", "Giải trí", "Sức khỏe", "Giáo dục",
                "Mua sắm", "Tiện ích", "Du lịch", "Thể thao", "Lương",
                "Thu nhập khác", "Đầu tư", "Kinh doanh", "Khác"
            };
            
            for (String category : testCategories) {
                testColors.put(category, categoryColorService.getCategoryColor(category));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("allCategoryColors", categoryColors);
            response.put("testCategoryColors", testColors);
            response.put("message", "Màu category đã được tạo thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing category colors: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Lỗi test màu category: " + e.getMessage()));
        }
    }
}

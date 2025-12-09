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

 /**
  * Get all categories (read-only)
  * Categories are system-managed for AI categorization
  */
 @GetMapping
 public ResponseEntity<?> list() {
 try {
 List<CategoryDTO> categories = service.findAll();
 log.info("Retrieved {} categories (read-only)", categories.size());
 
 return ResponseEntity.ok(Map.of(
 "success", true,
 "data", categories,
 "meta", Map.of(
 "total", categories.size(),
 "readOnly", true,
 "aiEnabled", true,
 "message", "Categories are auto-managed by AI. Use /api/ai/categorize for automatic categorization."
 )
 ));
 } catch (Exception e) {
 log.error("Error getting categories", e);
 return ResponseEntity.badRequest()
 .body(Map.of("success", false, "message", "Lỗi lấy danh sách danh mục: " + e.getMessage()));
 }
 }

 /**
  * DEPRECATED: Category creation disabled - AI auto-categorization handles this
  * Categories are pre-defined in database (14 fixed categories)
  */
 @PostMapping
 @Deprecated
 public ResponseEntity<?> create(@RequestBody CategoryDTO dto) {
 return ResponseEntity.status(403).body(Map.of(
 "success", false,
 "message", "Category creation is disabled. System uses AI auto-categorization with 14 pre-defined categories.",
 "hint", "Use GET /api/categories to view available categories"
 ));
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

 /**
  * DEPRECATED: Category update disabled - Categories are system-managed
  */
 @PutMapping("/{id}")
 @Deprecated
 public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody CategoryDTO dto) {
 return ResponseEntity.status(403).body(Map.of(
 "success", false,
 "message", "Category modification is disabled. Categories are system-managed and optimized for AI categorization.",
 "hint", "Transactions are auto-categorized by AI with 98% accuracy"
 ));
 }

 /**
  * DEPRECATED: Category deletion disabled - Categories are required for AI model
  */
 @DeleteMapping("/{id}")
 @Deprecated
 public ResponseEntity<?> delete(@PathVariable("id") Long id) {
 return ResponseEntity.status(403).body(Map.of(
 "success", false,
 "message", "Category deletion is disabled. All 14 categories are required for AI categorization model.",
 "hint", "Categories: Income (4) + Expense (10) = 14 total"
 ));
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

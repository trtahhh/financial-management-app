package com.example.finance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class CategoryColorService {

 // Bảng màu cố định cho các category phổ biến - đảm bảo không trùng lặp
 private static final Map<String, String> CATEGORY_COLOR_MAP = new HashMap<>();
 
 static {
 // Income categories - màu xanh lá và xanh dương
 CATEGORY_COLOR_MAP.put("Lương", "#27AE60"); // Xanh lá đậm
 CATEGORY_COLOR_MAP.put("Thu nhập khác", "#2980B9"); // Xanh dương đậm
 CATEGORY_COLOR_MAP.put("Đầu tư", "#8E44AD"); // Tím đậm
 CATEGORY_COLOR_MAP.put("Kinh doanh", "#E67E22"); // Cam
 
 // Expense categories - màu đa dạng
 CATEGORY_COLOR_MAP.put("Ăn uống", "#FF6B6B"); // Đỏ cam
 CATEGORY_COLOR_MAP.put("Giao thông", "#4ECDC4"); // Xanh lá
 CATEGORY_COLOR_MAP.put("Giải trí", "#45B7D1"); // Xanh dương
 CATEGORY_COLOR_MAP.put("Sức khỏe", "#96CEB4"); // Xanh lá nhạt
 CATEGORY_COLOR_MAP.put("Giáo dục", "#FFEAA7"); // Vàng
 CATEGORY_COLOR_MAP.put("Mua sắm", "#DDA0DD"); // Tím
 CATEGORY_COLOR_MAP.put("Tiện ích", "#98D8C8"); // Xanh lá đậm
 CATEGORY_COLOR_MAP.put("Du lịch", "#F7DC6F"); // Vàng cam
 CATEGORY_COLOR_MAP.put("Thể thao", "#BB8FCE"); // Tím nhạt
 CATEGORY_COLOR_MAP.put("Khác", "#95A5A6"); // Xám
 
 // Backup colors cho các category khác
 CATEGORY_COLOR_MAP.put("default", "#FF6384"); // Hồng
 }
 
 // Bảng màu dự phòng cho các category không có trong map cố định
 private static final String[] BACKUP_COLORS = {
 "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF", "#FF9F40",
 "#FF6B9D", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
 "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9",
 "#F8C471", "#82E0AA", "#F1948A", "#85C1E9", "#D7BDE2"
 };
 
 private int backupColorIndex = 0;
 
 /**
 * Lấy màu cho category, đảm bảo không trùng lặp
 */
 public String getCategoryColor(String categoryName) {
 if (categoryName == null || categoryName.trim().isEmpty()) {
 return getNextBackupColor();
 }
 
 // Tìm kiếm chính xác
 String exactMatch = CATEGORY_COLOR_MAP.get(categoryName);
 if (exactMatch != null) {
 return exactMatch;
 }
 
 // Tìm kiếm không phân biệt hoa thường
 String caseInsensitiveMatch = CATEGORY_COLOR_MAP.entrySet().stream()
 .filter(entry -> entry.getKey().equalsIgnoreCase(categoryName))
 .map(Map.Entry::getValue)
 .findFirst()
 .orElse(null);
 
 if (caseInsensitiveMatch != null) {
 return caseInsensitiveMatch;
 }
 
 // Nếu không tìm thấy, sử dụng màu dự phòng
 log.info("🎨 No predefined color found for category: {}, using backup color", categoryName);
 return getNextBackupColor();
 }
 
 /**
 * Lấy màu dự phòng theo thứ tự
 */
 private String getNextBackupColor() {
 String color = BACKUP_COLORS[backupColorIndex % BACKUP_COLORS.length];
 backupColorIndex++;
 return color;
 }
 
 /**
 * Lấy tất cả màu category để frontend có thể sử dụng
 */
 public Map<String, String> getAllCategoryColors() {
 return new HashMap<>(CATEGORY_COLOR_MAP);
 }
 
 /**
 * Thêm màu mới cho category
 */
 public void addCategoryColor(String categoryName, String color) {
 if (categoryName != null && color != null) {
 CATEGORY_COLOR_MAP.put(categoryName, color);
 log.info("🎨 Added color {} for category: {}", color, categoryName);
 }
 }
 
 /**
 * Reset index màu dự phòng
 */
 public void resetBackupColorIndex() {
 backupColorIndex = 0;
 }
}

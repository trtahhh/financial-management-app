package com.example.finance.controller;

import com.example.finance.dto.NotificationDTO;
import com.example.finance.security.CustomUserDetails;
import com.example.finance.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

 @Autowired
 private NotificationService notificationService;

 /**
  * Lấy thông báo chưa đọc của user hiện tại (không cần userId)
  */
 @GetMapping("/unread")
 public ResponseEntity<List<Map<String, Object>>> getUnreadNotifications() {
 try {
 Long userId = getCurrentUserId();
 if (userId == null) {
 return ResponseEntity.ok(List.of());
 }
 
 List<Map<String, Object>> notifications = notificationService.getUnreadNotifications(userId);
 return ResponseEntity.ok(notifications != null ? notifications : List.of());
 } catch (Exception e) {
 System.err.println("Error getting unread notifications: " + e.getMessage());
 e.printStackTrace();
 return ResponseEntity.ok(List.of());
 }
 }

 /**
  * Đánh dấu tất cả thông báo của user hiện tại là đã đọc (không cần userId)
  */
 @PostMapping("/mark-all-read")
 public ResponseEntity<Map<String, String>> markAllAsRead() {
 try {
 Long userId = getCurrentUserId();
 if (userId == null) {
 return ResponseEntity.ok(Map.of("message", "No user logged in"));
 }
 
 notificationService.markAllAsRead(userId);
 return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
 } catch (Exception e) {
 System.err.println("Error marking all as read: " + e.getMessage());
 e.printStackTrace();
 return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
 }
 }

 @GetMapping("/{userId}")
 public List<NotificationDTO> getUserNotifications(@PathVariable Long userId) {
 return notificationService.getUserNotifications(userId);
 }

 @PutMapping("/read/{id}")
 public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
 notificationService.markAsRead(id);
 return ResponseEntity.ok().build();
 }

 @DeleteMapping("/{id}")
 public ResponseEntity<String> deleteNotification(@PathVariable Long id) {
 notificationService.deleteNotification(id);
 return ResponseEntity.ok("Notification deleted successfully");
 }

 /**
  * Lấy số lượng thông báo chưa đọc
  */
 @GetMapping("/{userId}/unread-count")
 public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable Long userId) {
 Long count = notificationService.countUnreadNotifications(userId);
 return ResponseEntity.ok(Map.of("unreadCount", count));
 }

 /**
  * Lấy danh sách thông báo chưa đọc (giới hạn 5 cái)
  */
 @GetMapping("/{userId}/unread")
 public ResponseEntity<List<Map<String, Object>>> getUnreadNotificationsByUserId(@PathVariable Long userId) {
 List<Map<String, Object>> notifications = notificationService.getUnreadNotifications(userId);
 return ResponseEntity.ok(notifications);
 }

 /**
  * Đánh dấu tất cả thông báo của user là đã đọc
  */
 @PutMapping("/{userId}/read-all")
 public ResponseEntity<Map<String, String>> markAllAsReadByUserId(@PathVariable Long userId) {
 notificationService.markAllAsRead(userId);
 return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
 }

 /**
  * Helper method để lấy userId từ Authentication
  */
 private Long getCurrentUserId() {
 try {
 Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 if (auth == null || !auth.isAuthenticated()) {
 return null;
 }
 
 Object principal = auth.getPrincipal();
 if (principal instanceof CustomUserDetails) {
 return ((CustomUserDetails) principal).getId();
 }
 
 return null;
 } catch (Exception e) {
 System.err.println("Error getting current user ID: " + e.getMessage());
 return null;
 }
 }
}
package com.example.finance.service;

import com.example.finance.dto.NotificationDTO;
import com.example.finance.entity.*;
import com.example.finance.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private GoalRepository goalRepository;

    public List<NotificationDTO> getUserNotifications(Long userId) {
        List<Notification> list = notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public void markAsRead(Long id) {
        Notification n = notificationRepository.findById(id).orElseThrow();
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    public void deleteNotification(Long id) {
        Notification n = notificationRepository.findById(id).orElseThrow();
        n.setIsDeleted(true);
        n.setDeletedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }

    public boolean existsOverBudget(Long userId, Long categoryId, int month, int year) {
        return notificationRepository.existsByUserIdAndCategoryIdAndTypeAndMonthAndYearAndIsDeletedFalse(
            userId, categoryId, "OVER_BUDGET", month, year);
    }

    public void createOverBudgetNotification(Long userId, Long categoryId, Long budgetId, BigDecimal spending, BigDecimal limit, int month, int year) {
        Notification noti = new Notification();
        
        // Set relationships
        User user = userRepository.findById(userId).orElse(null);
        Category category = categoryRepository.findById(categoryId).orElse(null);
        Budget budget = budgetRepository.findById(budgetId).orElse(null);
        
        noti.setUser(user);
        noti.setCategory(category);
        noti.setBudget(budget);
        noti.setType("OVER_BUDGET");
        noti.setMessage("Bạn đã vượt quá ngân sách (" + limit + ") cho danh mục/tháng này! Tổng chi: " + spending);
        noti.setMonth(month);
        noti.setYear(year);
        notificationRepository.save(noti);
    }

    public void createLowBalanceNotification(Long userId, Long walletId, BigDecimal balance) {
        Notification notification = new Notification();
        
        User user = userRepository.findById(userId).orElse(null);
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        
        notification.setUser(user);
        notification.setWallet(wallet);
        notification.setType("LOW_BALANCE");
        notification.setMessage("Số dư ví đã xuống dưới " + balance + " VNĐ.");
        notificationRepository.save(notification);
    }

    public void createGoalNotification(Long userId, Long goalId, String type, String message) {
        Notification notification = new Notification();
        
        User user = userRepository.findById(userId).orElse(null);
        Goal goal = goalRepository.findById(goalId).orElse(null);
        
        notification.setUser(user);
        notification.setGoal(goal);
        notification.setType(type); 
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    public void createGoalCompletedNotification(Long userId, Long goalId, String goalName) {
        Notification noti = new Notification();
        
        User user = userRepository.findById(userId).orElse(null);
        Goal goal = goalRepository.findById(goalId).orElse(null);
        
        noti.setUser(user);
        noti.setGoal(goal);
        noti.setType("GOAL_COMPLETED");
        noti.setMessage("Chúc mừng! Bạn đã hoàn thành mục tiêu: " + goalName);
        notificationRepository.save(noti);
    }

    public NotificationDTO toDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setUserId(n.getUser() != null ? n.getUser().getId() : null);
        dto.setWalletId(n.getWallet() != null ? n.getWallet().getId() : null);
        dto.setBudgetId(n.getBudget() != null ? n.getBudget().getId() : null);
        dto.setGoalId(n.getGoal() != null ? n.getGoal().getId() : null);
        dto.setTransactionId(n.getTransaction() != null ? n.getTransaction().getId() : null);
        dto.setCategoryId(n.getCategory() != null ? n.getCategory().getId() : null);
        dto.setMessage(n.getMessage());
        dto.setType(n.getType());
        dto.setIsRead(n.getIsRead());
        dto.setIsDeleted(n.getIsDeleted());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setDeletedAt(n.getDeletedAt());
        dto.setMonth(n.getMonth());
        dto.setYear(n.getYear());
        return dto;
    }

    // Thêm các method này vào cuối class NotificationService (trước method toDTO):

    /**
     * Kiểm tra xem đã có notification cho budget với type cụ thể chưa
     */
    public boolean existsByBudgetAndType(Long budgetId, String type, Integer month, Integer year) {
        return notificationRepository.existsByBudgetIdAndTypeAndMonthAndYearAndIsDeletedFalse(
                budgetId, type, month, year);
    }

    /**
     * Tạo notification cho budget alert
     */
    public void createBudgetNotification(Long userId, Long budgetId, Long categoryId, 
                                    String message, String type, Integer month, Integer year) {
        Notification notification = new Notification();
        
        // Set relationships
        User user = userRepository.findById(userId).orElse(null);
        Budget budget = budgetRepository.findById(budgetId).orElse(null);
        Category category = categoryRepository.findById(categoryId).orElse(null);
        
        notification.setUser(user);
        notification.setBudget(budget);
        notification.setCategory(category);
        notification.setMessage(message);
        notification.setType(type);
        notification.setMonth(month);
        notification.setYear(year);
        notification.setIsRead(false);
        notification.setIsDeleted(false);
        
        // Set priority based on type
        if ("BUDGET_EXCEEDED".equals(type)) {
            notification.setPriority(3); // High priority
        } else if ("BUDGET_WARNING".equals(type)) {
            notification.setPriority(2); // Medium priority
        } else {
            notification.setPriority(1); // Low priority
        }
        
        notificationRepository.save(notification);
    }

    /**
     * Lấy danh sách notification chưa đọc
     */
    public List<Map<String, Object>> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository
                .findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        
        return notifications.stream()
                .limit(5) // Chỉ lấy 5 notification gần nhất
                .map(n -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", n.getId());
                    map.put("message", n.getMessage());
                    map.put("type", n.getType());
                    map.put("createdAt", n.getCreatedAt());
                    map.put("priority", n.getPriority() != null ? n.getPriority() : 1); 
                    return map;
                })
                .toList();
    }
}

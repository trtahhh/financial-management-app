package com.example.finance.service;

import com.example.finance.dto.NotificationDTO;
import com.example.finance.entity.Notification;
import com.example.finance.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

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
        noti.setUserId(userId);
        noti.setCategoryId(categoryId);
        noti.setBudgetId(budgetId);
        noti.setType("OVER_BUDGET");
        noti.setMessage("Bạn đã vượt quá ngân sách (" + limit + ") cho danh mục/tháng này! Tổng chi: " + spending);
        noti.setMonth(month);
        noti.setYear(year);
        noti.setIsRead(false);
        noti.setIsDeleted(false);
        noti.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(noti);
    }

    public void createLowBalanceNotification(Long userId, Long walletId, BigDecimal balance) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setWalletId(walletId);
        notification.setType("LOW_BALANCE");
        notification.setMessage("Số dư ví đã xuống dưới " + balance + " VNĐ.");
        notification.setIsRead(false);
        notification.setIsDeleted(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public void createGoalNotification(Long userId, Long goalId, String type, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setGoalId(goalId);
        notification.setType(type); 
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setIsDeleted(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public void createGoalCompletedNotification(Long userId, Long goalId, String goalName) {
        Notification noti = new Notification();
        noti.setUserId(userId);
        noti.setGoalId(goalId);
        noti.setType("GOAL_COMPLETED");
        noti.setMessage("Chúc mừng! Bạn đã hoàn thành mục tiêu: " + goalName);
        noti.setIsRead(false);
        noti.setIsDeleted(false);
        noti.setCreatedAt(java.time.LocalDateTime.now());
        notificationRepository.save(noti);
    }


    public NotificationDTO toDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setUserId(n.getUserId());
        dto.setWalletId(n.getWalletId());
        dto.setBudgetId(n.getBudgetId());
        dto.setGoalId(n.getGoalId());
        dto.setTransactionId(n.getTransactionId());
        dto.setCategoryId(n.getCategoryId());
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
}

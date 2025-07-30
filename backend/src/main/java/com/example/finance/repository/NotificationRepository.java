package com.example.finance.repository;

import com.example.finance.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndCategoryIdAndTypeAndMonthAndYearAndIsDeletedFalse(
        Long userId, Long categoryId, String type, Integer month, Integer year
    );
}

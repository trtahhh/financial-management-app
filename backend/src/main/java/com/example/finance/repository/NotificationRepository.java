package com.example.finance.repository;

import com.example.finance.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Notification n " +
           "WHERE n.user.id = :userId AND n.category.id = :categoryId AND n.type = :type " +
           "AND n.month = :month AND n.year = :year AND n.isDeleted = false")
    boolean existsByUserIdAndCategoryIdAndTypeAndMonthAndYearAndIsDeletedFalse(
        @Param("userId") Long userId, 
        @Param("categoryId") Long categoryId, 
        @Param("type") String type, 
        @Param("month") Integer month, 
        @Param("year") Integer year
    );
}

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

    // Thêm các method mới:
    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Notification n " +
           "WHERE n.budget.id = :budgetId AND n.type = :type " +
           "AND n.month = :month AND n.year = :year AND n.isDeleted = false")
    boolean existsByBudgetIdAndTypeAndMonthAndYearAndIsDeletedFalse(
        @Param("budgetId") Long budgetId, 
        @Param("type") String type, 
        @Param("month") Integer month, 
        @Param("year") Integer year
    );

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Kiểm tra xem đã có notification cho goal với type cụ thể chưa
     */
    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Notification n " +
           "WHERE n.user.id = :userId AND n.goal.id = :goalId AND n.type = :type AND n.isDeleted = false")
    boolean existsByUserIdAndGoalIdAndTypeAndIsDeletedFalse(
        @Param("userId") Long userId, 
        @Param("goalId") Long goalId, 
        @Param("type") String type
    );

    /**
     * Đếm số notification chưa đọc của user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.isDeleted = false")
    Long countByUserIdAndIsReadFalseAndIsDeletedFalse(@Param("userId") Long userId);

    /**
     * Lấy danh sách notification chưa đọc của user (không sắp xếp)
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.isDeleted = false")
    List<Notification> findByUserIdAndIsReadFalseAndIsDeletedFalse(@Param("userId") Long userId);

    /**
     * Lấy tất cả notifications liên quan đến một goal cụ thể
     */
    @Query("SELECT n FROM Notification n WHERE n.goal.id = :goalId")
    List<Notification> findByGoalId(@Param("goalId") Long goalId);
}
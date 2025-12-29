package com.example.finance.service;

import com.example.finance.dto.NotificationDTO;
import com.example.finance.entity.*;
import com.example.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private GoalRepository goalRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Category testCategory;
    private Wallet testWallet;
    private Budget testBudget;
    private Goal testGoal;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setName("Main Wallet");
        testWallet.setBalance(new BigDecimal("1000000"));

        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setAmount(new BigDecimal("5000000"));

        testGoal = new Goal();
        testGoal.setId(1L);
        testGoal.setName("Save for vacation");

        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUser(testUser);
        testNotification.setType("OVER_BUDGET");
        testNotification.setMessage("Budget exceeded");
        testNotification.setIsRead(false);
        testNotification.setIsDeleted(false);
        testNotification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should get user notifications")
    void testGetUserNotifications() {
        // Arrange
        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setUser(testUser);
        notification2.setType("LOW_BALANCE");
        notification2.setMessage("Low balance alert");
        notification2.setIsRead(false);
        notification2.setIsDeleted(false);

        when(notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L))
            .thenReturn(Arrays.asList(testNotification, notification2));

        // Act
        List<NotificationDTO> result = notificationService.getUserNotifications(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(notificationRepository).findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("Should return empty list when no notifications")
    void testGetUserNotifications_Empty() {
        // Arrange
        when(notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L))
            .thenReturn(Arrays.asList());

        // Act
        List<NotificationDTO> result = notificationService.getUserNotifications(1L);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should mark notification as read")
    void testMarkAsRead() {
        // Arrange
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        notificationService.markAsRead(1L);

        // Assert
        verify(notificationRepository).save(argThat(n -> n.getIsRead()));
    }

    @Test
    @DisplayName("Should delete notification")
    void testDeleteNotification() {
        // Arrange
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        notificationService.deleteNotification(1L);

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            n.getIsDeleted() && n.getDeletedAt() != null
        ));
    }

    @Test
    @DisplayName("Should check if over budget notification exists")
    void testExistsOverBudget_True() {
        // Arrange
        when(notificationRepository.existsByUserIdAndCategoryIdAndTypeAndMonthAndYearAndIsDeletedFalse(
            1L, 1L, "OVER_BUDGET", 12, 2025)).thenReturn(true);

        // Act
        boolean result = notificationService.existsOverBudget(1L, 1L, 12, 2025);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when over budget notification does not exist")
    void testExistsOverBudget_False() {
        // Arrange
        when(notificationRepository.existsByUserIdAndCategoryIdAndTypeAndMonthAndYearAndIsDeletedFalse(
            1L, 1L, "OVER_BUDGET", 12, 2025)).thenReturn(false);

        // Act
        boolean result = notificationService.existsOverBudget(1L, 1L, 12, 2025);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should create over budget notification")
    void testCreateOverBudgetNotification() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        notificationService.createOverBudgetNotification(
            1L, 1L, 1L, 
            new BigDecimal("6000000"), 
            new BigDecimal("5000000"), 
            12, 2025
        );

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            "OVER_BUDGET".equals(n.getType()) &&
            n.getMessage().contains("vượt quá ngân sách") &&
            n.getMonth() == 12 &&
            n.getYear() == 2025
        ));
    }

    @Test
    @DisplayName("Should create low balance notification")
    void testCreateLowBalanceNotification() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        notificationService.createLowBalanceNotification(1L, 1L, new BigDecimal("100000"));

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            "LOW_BALANCE".equals(n.getType()) &&
            n.getMessage().contains("Số dư ví")
        ));
    }

    @Test
    @DisplayName("Should create goal notification")
    void testCreateGoalNotification() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(goalRepository.findById(1L)).thenReturn(Optional.of(testGoal));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        notificationService.createGoalNotification(
            1L, 1L, "GOAL_ACHIEVED", "You have achieved your goal!"
        );

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            "GOAL_ACHIEVED".equals(n.getType()) &&
            "You have achieved your goal!".equals(n.getMessage())
        ));
    }

    @Test
    @DisplayName("Should handle null user when creating notification")
    void testCreateOverBudgetNotification_NullUser() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        notificationService.createOverBudgetNotification(
            999L, 1L, 1L, 
            new BigDecimal("6000000"), 
            new BigDecimal("5000000"), 
            12, 2025
        );

        // Assert - Should still save notification even with null user
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should filter out deleted notifications")
    void testGetUserNotifications_OnlyNotDeleted() {
        // Arrange
        Notification deletedNotification = new Notification();
        deletedNotification.setId(3L);
        deletedNotification.setIsDeleted(true);

        when(notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L))
            .thenReturn(Arrays.asList(testNotification));

        // Act
        List<NotificationDTO> result = notificationService.getUserNotifications(1L);

        // Assert
        assertEquals(1, result.size());
        verify(notificationRepository).findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("Should set month and year for budget notification")
    void testCreateOverBudgetNotification_MonthYear() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        notificationService.createOverBudgetNotification(
            1L, 1L, 1L, 
            new BigDecimal("6000000"), 
            new BigDecimal("5000000"), 
            6, 2024
        );

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            n.getMonth() == 6 && n.getYear() == 2024
        ));
    }

    @Test
    @DisplayName("Should create notification with correct spending and limit")
    void testCreateOverBudgetNotification_Amounts() {
        // Arrange
        BigDecimal spending = new BigDecimal("7500000");
        BigDecimal limit = new BigDecimal("5000000");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        notificationService.createOverBudgetNotification(
            1L, 1L, 1L, spending, limit, 12, 2025
        );

        // Assert
        verify(notificationRepository).save(argThat(n -> 
            n.getMessage().contains("7500000") &&
            n.getMessage().contains("5000000")
        ));
    }
}

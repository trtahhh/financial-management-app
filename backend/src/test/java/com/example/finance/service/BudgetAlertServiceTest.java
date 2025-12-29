package com.example.finance.service;

import com.example.finance.entity.*;
import com.example.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetAlertServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private BudgetCalculationService budgetCalculationService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetAlertService budgetAlertService;

    private User testUser;
    private Category testCategory;
    private Wallet testWallet;
    private Budget testBudget;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        // Set configuration properties
        ReflectionTestUtils.setField(budgetAlertService, "emailVerificationEnabled", true);
        ReflectionTestUtils.setField(budgetAlertService, "budgetAlertsEnabled", true);
        ReflectionTestUtils.setField(budgetAlertService, "budgetWarningThreshold", 80);
        ReflectionTestUtils.setField(budgetAlertService, "budgetExceededThreshold", 100);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setName("Main Wallet");

        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setAmount(new BigDecimal("2000000"));
        testBudget.setSpentAmount(new BigDecimal("1000000"));
        testBudget.setMonth(12);
        testBudget.setYear(2025);
        testBudget.setUser(testUser);
        testBudget.setCategory(testCategory);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setType("expense");
        testTransaction.setAmount(new BigDecimal("500000"));
        testTransaction.setDate(LocalDate.of(2025, 12, 15));
        testTransaction.setUser(testUser);
        testTransaction.setCategory(testCategory);
        testTransaction.setWallet(testWallet);
    }

    @Test
    @DisplayName("Should skip alert check for income transaction")
    void testCheckBudgetAlert_IncomeTransaction() {
        // Arrange
        testTransaction.setType("income");

        // Act
        budgetAlertService.checkBudgetAlert(testTransaction);

        // Assert
        verify(budgetRepository, never()).findByUserAndMonthAndCategoryAndIsDeletedFalse(
                anyLong(), anyInt(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("Should skip alert when no budget exists")
    void testCheckBudgetAlert_NoBudget() {
        // Arrange
        when(budgetRepository.findByUserAndMonthAndCategoryAndIsDeletedFalse(
                1L, 12, 2025, 1L)).thenReturn(Optional.empty());

        // Act
        budgetAlertService.checkBudgetAlert(testTransaction);

        // Assert
        verify(budgetRepository).findByUserAndMonthAndCategoryAndIsDeletedFalse(1L, 12, 2025, 1L);
        verify(budgetCalculationService, never()).calculateSpentAmount(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should not send alert when spending is under threshold")
    void testCheckBudgetAlert_UnderThreshold() {
        // Arrange
        BigDecimal totalSpent = new BigDecimal("1000000"); // 50% of budget

        when(budgetRepository.findByUserAndMonthAndCategoryAndIsDeletedFalse(
                1L, 12, 2025, 1L)).thenReturn(Optional.of(testBudget));
        when(budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025))
                .thenReturn(totalSpent);

        // Act
        budgetAlertService.checkBudgetAlert(testTransaction);

        // Assert
        verify(notificationService, never()).createBudgetNotification(
                anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyInt());
        // Email is called once from checkBudgetAlert (budget update email)
        verify(emailService).sendBudgetAlertEmail(anyString(), anyString(), anyString(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should send warning alert when spending reaches 80%")
    void testCheckBudgetAlert_WarningThreshold() {
        // Arrange
        BigDecimal totalSpent = new BigDecimal("1600000"); // 80% of budget

        when(budgetRepository.findByUserAndMonthAndCategoryAndIsDeletedFalse(
                1L, 12, 2025, 1L)).thenReturn(Optional.of(testBudget));
        when(budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025))
                .thenReturn(totalSpent);
        when(notificationService.existsByBudgetAndType(1L, "BUDGET_WARNING", 12, 2025))
                .thenReturn(false);

        // Act
        budgetAlertService.checkBudgetAlert(testTransaction);

        // Assert
        verify(notificationService).createBudgetNotification(
                eq(1L), eq(1L), eq(1L), anyString(), eq("BUDGET_WARNING"), eq(12), eq(2025));
        // Email is called twice: once in createBudgetNotification, once in checkBudgetAlert
        verify(emailService, times(2)).sendBudgetAlertEmail(
                eq("testuser@example.com"),
                eq("testuser"),
                eq("Food"),
                eq(1600000.0),
                eq(2000000.0));
        verify(budgetRepository).save(testBudget);
    }

    @Test
    @DisplayName("Should send exceeded alert when spending reaches 100%")
    void testCheckBudgetAlert_ExceededThreshold() {
        // Arrange
        BigDecimal totalSpent = new BigDecimal("2500000"); // 125% of budget

        when(budgetRepository.findByUserAndMonthAndCategoryAndIsDeletedFalse(
                1L, 12, 2025, 1L)).thenReturn(Optional.of(testBudget));
        when(budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025))
                .thenReturn(totalSpent);
        when(notificationService.existsByBudgetAndType(1L, "BUDGET_EXCEEDED", 12, 2025))
                .thenReturn(false);

        // Act
        budgetAlertService.checkBudgetAlert(testTransaction);

        // Assert
        verify(notificationService).createBudgetNotification(
                eq(1L), eq(1L), eq(1L), anyString(), eq("BUDGET_EXCEEDED"), eq(12), eq(2025));
        // Email is called twice: once in createBudgetNotification, once in checkBudgetAlert
        verify(emailService, times(2)).sendBudgetAlertEmail(
                eq("testuser@example.com"),
                eq("testuser"),
                eq("Food"),
                eq(2500000.0),
                eq(2000000.0));
    }

    @Test
    @DisplayName("Should not send email when budget alerts are disabled")
    void testCheckBudgetAlert_AlertsDisabled() {
        // Arrange
        ReflectionTestUtils.setField(budgetAlertService, "budgetAlertsEnabled", false);
        BigDecimal totalSpent = new BigDecimal("1700000"); // 85% of budget

        when(budgetRepository.findByUserAndMonthAndCategoryAndIsDeletedFalse(
                1L, 12, 2025, 1L)).thenReturn(Optional.of(testBudget));
        when(budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025))
                .thenReturn(totalSpent);
        when(notificationService.existsByBudgetAndType(1L, "BUDGET_WARNING", 12, 2025))
                .thenReturn(false);

        // Act
        budgetAlertService.checkBudgetAlert(testTransaction);

        // Assert
        verify(notificationService).createBudgetNotification(
                anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyInt());
        verify(emailService, never()).sendBudgetAlertEmail(anyString(), anyString(), anyString(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should not send email when user has no email address")
    void testCheckBudgetAlert_NoUserEmail() {
        // Arrange
        testUser.setEmail(null);
        BigDecimal totalSpent = new BigDecimal("1700000"); // 85% of budget

        when(budgetRepository.findByUserAndMonthAndCategoryAndIsDeletedFalse(
                1L, 12, 2025, 1L)).thenReturn(Optional.of(testBudget));
        when(budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025))
                .thenReturn(totalSpent);
        when(notificationService.existsByBudgetAndType(1L, "BUDGET_WARNING", 12, 2025))
                .thenReturn(false);

        // Act
        budgetAlertService.checkBudgetAlert(testTransaction);

        // Assert
        verify(notificationService).createBudgetNotification(
                anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyInt());
        verify(emailService, never()).sendBudgetAlertEmail(anyString(), anyString(), anyString(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should update budget spent amount")
    void testCheckBudgetAlert_UpdatesSpentAmount() {
        // Arrange
        BigDecimal totalSpent = new BigDecimal("1700000");

        when(budgetRepository.findByUserAndMonthAndCategoryAndIsDeletedFalse(
                1L, 12, 2025, 1L)).thenReturn(Optional.of(testBudget));
        when(budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025))
                .thenReturn(totalSpent);
        when(notificationService.existsByBudgetAndType(1L, "BUDGET_WARNING", 12, 2025))
                .thenReturn(false);

        // Act
        budgetAlertService.checkBudgetAlert(testTransaction);

        // Assert
        verify(budgetRepository).save(testBudget);
    }

    @Test
    @DisplayName("Should not send duplicate notifications")
    void testCheckBudgetAlert_NoDuplicateNotification() {
        // Arrange
        BigDecimal totalSpent = new BigDecimal("1700000");

        when(budgetRepository.findByUserAndMonthAndCategoryAndIsDeletedFalse(
                1L, 12, 2025, 1L)).thenReturn(Optional.of(testBudget));
        when(budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025))
                .thenReturn(totalSpent);
        when(notificationService.existsByBudgetAndType(1L, "BUDGET_WARNING", 12, 2025))
                .thenReturn(true); // Already exists

        // Act
        budgetAlertService.checkBudgetAlert(testTransaction);

        // Assert
        verify(notificationService, never()).createBudgetNotification(
                anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyInt());
        // Email is called once from checkBudgetAlert (budget update email only)
        verify(emailService).sendBudgetAlertEmail(anyString(), anyString(), anyString(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should handle exception when sending email fails")
    void testCheckBudgetAlert_EmailFailure() {
        // Arrange
        BigDecimal totalSpent = new BigDecimal("1700000");

        when(budgetRepository.findByUserAndMonthAndCategoryAndIsDeletedFalse(
                1L, 12, 2025, 1L)).thenReturn(Optional.of(testBudget));
        when(budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025))
                .thenReturn(totalSpent);
        when(notificationService.existsByBudgetAndType(1L, "BUDGET_WARNING", 12, 2025))
                .thenReturn(false);
        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendBudgetAlertEmail(anyString(), anyString(), anyString(), anyDouble(), anyDouble());

        // Act - Should not throw exception
        budgetAlertService.checkBudgetAlert(testTransaction);

        // Assert - Notification still created
        verify(notificationService).createBudgetNotification(
                anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyInt());
    }
}

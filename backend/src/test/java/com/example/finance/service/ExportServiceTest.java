package com.example.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private GoalService goalService;

    @InjectMocks
    private ExportService exportService;

    @BeforeEach
    void setUp() {
        // Setup mock data
        lenient().when(transactionService.getRecentTransactions(anyLong(), anyInt()))
            .thenReturn(createMockTransactions());
        lenient().when(budgetService.getBudgetVsActualByDate(anyLong(), any(), any()))
            .thenReturn(createMockBudgets());
        lenient().when(goalService.getActiveGoals(anyLong()))
            .thenReturn(new ArrayList<>());
    }

    @Test
    @DisplayName("Should export to Excel successfully")
    void testExportToExcel_Success() throws IOException {
        // Arrange
        Long userId = 1L;
        String reportType = "summary";
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Act
        byte[] result = exportService.exportToExcel(userId, reportType, startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(transactionService).getRecentTransactions(userId, 100);
    }

    @Test
    @DisplayName("Should export transactions report to Excel")
    void testExportToExcel_TransactionReport() throws IOException {
        // Arrange
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Act
        byte[] result = exportService.exportToExcel(userId, "transaction", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(transactionService).getRecentTransactions(userId, 100);
        verify(budgetService, never()).getBudgetVsActualByDate(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should export budget report to Excel")
    void testExportToExcel_BudgetReport() throws IOException {
        // Arrange
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Act
        byte[] result = exportService.exportToExcel(userId, "budget", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(budgetService).getBudgetVsActualByDate(userId, startDate, endDate);
    }

    @Test
    @DisplayName("Should export goal report to Excel")
    void testExportToExcel_GoalReport() throws IOException {
        // Arrange
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Act
        byte[] result = exportService.exportToExcel(userId, "goal", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(goalService).getGoalProgress(userId);
    }

    @Test
    @DisplayName("Should export to PDF successfully")
    void testExportToPDF_Success() throws Exception {
        // Arrange
        Long userId = 1L;
        String reportType = "summary";
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Act
        byte[] result = exportService.exportToPDF(userId, reportType, startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(transactionService).getRecentTransactions(userId, 100);
    }

    @Test
    @DisplayName("Should export transactions report to PDF")
    void testExportToPDF_TransactionReport() throws Exception {
        // Arrange
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Act
        byte[] result = exportService.exportToPDF(userId, "transaction", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(transactionService).getRecentTransactions(userId, 100);
    }

    @Test
    @DisplayName("Should generate Excel report with date strings")
    void testGenerateExcelReport_WithDateStrings() throws IOException {
        // Arrange
        Long userId = 1L;
        String startDateStr = "2025-12-01";
        String endDateStr = "2025-12-31";

        // Act
        byte[] result = exportService.generateExcelReport(userId, startDateStr, endDateStr);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(transactionService).getRecentTransactions(userId, 100);
    }

    @Test
    @DisplayName("Should generate Excel report with null dates")
    void testGenerateExcelReport_NullDates() throws IOException {
        // Arrange
        Long userId = 1L;

        // Act
        byte[] result = exportService.generateExcelReport(userId, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(transactionService).getRecentTransactions(userId, 100);
    }

    @Test
    @DisplayName("Should generate PDF report with date strings")
    void testGeneratePdfReport_WithDateStrings() throws Exception {
        // Arrange
        Long userId = 1L;
        String startDateStr = "2025-12-01";
        String endDateStr = "2025-12-31";

        // Act
        byte[] result = exportService.generatePdfReport(userId, startDateStr, endDateStr);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(transactionService).getRecentTransactions(userId, 100);
    }

    @Test
    @DisplayName("Should generate PDF report with null dates")
    void testGeneratePdfReport_NullDates() throws Exception {
        // Arrange
        Long userId = 1L;

        // Act
        byte[] result = exportService.generatePdfReport(userId, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(transactionService).getRecentTransactions(userId, 100);
    }

    @Test
    @DisplayName("Should handle empty transaction data in Excel")
    void testExportToExcel_EmptyTransactions() throws IOException {
        // Arrange
        when(transactionService.getRecentTransactions(anyLong(), anyInt()))
            .thenReturn(Collections.emptyList());
        
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Act
        byte[] result = exportService.exportToExcel(userId, "transaction", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Should handle empty budget data in Excel")
    void testExportToExcel_EmptyBudgets() throws IOException {
        // Arrange
        when(budgetService.getBudgetVsActualByDate(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());
        
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Act
        byte[] result = exportService.exportToExcel(userId, "budget", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Should export summary report with all data types")
    void testExportToExcel_SummaryReport() throws IOException {
        // Arrange
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Act
        byte[] result = exportService.exportToExcel(userId, "summary", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(transactionService).getRecentTransactions(userId, 100);
        verify(budgetService).getBudgetVsActualByDate(userId, startDate, endDate);
        verify(goalService).getGoalProgress(userId);
    }

    @Test
    @DisplayName("Should export summary report to PDF")
    void testExportToPDF_SummaryReport() throws Exception {
        // Arrange
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // Act
        byte[] result = exportService.exportToPDF(userId, "summary", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(transactionService).getRecentTransactions(userId, 100);
        verify(budgetService).getBudgetVsActualByDate(userId, startDate, endDate);
        verify(goalService).getGoalProgress(userId);
    }

    private List<Map<String, Object>> createMockTransactions() {
        List<Map<String, Object>> transactions = new ArrayList<>();
        
        Map<String, Object> tx1 = new HashMap<>();
        tx1.put("id", 1L);
        tx1.put("date", LocalDate.now());
        tx1.put("type", "expense");
        tx1.put("categoryName", "Food");
        tx1.put("description", "Lunch");
        tx1.put("amount", "50000");
        tx1.put("walletName", "Main Wallet");
        transactions.add(tx1);
        
        Map<String, Object> tx2 = new HashMap<>();
        tx2.put("id", 2L);
        tx2.put("date", LocalDate.now().minusDays(1));
        tx2.put("type", "income");
        tx2.put("categoryName", "Salary");
        tx2.put("description", "Monthly salary");
        tx2.put("amount", "10000000");
        tx2.put("walletName", "Main Wallet");
        transactions.add(tx2);
        
        return transactions;
    }

    private List<Map<String, Object>> createMockBudgets() {
        List<Map<String, Object>> budgets = new ArrayList<>();
        
        Map<String, Object> budget1 = new HashMap<>();
        budget1.put("categoryName", "Food");
        budget1.put("budgetAmount", "2000000");
        budget1.put("actualAmount", "1500000");
        budget1.put("remainingAmount", "500000");
        budget1.put("usagePercent", "75");
        budgets.add(budget1);
        
        Map<String, Object> budget2 = new HashMap<>();
        budget2.put("categoryName", "Transportation");
        budget2.put("budgetAmount", "1000000");
        budget2.put("actualAmount", "800000");
        budget2.put("remainingAmount", "200000");
        budget2.put("usagePercent", "80");
        budgets.add(budget2);
        
        return budgets;
    }
}

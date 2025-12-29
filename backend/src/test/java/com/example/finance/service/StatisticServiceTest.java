package com.example.finance.service;

import com.example.finance.dto.CategoryStatisticDTO;
import com.example.finance.dto.SummaryDTO;
import com.example.finance.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Statistic Service Tests")
class StatisticServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private StatisticService statisticService;

    @Test
    @DisplayName("Should calculate summary with income and expense")
    void testGetSummary_WithIncomeAndExpense() {
        // Arrange
        Long userId = 1L;
        Integer month = 12;
        Integer year = 2025;
        
        when(transactionRepository.sumAmountByUserAndType(userId, "income", month, year))
            .thenReturn(new BigDecimal("5000000.00"));
        when(transactionRepository.sumAmountByUserAndType(userId, "expense", month, year))
            .thenReturn(new BigDecimal("3000000.00"));

        // Act
        SummaryDTO result = statisticService.getSummary(userId, month, year);

        // Assert
        assertNotNull(result);
        assertEquals(5000000.00, result.getTotalIncome());
        assertEquals(3000000.00, result.getTotalExpense());
        assertEquals(2000000.00, result.getBalance()); // 5M - 3M = 2M
        
        verify(transactionRepository).sumAmountByUserAndType(userId, "income", month, year);
        verify(transactionRepository).sumAmountByUserAndType(userId, "expense", month, year);
    }

    @Test
    @DisplayName("Should handle null income")
    void testGetSummary_NullIncome() {
        // Arrange
        Long userId = 1L;
        Integer month = 12;
        Integer year = 2025;
        
        when(transactionRepository.sumAmountByUserAndType(userId, "income", month, year))
            .thenReturn(null);
        when(transactionRepository.sumAmountByUserAndType(userId, "expense", month, year))
            .thenReturn(new BigDecimal("1000000.00"));

        // Act
        SummaryDTO result = statisticService.getSummary(userId, month, year);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getTotalIncome());
        assertEquals(1000000.00, result.getTotalExpense());
        assertEquals(-1000000.00, result.getBalance()); // 0 - 1M = -1M
    }

    @Test
    @DisplayName("Should handle null expense")
    void testGetSummary_NullExpense() {
        // Arrange
        Long userId = 1L;
        Integer month = 12;
        Integer year = 2025;
        
        when(transactionRepository.sumAmountByUserAndType(userId, "income", month, year))
            .thenReturn(new BigDecimal("2000000.00"));
        when(transactionRepository.sumAmountByUserAndType(userId, "expense", month, year))
            .thenReturn(null);

        // Act
        SummaryDTO result = statisticService.getSummary(userId, month, year);

        // Assert
        assertNotNull(result);
        assertEquals(2000000.00, result.getTotalIncome());
        assertEquals(0.0, result.getTotalExpense());
        assertEquals(2000000.00, result.getBalance()); // 2M - 0 = 2M
    }

    @Test
    @DisplayName("Should handle both null income and expense")
    void testGetSummary_BothNull() {
        // Arrange
        Long userId = 1L;
        Integer month = 12;
        Integer year = 2025;
        
        when(transactionRepository.sumAmountByUserAndType(userId, "income", month, year))
            .thenReturn(null);
        when(transactionRepository.sumAmountByUserAndType(userId, "expense", month, year))
            .thenReturn(null);

        // Act
        SummaryDTO result = statisticService.getSummary(userId, month, year);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getTotalIncome());
        assertEquals(0.0, result.getTotalExpense());
        assertEquals(0.0, result.getBalance());
    }

    @Test
    @DisplayName("Should handle negative balance")
    void testGetSummary_NegativeBalance() {
        // Arrange
        Long userId = 1L;
        Integer month = 12;
        Integer year = 2025;
        
        when(transactionRepository.sumAmountByUserAndType(userId, "income", month, year))
            .thenReturn(new BigDecimal("1000000.00"));
        when(transactionRepository.sumAmountByUserAndType(userId, "expense", month, year))
            .thenReturn(new BigDecimal("3000000.00"));

        // Act
        SummaryDTO result = statisticService.getSummary(userId, month, year);

        // Assert
        assertNotNull(result);
        assertEquals(1000000.00, result.getTotalIncome());
        assertEquals(3000000.00, result.getTotalExpense());
        assertEquals(-2000000.00, result.getBalance()); // 1M - 3M = -2M (overspent)
    }

    @Test
    @DisplayName("Should get category statistics")
    void testGetByCategory() {
        // Arrange
        Long userId = 1L;
        Integer month = 12;
        Integer year = 2025;
        
        CategoryStatisticDTO cat1 = new CategoryStatisticDTO("Food", "#FF5733", new BigDecimal("500000.0"), 5L);
        CategoryStatisticDTO cat2 = new CategoryStatisticDTO("Transport", "#33FF57", new BigDecimal("300000.0"), 3L);
        CategoryStatisticDTO cat3 = new CategoryStatisticDTO("Shopping", "#3357FF", new BigDecimal("700000.0"), 7L);
        
        when(transactionRepository.findExpenseStatisticsByCategory(userId, month, year))
            .thenReturn(Arrays.asList(cat1, cat2, cat3));

        // Act
        List<CategoryStatisticDTO> result = statisticService.getByCategory(userId, month, year);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        
        assertEquals("Food", result.get(0).getCategoryName());
        assertEquals(new BigDecimal("500000.0"), result.get(0).getTotalAmount());
        assertEquals(5L, result.get(0).getTransactionCount());
        
        assertEquals("Transport", result.get(1).getCategoryName());
        assertEquals(new BigDecimal("300000.0"), result.get(1).getTotalAmount());
        
        assertEquals("Shopping", result.get(2).getCategoryName());
        assertEquals(new BigDecimal("700000.0"), result.get(2).getTotalAmount());
        
        verify(transactionRepository).findExpenseStatisticsByCategory(userId, month, year);
    }

    @Test
    @DisplayName("Should return empty list when no categories")
    void testGetByCategory_Empty() {
        // Arrange
        Long userId = 1L;
        Integer month = 12;
        Integer year = 2025;
        
        when(transactionRepository.findExpenseStatisticsByCategory(userId, month, year))
            .thenReturn(Arrays.asList());

        // Act
        List<CategoryStatisticDTO> result = statisticService.getByCategory(userId, month, year);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should calculate summary for different months")
    void testGetSummary_DifferentMonths() {
        // Arrange
        Long userId = 1L;
        
        // January
        when(transactionRepository.sumAmountByUserAndType(userId, "income", 1, 2025))
            .thenReturn(new BigDecimal("3000000.00"));
        when(transactionRepository.sumAmountByUserAndType(userId, "expense", 1, 2025))
            .thenReturn(new BigDecimal("2000000.00"));
        
        // February
        when(transactionRepository.sumAmountByUserAndType(userId, "income", 2, 2025))
            .thenReturn(new BigDecimal("4000000.00"));
        when(transactionRepository.sumAmountByUserAndType(userId, "expense", 2, 2025))
            .thenReturn(new BigDecimal("3500000.00"));

        // Act
        SummaryDTO jan = statisticService.getSummary(userId, 1, 2025);
        SummaryDTO feb = statisticService.getSummary(userId, 2, 2025);

        // Assert
        assertEquals(1000000.00, jan.getBalance()); // 3M - 2M
        assertEquals(500000.00, feb.getBalance()); // 4M - 3.5M
    }

    @Test
    @DisplayName("Should handle large amounts correctly")
    void testGetSummary_LargeAmounts() {
        // Arrange
        Long userId = 1L;
        Integer month = 12;
        Integer year = 2025;
        
        when(transactionRepository.sumAmountByUserAndType(userId, "income", month, year))
            .thenReturn(new BigDecimal("100000000.00")); // 100M
        when(transactionRepository.sumAmountByUserAndType(userId, "expense", month, year))
            .thenReturn(new BigDecimal("75000000.00")); // 75M

        // Act
        SummaryDTO result = statisticService.getSummary(userId, month, year);

        // Assert
        assertNotNull(result);
        assertEquals(100000000.00, result.getTotalIncome());
        assertEquals(75000000.00, result.getTotalExpense());
        assertEquals(25000000.00, result.getBalance()); // 100M - 75M = 25M
    }

    @Test
    @DisplayName("Should get statistics for specific year")
    void testGetSummary_SpecificYear() {
        // Arrange
        Long userId = 1L;
        Integer month = 12;
        
        // 2024
        when(transactionRepository.sumAmountByUserAndType(userId, "income", month, 2024))
            .thenReturn(new BigDecimal("5000000.00"));
        when(transactionRepository.sumAmountByUserAndType(userId, "expense", month, 2024))
            .thenReturn(new BigDecimal("4000000.00"));
        
        // 2025
        when(transactionRepository.sumAmountByUserAndType(userId, "income", month, 2025))
            .thenReturn(new BigDecimal("6000000.00"));
        when(transactionRepository.sumAmountByUserAndType(userId, "expense", month, 2025))
            .thenReturn(new BigDecimal("5000000.00"));

        // Act
        SummaryDTO summary2024 = statisticService.getSummary(userId, month, 2024);
        SummaryDTO summary2025 = statisticService.getSummary(userId, month, 2025);

        // Assert
        assertEquals(1000000.00, summary2024.getBalance()); // 2024: 5M - 4M
        assertEquals(1000000.00, summary2025.getBalance()); // 2025: 6M - 5M
    }

    @Test
    @DisplayName("Should get category statistics with single category")
    void testGetByCategory_SingleCategory() {
        // Arrange
        Long userId = 1L;
        Integer month = 12;
        Integer year = 2025;
        
        CategoryStatisticDTO cat = new CategoryStatisticDTO("Food", "#FF5733", new BigDecimal("1500000.0"), 15L);
        
        when(transactionRepository.findExpenseStatisticsByCategory(userId, month, year))
            .thenReturn(Arrays.asList(cat));

        // Act
        List<CategoryStatisticDTO> result = statisticService.getByCategory(userId, month, year);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).getCategoryName());
        assertEquals(new BigDecimal("1500000.0"), result.get(0).getTotalAmount());
        assertEquals(15L, result.get(0).getTransactionCount());
    }
}

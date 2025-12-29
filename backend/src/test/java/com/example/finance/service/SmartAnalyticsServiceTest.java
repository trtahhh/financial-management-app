package com.example.finance.service;

import com.example.finance.dto.SmartAnalyticsResponse;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.Category;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartAnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private SmartAnalyticsService smartAnalyticsService;

    private Transaction testTransaction;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Ăn uống");

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setType("expense");
        testTransaction.setAmount(new BigDecimal("500000"));
        testTransaction.setDate(LocalDate.now());
        testTransaction.setCategory(testCategory);
        testTransaction.setNote("Mua cơm gà");
    }

    @Test
    @DisplayName("Should detect largest expense query")
    void testAnalyzeQuery_LargestExpense() {
        String query = "Khoản chi lớn nhất tháng này";
        List<Transaction> transactions = Arrays.asList(
            createTransaction(1L, "500000", testCategory),
            createTransaction(2L, "1000000", testCategory)
        );

        when(transactionRepository.findByUserIdAndTypeBetweenDates(
            eq(1L), eq("expense"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(transactions);

        SmartAnalyticsResponse response = smartAnalyticsService.analyzeQuery(query, 1L);

        assertNotNull(response);
        assertNotNull(response.getMainMessage());
    }

    @Test
    @DisplayName("Should detect top expenses query")
    void testAnalyzeQuery_TopExpenses() {
        String query = "Top chi tiêu tháng này";
        List<Transaction> transactions = Arrays.asList(
            createTransaction(1L, "500000", testCategory),
            createTransaction(2L, "300000", testCategory)
        );

        when(transactionRepository.findByUserIdAndTypeBetweenDates(
            eq(1L), eq("expense"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(transactions);

        SmartAnalyticsResponse response = smartAnalyticsService.analyzeQuery(query, 1L);

        assertNotNull(response);
        assertNotNull(response.getMainMessage());
    }

    @Test
    @DisplayName("Should detect category analysis query")
    void testAnalyzeQuery_CategoryAnalysis() {
        String query = "Category nào tôi chi nhiều nhất";
        List<Transaction> transactions = Arrays.asList(
            createTransaction(1L, "500000", testCategory),
            createTransaction(2L, "300000", testCategory)
        );

        when(transactionRepository.findByUserIdAndTypeBetweenDates(
            eq(1L), eq("expense"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(transactions);

        SmartAnalyticsResponse response = smartAnalyticsService.analyzeQuery(query, 1L);

        assertNotNull(response);
        assertNotNull(response.getMainMessage());
    }

    @Test
    @DisplayName("Should detect monthly total query")
    void testAnalyzeQuery_MonthlyTotal() {
        String query = "Tổng chi tháng này bao nhiêu";
        List<Transaction> transactions = Arrays.asList(
            createTransaction(1L, "500000", testCategory),
            createTransaction(2L, "300000", testCategory)
        );

        when(transactionRepository.findByUserIdAndTypeBetweenDates(
            eq(1L), eq("expense"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(transactions);

        SmartAnalyticsResponse response = smartAnalyticsService.analyzeQuery(query, 1L);

        assertNotNull(response);
        assertNotNull(response.getMainMessage());
    }

    @Test
    @DisplayName("Should detect saving advice query")
    void testAnalyzeQuery_SavingAdvice() {
        String query = "Cho tôi lời khuyên tiết kiệm";
        
        SmartAnalyticsResponse response = smartAnalyticsService.analyzeQuery(query, 1L);

        assertNotNull(response);
        assertNotNull(response.getMainMessage());
    }

    @Test
    @DisplayName("Should detect spending limit query")
    void testAnalyzeQuery_SpendingLimit() {
        String query = "Tôi nên chi bao nhiêu là hợp lý";
        
        SmartAnalyticsResponse response = smartAnalyticsService.analyzeQuery(query, 1L);

        assertNotNull(response);
        assertNotNull(response.getMainMessage());
    }

    @Test
    @DisplayName("Should handle empty query")
    void testAnalyzeQuery_EmptyQuery() {
        SmartAnalyticsResponse response = smartAnalyticsService.analyzeQuery("", 1L);

        assertNotNull(response);
        assertNotNull(response.getMainMessage());
    }

    @Test
    @DisplayName("Should return general overview for unknown query")
    void testAnalyzeQuery_UnknownQuery() {
        String query = "Random query without match";
        
        SmartAnalyticsResponse response = smartAnalyticsService.analyzeQuery(query, 1L);

        assertNotNull(response);
        assertNotNull(response.getMainMessage());
    }

    private Transaction createTransaction(Long id, String amount, Category category) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setType("expense");
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDate(LocalDate.now());
        transaction.setCategory(category);
        transaction.setNote("Test transaction " + id);
        return transaction;
    }
}

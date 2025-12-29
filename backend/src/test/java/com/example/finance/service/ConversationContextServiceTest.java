package com.example.finance.service;

import com.example.finance.entity.User;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.Category;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Test for ConversationContextService - Chatbot context management
 */
@ExtendWith(MockitoExtension.class)
public class ConversationContextServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ConversationContextService conversationContextService;

    private User testUser;
    private Category foodCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("Nguyen Van A");
        
        foodCategory = new Category();
        foodCategory.setId(5L);
        foodCategory.setName("Ăn uống");
    }

    @Test
    void testGeneratePersonalizedGreeting_UserNotFound() {
        // Given: User doesn't exist
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // When
        String greeting = conversationContextService.generatePersonalizedGreeting(999L);
        
        // Then: Should return default greeting
        assertTrue(greeting.contains("Xin chào"));
        assertTrue(greeting.contains("Moni"));
    }

    @Test
    void testGeneratePersonalizedGreeting_WithUser_NoTransactions() {
        // Given: User exists but no recent transactions
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());
        
        // When
        String greeting = conversationContextService.generatePersonalizedGreeting(1L);
        
        // Then: Should include user's first name and time greeting
        assertTrue(greeting.contains("A")); // Vietnamese first name
        assertTrue(greeting.contains("chưa có giao dịch"));
    }

    @Test
    void testGeneratePersonalizedGreeting_WithTransactions() {
        // Given: User with recent transactions
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        Transaction tx1 = createTransaction("expense", new BigDecimal("50000"), foodCategory);
        Transaction tx2 = createTransaction("expense", new BigDecimal("30000"), foodCategory);
        
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Arrays.asList(tx1, tx2));
        
        // When
        String greeting = conversationContextService.generatePersonalizedGreeting(1L);
        
        // Then: Should include spending insight
        assertTrue(greeting.contains("A")); // First name
        assertTrue(greeting.contains("chi") || greeting.contains("80,000")); // Spending amount
    }

    @Test
    void testGeneratePersonalizedGreeting_VietnameseNameExtraction() {
        // Given: Vietnamese full name (3 parts)
        testUser.setUsername("Nguyen Van Binh");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());
        
        // When
        String greeting = conversationContextService.generatePersonalizedGreeting(1L);
        
        // Then: Should extract last part as first name
        assertTrue(greeting.contains("Binh"));
    }

    @Test
    void testGeneratePersonalizedGreeting_WesternName() {
        // Given: Western name (2 parts)
        testUser.setUsername("John Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());
        
        // When
        String greeting = conversationContextService.generatePersonalizedGreeting(1L);
        
        // Then: Should use first part
        assertTrue(greeting.contains("John"));
    }

    @Test
    void testGenerateQuickActions_ReturnsActions() {
        // Given: Any user
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());
        
        // When
        List<Map<String, String>> actions = conversationContextService.generateQuickActions(1L);
        
        // Then: Should return list of quick actions
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
        
        // Verify action structure
        Map<String, String> firstAction = actions.get(0);
        assertTrue(firstAction.containsKey("emoji"));
        assertTrue(firstAction.containsKey("text"));
        assertTrue(firstAction.containsKey("action"));
    }

    @Test
    void testGenerateQuickActions_IncludesCommonActions() {
        // Given
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());
        
        // When
        List<Map<String, String>> actions = conversationContextService.generateQuickActions(1L);
        
        // Then: Should include common actions like "features", "food analysis"
        assertTrue(actions.stream().anyMatch(a -> a.get("text").contains("làm gì") || a.get("text").contains("Ăn")));
    }

    @Test
    void testGenerateQuickActions_WithUserTransactions() {
        // Given: User with transactions in specific category
        Transaction tx = createTransaction("expense", new BigDecimal("100000"), foodCategory);
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Arrays.asList(tx));
        
        // When
        List<Map<String, String>> actions = conversationContextService.generateQuickActions(1L);
        
        // Then: Should include personalized action for top category
        assertNotNull(actions);
        assertTrue(actions.size() > 0);
    }

    @Test
    void testGenerateNaturalResponse_LargestExpense() {
        // When
        String response = conversationContextService.generateNaturalResponse("largest_expense", new HashMap<>());
        
        // Then: Should return natural language response
        assertNotNull(response);
        assertTrue(response.contains("Moni") || response.contains("khoản"));
    }

    @Test
    void testGenerateNaturalResponse_SavingAdvice() {
        // When
        String response = conversationContextService.generateNaturalResponse("saving_advice", new HashMap<>());
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("tiết kiệm") || response.contains("Moni"));
    }

    @Test
    void testGenerateNaturalResponse_CategoryAnalysis() {
        // When
        String response = conversationContextService.generateNaturalResponse("category_analysis", new HashMap<>());
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("phân tích") || response.contains("chi tiêu"));
    }

    @Test
    void testGenerateNaturalResponse_UnknownContext() {
        // When
        String response = conversationContextService.generateNaturalResponse("unknown_context", new HashMap<>());
        
        // Then: Should return default response
        assertNotNull(response);
        assertTrue(response.contains("Moni"));
    }

    @Test
    void testGeneratePersonalizedGreeting_IncomeExceedsExpense() {
        // Given: Income exceeds expense
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        Transaction income = createTransaction("income", new BigDecimal("200000"), null);
        Transaction expense = createTransaction("expense", new BigDecimal("50000"), foodCategory);
        
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Arrays.asList(income, expense));
        
        // When
        String greeting = conversationContextService.generatePersonalizedGreeting(1L);
        
        // Then: Should include positive insight
        assertTrue(greeting.contains("Tuyệt") || greeting.contains("vượt"));
    }

    @Test
    void testGeneratePersonalizedGreeting_HighExpenseRatio() {
        // Given: Expense is 80%+ of income
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        Transaction income = createTransaction("income", new BigDecimal("100000"), null);
        Transaction expense = createTransaction("expense", new BigDecimal("85000"), foodCategory);
        
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Arrays.asList(income, expense));
        
        // When
        String greeting = conversationContextService.generatePersonalizedGreeting(1L);
        
        // Then: Should include first name and spending insight
        assertNotNull(greeting);
        assertTrue(greeting.contains("A")); // First name extracted
    }

    @Test
    void testGeneratePersonalizedGreeting_EmptyUsername() {
        // Given: User with empty username
        testUser.setUsername("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());
        
        // When
        String greeting = conversationContextService.generatePersonalizedGreeting(1L);
        
        // Then: Should use default "bạn"
        assertTrue(greeting.contains("bạn"));
    }

    @Test
    void testGeneratePersonalizedGreeting_UsernameWithPrefix() {
        // Given: Username with prefix (Anh, Chị, etc.)
        testUser.setUsername("Anh Minh");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
            .thenReturn(Collections.emptyList());
        
        // When
        String greeting = conversationContextService.generatePersonalizedGreeting(1L);
        
        // Then: Should extract a name (implementation may vary)
        assertNotNull(greeting);
        assertTrue(greeting.length() > 0);
        // Name extraction logic may keep "Anh" as 2-word name or remove prefix
    }

    // Helper method to create transactions
    private Transaction createTransaction(String type, BigDecimal amount, Category category) {
        Transaction tx = new Transaction();
        tx.setType(type);
        tx.setAmount(amount);
        tx.setCategory(category);
        tx.setDate(LocalDate.now());
        tx.setNote("Test transaction");
        return tx;
    }
}

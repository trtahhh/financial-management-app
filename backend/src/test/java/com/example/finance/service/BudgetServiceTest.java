package com.example.finance.service;

import com.example.finance.dto.BudgetDTO;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetMapper budgetMapper;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetCalculationService budgetCalculationService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Category testCategory;
    private Budget testBudget;
    private BudgetDTO testBudgetDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");
        testCategory.setType("EXPENSE");

        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setUser(testUser);
        testBudget.setCategory(testCategory);
        testBudget.setMonth(12);
        testBudget.setYear(2025);
        testBudget.setAmount(new BigDecimal("5000000"));
        testBudget.setSpentAmount(new BigDecimal("2000000"));
        testBudget.setIsDeleted(false);
        testBudget.setCreatedAt(LocalDateTime.now());

        testBudgetDTO = new BudgetDTO();
        testBudgetDTO.setId(1L);
        testBudgetDTO.setUserId(1L);
        testBudgetDTO.setCategoryId(1L);
        testBudgetDTO.setMonth(12);
        testBudgetDTO.setYear(2025);
        testBudgetDTO.setAmount(new BigDecimal("5000000"));
        testBudgetDTO.setSpentAmount(new BigDecimal("2000000"));
    }

    @Test
    void testGetAllBudgets_Success() {
        // Arrange
        List<Budget> budgets = Arrays.asList(testBudget);
        List<BudgetDTO> budgetDTOs = Arrays.asList(testBudgetDTO);
        
        when(budgetRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(budgets);
        when(budgetMapper.toDTOs(budgets)).thenReturn(budgetDTOs);
        when(budgetCalculationService.calculateSpentAmount(eq(1L), eq(1L), eq(12), eq(2025)))
            .thenReturn(new BigDecimal("2000000"));

        // Act
        List<BudgetDTO> result = budgetService.getAllBudgets(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        BudgetDTO budget = result.get(0);
        assertEquals(new BigDecimal("2000000"), budget.getSpentAmount());
        assertEquals(40, budget.getProgress()); // 2000000 / 5000000 * 100 = 40%
        verify(budgetRepository).findByUserIdAndIsDeletedFalse(1L);
        verify(budgetCalculationService).calculateSpentAmount(1L, 1L, 12, 2025);
    }

    @Test
    void testGetAllBudgets_ZeroSpent_UsesTotalSpent() {
        // Arrange
        List<Budget> budgets = Arrays.asList(testBudget);
        List<BudgetDTO> budgetDTOs = Arrays.asList(testBudgetDTO);
        
        when(budgetRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(budgets);
        when(budgetMapper.toDTOs(budgets)).thenReturn(budgetDTOs);
        when(budgetCalculationService.calculateSpentAmount(eq(1L), eq(1L), eq(12), eq(2025)))
            .thenReturn(BigDecimal.ZERO);
        when(budgetCalculationService.calculateTotalSpentAmount(eq(1L), eq(12), eq(2025)))
            .thenReturn(new BigDecimal("1500000"));

        // Act
        List<BudgetDTO> result = budgetService.getAllBudgets(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        BudgetDTO budget = result.get(0);
        assertEquals(new BigDecimal("1500000"), budget.getSpentAmount());
        assertEquals(30, budget.getProgress()); // 1500000 / 5000000 * 100 = 30%
        verify(budgetCalculationService).calculateTotalSpentAmount(1L, 12, 2025);
    }

    @Test
    void testGetAllBudgets_EmptyList() {
        // Arrange
        when(budgetRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(Arrays.asList());
        when(budgetMapper.toDTOs(any())).thenReturn(Arrays.asList());

        // Act
        List<BudgetDTO> result = budgetService.getAllBudgets(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(budgetRepository).findByUserIdAndIsDeletedFalse(1L);
    }

    @Test
    void testGetAllBudgets_ProgressExceeds100Percent() {
        // Arrange
        testBudget.setAmount(new BigDecimal("1000000"));
        testBudgetDTO.setAmount(new BigDecimal("1000000"));
        
        List<Budget> budgets = Arrays.asList(testBudget);
        List<BudgetDTO> budgetDTOs = Arrays.asList(testBudgetDTO);
        
        when(budgetRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(budgets);
        when(budgetMapper.toDTOs(budgets)).thenReturn(budgetDTOs);
        when(budgetCalculationService.calculateSpentAmount(eq(1L), eq(1L), eq(12), eq(2025)))
            .thenReturn(new BigDecimal("1500000")); // Spent more than budget

        // Act
        List<BudgetDTO> result = budgetService.getAllBudgets(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        BudgetDTO budget = result.get(0);
        assertEquals(150, budget.getProgress()); // Over budget: 150%
        verify(budgetCalculationService).calculateSpentAmount(1L, 1L, 12, 2025);
    }

    @Test
    void testGetAllBudgets_ZeroBudgetAmount() {
        // Arrange
        testBudget.setAmount(BigDecimal.ZERO);
        testBudgetDTO.setAmount(BigDecimal.ZERO);
        
        List<Budget> budgets = Arrays.asList(testBudget);
        List<BudgetDTO> budgetDTOs = Arrays.asList(testBudgetDTO);
        
        when(budgetRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(budgets);
        when(budgetMapper.toDTOs(budgets)).thenReturn(budgetDTOs);
        when(budgetCalculationService.calculateSpentAmount(eq(1L), eq(1L), eq(12), eq(2025)))
            .thenReturn(new BigDecimal("1000000"));

        // Act
        List<BudgetDTO> result = budgetService.getAllBudgets(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        BudgetDTO budget = result.get(0);
        assertEquals(0, budget.getProgress()); // Cannot calculate progress with zero budget
    }

    @Test
    void testClearBudgetCache_Success() {
        // Act
        budgetService.clearBudgetCache(1L);

        // Assert - method should execute without errors
        // No exceptions means success
    }

    @Test
    void testGetAllBudgets_MultipleBudgets() {
        // Arrange
        Budget budget2 = new Budget();
        budget2.setId(2L);
        budget2.setUser(testUser);
        budget2.setCategory(testCategory);
        budget2.setMonth(12);
        budget2.setYear(2025);
        budget2.setAmount(new BigDecimal("3000000"));
        budget2.setSpentAmount(new BigDecimal("1000000"));
        budget2.setIsDeleted(false);

        BudgetDTO budgetDTO2 = new BudgetDTO();
        budgetDTO2.setId(2L);
        budgetDTO2.setUserId(1L);
        budgetDTO2.setCategoryId(1L);
        budgetDTO2.setMonth(12);
        budgetDTO2.setYear(2025);
        budgetDTO2.setAmount(new BigDecimal("3000000"));
        budgetDTO2.setSpentAmount(new BigDecimal("1000000"));

        List<Budget> budgets = Arrays.asList(testBudget, budget2);
        List<BudgetDTO> budgetDTOs = Arrays.asList(testBudgetDTO, budgetDTO2);
        
        when(budgetRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(budgets);
        when(budgetMapper.toDTOs(budgets)).thenReturn(budgetDTOs);
        when(budgetCalculationService.calculateSpentAmount(eq(1L), eq(1L), eq(12), eq(2025)))
            .thenReturn(new BigDecimal("2000000"))
            .thenReturn(new BigDecimal("1000000"));

        // Act
        List<BudgetDTO> result = budgetService.getAllBudgets(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        BudgetDTO firstBudget = result.get(0);
        assertEquals(40, firstBudget.getProgress());
        
        BudgetDTO secondBudget = result.get(1);
        assertEquals(33, secondBudget.getProgress()); // 1000000 / 3000000 * 100 = 33.33
        
        verify(budgetCalculationService, times(2))
            .calculateSpentAmount(eq(1L), eq(1L), eq(12), eq(2025));
    }

    @Test
    void testGetAllBudgets_NullSpentAmount() {
        // Arrange
        testBudget.setSpentAmount(null);
        testBudgetDTO.setSpentAmount(null);
        
        List<Budget> budgets = Arrays.asList(testBudget);
        List<BudgetDTO> budgetDTOs = Arrays.asList(testBudgetDTO);
        
        when(budgetRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(budgets);
        when(budgetMapper.toDTOs(budgets)).thenReturn(budgetDTOs);
        when(budgetCalculationService.calculateSpentAmount(eq(1L), eq(1L), eq(12), eq(2025)))
            .thenReturn(new BigDecimal("500000"));

        // Act
        List<BudgetDTO> result = budgetService.getAllBudgets(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        BudgetDTO budget = result.get(0);
        assertEquals(new BigDecimal("500000"), budget.getSpentAmount());
        assertEquals(10, budget.getProgress()); // 500000 / 5000000 * 100 = 10%
    }

    @Test
    void testGetAllBudgets_CalculatesProgressCorrectly() {
        // Arrange
        testBudget.setAmount(new BigDecimal("10000000"));
        testBudgetDTO.setAmount(new BigDecimal("10000000"));
        
        List<Budget> budgets = Arrays.asList(testBudget);
        List<BudgetDTO> budgetDTOs = Arrays.asList(testBudgetDTO);
        
        when(budgetRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(budgets);
        when(budgetMapper.toDTOs(budgets)).thenReturn(budgetDTOs);
        when(budgetCalculationService.calculateSpentAmount(eq(1L), eq(1L), eq(12), eq(2025)))
            .thenReturn(new BigDecimal("2500000"));

        // Act
        List<BudgetDTO> result = budgetService.getAllBudgets(1L);

        // Assert
        assertNotNull(result);
        BudgetDTO budget = result.get(0);
        assertEquals(new BigDecimal("2500000"), budget.getSpentAmount());
        assertEquals(25, budget.getProgress()); // 2500000 / 10000000 * 100 = 25%
    }
}

package com.example.finance.service;

import com.example.finance.dto.RecurringTransactionDTO;
import com.example.finance.entity.*;
import com.example.finance.mapper.RecurringTransactionMapper;
import com.example.finance.repository.*;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Recurring Transaction Service Tests")
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository repository;

    @Mock
    private RecurringTransactionMapper mapper;

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    private User testUser;
    private Wallet testWallet;
    private Category testCategory;
    private RecurringTransaction testRecurringTransaction;
    private RecurringTransactionDTO testDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setName("Main Wallet");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Salary");

        testRecurringTransaction = new RecurringTransaction();
        testRecurringTransaction.setId(1L);
        testRecurringTransaction.setUser(testUser);
        testRecurringTransaction.setWallet(testWallet);
        testRecurringTransaction.setCategory(testCategory);
        testRecurringTransaction.setAmount(new BigDecimal("5000000"));
        testRecurringTransaction.setNote("Monthly Salary");
        testRecurringTransaction.setType("income");
        testRecurringTransaction.setFrequency("monthly");
        testRecurringTransaction.setNextExecution(LocalDate.now().plusMonths(1));
        testRecurringTransaction.setIsActive(true);

        testDTO = new RecurringTransactionDTO();
        testDTO.setId(1L);
        testDTO.setUserId(1L);
        testDTO.setWalletId(1L);
        testDTO.setCategoryId(1L);
        testDTO.setAmount(new BigDecimal("5000000"));
        testDTO.setNote("Monthly Salary");
        testDTO.setType("income");
        testDTO.setFrequency("monthly");
        testDTO.setNextExecution(LocalDate.now().plusMonths(1));
        testDTO.setIsActive(true);
    }

    @Test
    @DisplayName("Should find recurring transactions by user ID")
    void testFindByUserId() {
        // Arrange
        when(repository.findByUser_IdAndIsActiveTrue(1L))
            .thenReturn(Arrays.asList(testRecurringTransaction));
        when(mapper.toDto(testRecurringTransaction)).thenReturn(testDTO);

        // Act
        List<RecurringTransactionDTO> result = recurringTransactionService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Monthly Salary", result.get(0).getNote());
        assertEquals("income", result.get(0).getType());
        verify(repository).findByUser_IdAndIsActiveTrue(1L);
    }

    @Test
    @DisplayName("Should create recurring transaction with auto-calculated next execution")
    void testCreate_AutoCalculateNextExecution() {
        // Arrange
        RecurringTransactionDTO dtoWithoutNextExecution = new RecurringTransactionDTO();
        dtoWithoutNextExecution.setUserId(1L);
        dtoWithoutNextExecution.setWalletId(1L);
        dtoWithoutNextExecution.setCategoryId(1L);
        dtoWithoutNextExecution.setAmount(new BigDecimal("1000000"));
        dtoWithoutNextExecution.setFrequency("weekly");
        dtoWithoutNextExecution.setType("expense");
        dtoWithoutNextExecution.setNote("Weekly expense");
        dtoWithoutNextExecution.setIsActive(true);

        RecurringTransaction mappedEntity = new RecurringTransaction();
        mappedEntity.setAmount(dtoWithoutNextExecution.getAmount());
        mappedEntity.setFrequency("weekly");
        mappedEntity.setType("expense");
        mappedEntity.setNote("Weekly expense");
        mappedEntity.setIsActive(true);

        when(mapper.toEntity(dtoWithoutNextExecution)).thenReturn(mappedEntity);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(repository.save(any(RecurringTransaction.class))).thenReturn(testRecurringTransaction);
        when(mapper.toDto(any(RecurringTransaction.class))).thenReturn(testDTO);

        // Act
        RecurringTransactionDTO result = recurringTransactionService.create(dtoWithoutNextExecution);

        // Assert
        assertNotNull(result);
        verify(repository).save(argThat(rt -> rt.getNextExecution() != null));
    }

    @Test
    @DisplayName("Should create recurring transaction with manual next execution")
    void testCreate_ManualNextExecution() {
        // Arrange
        LocalDate manualDate = LocalDate.now().plusDays(15);
        testDTO.setNextExecution(manualDate);

        RecurringTransaction mappedEntity = new RecurringTransaction();
        mappedEntity.setAmount(testDTO.getAmount());
        mappedEntity.setFrequency("monthly");
        mappedEntity.setType("income");
        mappedEntity.setNote("Monthly Salary");
        mappedEntity.setIsActive(true);

        when(mapper.toEntity(testDTO)).thenReturn(mappedEntity);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(repository.save(any(RecurringTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(RecurringTransaction.class))).thenReturn(testDTO);

        // Act
        RecurringTransactionDTO result = recurringTransactionService.create(testDTO);

        // Assert
        assertNotNull(result);
        verify(repository).save(argThat(rt -> rt.getNextExecution().equals(manualDate)));
    }

    @Test
    @DisplayName("Should update recurring transaction amount")
    void testUpdate_Amount() {
        // Arrange
        RecurringTransactionDTO updateDTO = new RecurringTransactionDTO();
        updateDTO.setAmount(new BigDecimal("6000000")); // Increase salary

        when(repository.findById(1L)).thenReturn(Optional.of(testRecurringTransaction));
        when(repository.save(any(RecurringTransaction.class))).thenReturn(testRecurringTransaction);
        when(mapper.toDto(testRecurringTransaction)).thenReturn(testDTO);

        // Act
        RecurringTransactionDTO result = recurringTransactionService.update(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(repository).save(argThat(rt -> rt.getAmount().equals(new BigDecimal("6000000"))));
    }

    @Test
    @DisplayName("Should update recurring transaction frequency and recalculate next execution")
    void testUpdate_FrequencyRecalculation() {
        // Arrange
        RecurringTransactionDTO updateDTO = new RecurringTransactionDTO();
        updateDTO.setFrequency("weekly"); // Change from monthly to weekly

        when(repository.findById(1L)).thenReturn(Optional.of(testRecurringTransaction));
        when(repository.save(any(RecurringTransaction.class))).thenReturn(testRecurringTransaction);
        when(mapper.toDto(testRecurringTransaction)).thenReturn(testDTO);

        // Act
        RecurringTransactionDTO result = recurringTransactionService.update(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(repository).save(argThat(rt -> 
            rt.getFrequency().equals("weekly") && rt.getNextExecution() != null
        ));
    }

    @Test
    @DisplayName("Should deactivate recurring transaction")
    void testUpdate_Deactivate() {
        // Arrange
        RecurringTransactionDTO updateDTO = new RecurringTransactionDTO();
        updateDTO.setIsActive(false);

        when(repository.findById(1L)).thenReturn(Optional.of(testRecurringTransaction));
        when(repository.save(any(RecurringTransaction.class))).thenReturn(testRecurringTransaction);
        when(mapper.toDto(testRecurringTransaction)).thenReturn(testDTO);

        // Act
        RecurringTransactionDTO result = recurringTransactionService.update(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(repository).save(argThat(rt -> !rt.getIsActive()));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent recurring transaction")
    void testUpdate_NotFound() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            recurringTransactionService.update(999L, testDTO);
        });
    }

    @Test
    @DisplayName("Should update wallet when updating recurring transaction")
    void testUpdate_Wallet() {
        // Arrange
        Wallet newWallet = new Wallet();
        newWallet.setId(2L);
        newWallet.setName("Savings Wallet");

        RecurringTransactionDTO updateDTO = new RecurringTransactionDTO();
        updateDTO.setWalletId(2L);

        when(repository.findById(1L)).thenReturn(Optional.of(testRecurringTransaction));
        when(walletRepository.findById(2L)).thenReturn(Optional.of(newWallet));
        when(repository.save(any(RecurringTransaction.class))).thenReturn(testRecurringTransaction);
        when(mapper.toDto(testRecurringTransaction)).thenReturn(testDTO);

        // Act
        RecurringTransactionDTO result = recurringTransactionService.update(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(walletRepository).findById(2L);
        verify(repository).save(argThat(rt -> rt.getWallet().getId().equals(2L)));
    }

    @Test
    @DisplayName("Should update category when updating recurring transaction")
    void testUpdate_Category() {
        // Arrange
        Category newCategory = new Category();
        newCategory.setId(2L);
        newCategory.setName("Bonus");

        RecurringTransactionDTO updateDTO = new RecurringTransactionDTO();
        updateDTO.setCategoryId(2L);

        when(repository.findById(1L)).thenReturn(Optional.of(testRecurringTransaction));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(repository.save(any(RecurringTransaction.class))).thenReturn(testRecurringTransaction);
        when(mapper.toDto(testRecurringTransaction)).thenReturn(testDTO);

        // Act
        RecurringTransactionDTO result = recurringTransactionService.update(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(categoryRepository).findById(2L);
        verify(repository).save(argThat(rt -> rt.getCategory().getId().equals(2L)));
    }

    @Test
    @DisplayName("Should find only active recurring transactions")
    void testFindByUserId_OnlyActive() {
        // Arrange
        RecurringTransaction inactiveTransaction = new RecurringTransaction();
        inactiveTransaction.setId(2L);
        inactiveTransaction.setIsActive(false);

        when(repository.findByUser_IdAndIsActiveTrue(1L))
            .thenReturn(Arrays.asList(testRecurringTransaction)); // Only active ones
        when(mapper.toDto(testRecurringTransaction)).thenReturn(testDTO);

        // Act
        List<RecurringTransactionDTO> result = recurringTransactionService.findByUserId(1L);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    @DisplayName("Should return empty list when user has no recurring transactions")
    void testFindByUserId_Empty() {
        // Arrange
        when(repository.findByUser_IdAndIsActiveTrue(1L)).thenReturn(Arrays.asList());

        // Act
        List<RecurringTransactionDTO> result = recurringTransactionService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should handle multiple recurring transactions")
    void testFindByUserId_Multiple() {
        // Arrange
        RecurringTransaction rt2 = new RecurringTransaction();
        rt2.setId(2L);
        rt2.setNote("Utility Bill");
        rt2.setType("expense");

        RecurringTransactionDTO dto2 = new RecurringTransactionDTO();
        dto2.setId(2L);
        dto2.setNote("Utility Bill");
        dto2.setType("expense");

        when(repository.findByUser_IdAndIsActiveTrue(1L))
            .thenReturn(Arrays.asList(testRecurringTransaction, rt2));
        when(mapper.toDto(testRecurringTransaction)).thenReturn(testDTO);
        when(mapper.toDto(rt2)).thenReturn(dto2);

        // Act
        List<RecurringTransactionDTO> result = recurringTransactionService.findByUserId(1L);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Monthly Salary", result.get(0).getNote());
        assertEquals("Utility Bill", result.get(1).getNote());
    }

    @Test
    @DisplayName("Should throw exception when wallet not found during update")
    void testUpdate_WalletNotFound() {
        // Arrange
        RecurringTransactionDTO updateDTO = new RecurringTransactionDTO();
        updateDTO.setWalletId(999L);

        when(repository.findById(1L)).thenReturn(Optional.of(testRecurringTransaction));
        when(walletRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            recurringTransactionService.update(1L, updateDTO);
        });
    }

    @Test
    @DisplayName("Should throw exception when category not found during update")
    void testUpdate_CategoryNotFound() {
        // Arrange
        RecurringTransactionDTO updateDTO = new RecurringTransactionDTO();
        updateDTO.setCategoryId(999L);

        when(repository.findById(1L)).thenReturn(Optional.of(testRecurringTransaction));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            recurringTransactionService.update(1L, updateDTO);
        });
    }
}

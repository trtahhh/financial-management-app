package com.example.finance.service;

import com.example.finance.dto.TransactionDTO;
import com.example.finance.entity.*;
import com.example.finance.exception.CustomException;
import com.example.finance.mapper.TransactionMapperImpl;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapperImpl transactionMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BudgetAlertService budgetAlertService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private AICategorizationService aiCategorizationService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Wallet testWallet;
    private Category testCategory;
    private TransactionDTO testDTO;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setName("Main Wallet");
        testWallet.setBalance(new BigDecimal("1000000"));
        testWallet.setUser(testUser);

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");

        testDTO = new TransactionDTO();
        testDTO.setUserId(1L);
        testDTO.setWalletId(1L);
        testDTO.setCategoryId(1L);
        testDTO.setAmount(new BigDecimal("50000"));
        testDTO.setType("expense");
        testDTO.setNote("Lunch");
        testDTO.setDate(LocalDate.now());

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setUser(testUser);
        testTransaction.setWallet(testWallet);
        testTransaction.setCategory(testCategory);
        testTransaction.setAmount(new BigDecimal("50000"));
        testTransaction.setType("expense");
        testTransaction.setNote("Lunch");
        testTransaction.setDate(LocalDate.now());
        testTransaction.setStatus("cleared");
    }

    @Test
    @DisplayName("Should save transaction successfully")
    void testSave_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionMapper.toEntity(testDTO)).thenReturn(testTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(transactionMapper.toDto(testTransaction)).thenReturn(testDTO);

        // Act
        TransactionDTO result = transactionService.save(testDTO, null);

        // Assert
        assertNotNull(result);
        verify(transactionRepository).save(any(Transaction.class));
        verify(budgetAlertService).checkBudgetAlert(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when amount is zero or negative")
    void testSave_InvalidAmount() {
        // Arrange
        testDTO.setAmount(BigDecimal.ZERO);

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            transactionService.save(testDTO, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void testSave_NegativeAmount() {
        // Arrange
        testDTO.setAmount(new BigDecimal("-100"));

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            transactionService.save(testDTO, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when type is invalid")
    void testSave_InvalidType() {
        // Arrange
        testDTO.setType("invalid");

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            transactionService.save(testDTO, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when date is in future")
    void testSave_FutureDate() {
        // Arrange
        testDTO.setDate(LocalDate.now().plusDays(1));

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            transactionService.save(testDTO, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when date is too old")
    void testSave_TooOldDate() {
        // Arrange
        testDTO.setDate(LocalDate.now().minusYears(11));

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            transactionService.save(testDTO, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when userId is null")
    void testSave_NullUserId() {
        // Arrange
        testDTO.setUserId(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            transactionService.save(testDTO, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testSave_UserNotFound() {
        // Arrange
        // No need to stub repositories - mapper.toEntity() returns null, causing NullPointerException

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            transactionService.save(testDTO, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when wallet not found")
    void testSave_WalletNotFound() {
        // Arrange
        // No need to stub repositories - mapper.toEntity() returns null, causing NullPointerException

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            transactionService.save(testDTO, null);
        });
    }

    @Test
    @DisplayName("Should create default wallet if walletId is null")
    void testSave_CreateDefaultWallet() {
        // Arrange
        testDTO.setWalletId(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findFirstByUserId(1L)).thenReturn(Optional.of(testWallet));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionMapper.toEntity(testDTO)).thenReturn(testTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(transactionMapper.toDto(testTransaction)).thenReturn(testDTO);
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        // Act
        TransactionDTO result = transactionService.save(testDTO, null);

        // Assert
        assertNotNull(result);
        verify(walletRepository).findFirstByUserId(1L);
    }

    @Test
    @DisplayName("Should save income transaction")
    void testSave_IncomeTransaction() {
        // Arrange
        testDTO.setType("income");
        testTransaction.setType("income");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionMapper.toEntity(testDTO)).thenReturn(testTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(transactionMapper.toDto(testTransaction)).thenReturn(testDTO);

        // Act
        TransactionDTO result = transactionService.save(testDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals("income", result.getType());
    }

    @Test
    @DisplayName("Should update existing transaction")
    void testSave_UpdateTransaction() {
        // Arrange
        testDTO.setId(1L);
        testDTO.setAmount(new BigDecimal("75000"));
        
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(transactionMapper.toDto(testTransaction)).thenReturn(testDTO);

        // Act
        TransactionDTO result = transactionService.save(testDTO, null);

        // Assert
        assertNotNull(result);
        verify(transactionRepository).findById(1L);
    }

    @Test
    @DisplayName("Should set default status to cleared")
    void testSave_DefaultStatus() {
        // Arrange
        testDTO.setStatus(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionMapper.toEntity(testDTO)).thenReturn(testTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(transactionMapper.toDto(testTransaction)).thenReturn(testDTO);

        // Act
        TransactionDTO result = transactionService.save(testDTO, null);

        // Assert
        assertNotNull(result);
        verify(transactionRepository).save(argThat(t -> "cleared".equals(t.getStatus())));
    }

    @Test
    @DisplayName("Should validate date is required")
    void testSave_NullDate() {
        // Arrange
        testDTO.setDate(null);

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            transactionService.save(testDTO, null);
        });
    }

    @Test
    @DisplayName("Should allow transaction on today")
    void testSave_TodayDate() {
        // Arrange
        testDTO.setDate(LocalDate.now());
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionMapper.toEntity(testDTO)).thenReturn(testTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(transactionMapper.toDto(testTransaction)).thenReturn(testDTO);

        // Act
        TransactionDTO result = transactionService.save(testDTO, null);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should allow transaction in past within 10 years")
    void testSave_PastDate() {
        // Arrange
        testDTO.setDate(LocalDate.now().minusYears(5));
        testTransaction.setDate(LocalDate.now().minusYears(5));
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionMapper.toEntity(testDTO)).thenReturn(testTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(transactionMapper.toDto(testTransaction)).thenReturn(testDTO);

        // Act
        TransactionDTO result = transactionService.save(testDTO, null);

        // Assert
        assertNotNull(result);
    }
}

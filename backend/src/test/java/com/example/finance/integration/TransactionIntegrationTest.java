package com.example.finance.integration;

import com.example.finance.dto.TransactionDTO;
import com.example.finance.entity.*;
import com.example.finance.repository.*;
import com.example.finance.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Transaction functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private Wallet testWallet;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Clean database
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("transactiontest");
        testUser.setEmail("transaction@test.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setRole("USER");
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);

        // Create test wallet
        testWallet = new Wallet();
        testWallet.setUser(testUser);
        testWallet.setName("Test Wallet");
        testWallet.setBalance(new BigDecimal("1000000"));
        testWallet.setType("CASH");
        testWallet = walletRepository.save(testWallet);

        // Create test category
        testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setType("EXPENSE");
        testCategory.setIcon("food");
        testCategory.setColor("#FF5722");
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    void testCreateTransaction_WithDTO() {
        // Arrange
        TransactionDTO dto = new TransactionDTO();
        dto.setUserId(testUser.getId());
        dto.setWalletId(testWallet.getId());
        dto.setCategoryId(testCategory.getId());
        dto.setAmount(new BigDecimal("50000"));
        dto.setType("expense");
        dto.setDate(LocalDate.now());

        // Act
        TransactionDTO savedDto = transactionService.save(dto, null);

        // Assert
        assertNotNull(savedDto.getId());
        assertEquals("expense", savedDto.getType());
        assertEquals(new BigDecimal("50000"), savedDto.getAmount());

        // Verify wallet balance updated
        // Note: TransactionService recalculates balance from ALL transactions
        // This is the first transaction, so balance = 0 (no previous transactions) - 50000 (this expense)
        Wallet updatedWallet = walletRepository.findById(testWallet.getId()).orElseThrow();
        BigDecimal expectedBalance = new BigDecimal("-50000");
        assertEquals(expectedBalance.setScale(2), updatedWallet.getBalance().setScale(2));
    }

    @Test
    void testGetAllTransactions() {
        // Arrange - Create multiple transactions using service
        for (int i = 0; i < 3; i++) {
            TransactionDTO dto = new TransactionDTO();
            dto.setUserId(testUser.getId());
            dto.setWalletId(testWallet.getId());
            dto.setCategoryId(testCategory.getId());
            dto.setAmount(new BigDecimal("10000").multiply(new BigDecimal(i + 1)));
            dto.setType("expense");
            dto.setDate(LocalDate.now().minusDays(i));
            transactionService.save(dto, null);
        }

        // Act
        List<TransactionDTO> transactions = transactionService.findAll();

        // Assert
        assertTrue(transactions.size() >= 3);
    }

    @Test
    void testDeleteTransaction() {
        // Arrange
        TransactionDTO dto = new TransactionDTO();
        dto.setUserId(testUser.getId());
        dto.setWalletId(testWallet.getId());
        dto.setCategoryId(testCategory.getId());
        dto.setAmount(new BigDecimal("100000"));
        dto.setType("expense");
        dto.setDate(LocalDate.now());
        TransactionDTO savedDto = transactionService.save(dto, null);

        BigDecimal balanceAfterExpense = walletRepository.findById(testWallet.getId())
                .orElseThrow().getBalance();

        Long transactionId = savedDto.getId();

        // Act
        transactionService.deleteById(transactionId);

        // Assert
        assertFalse(transactionRepository.existsById(transactionId));
        
        // Verify transaction was deleted
        Wallet restoredWallet = walletRepository.findById(testWallet.getId()).orElseThrow();
        // Balance should be restored
        BigDecimal expectedBalance = balanceAfterExpense.add(new BigDecimal("100000"));
        assertEquals(0, expectedBalance.compareTo(restoredWallet.getBalance()));
    }

    @Test
    void testGetTransactionById() {
        // Arrange
        TransactionDTO dto = new TransactionDTO();
        dto.setUserId(testUser.getId());
        dto.setWalletId(testWallet.getId());
        dto.setCategoryId(testCategory.getId());
        dto.setAmount(new BigDecimal("75000"));
        dto.setType("expense");
        dto.setDate(LocalDate.now());
        TransactionDTO savedDto = transactionService.save(dto, null);

        // Act
        TransactionDTO foundDto = transactionService.findById(savedDto.getId());

        // Assert
        assertNotNull(foundDto);
        assertEquals(savedDto.getId(), foundDto.getId());
        assertEquals(new BigDecimal("75000"), foundDto.getAmount());
    }
}

package com.example.finance.service;

import com.example.finance.dto.WalletDTO;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import com.example.finance.mapper.WalletMapper;
import com.example.finance.repository.NotificationRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    private User testUser;
    private Wallet testWallet;
    private WalletDTO testWalletDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setUser(testUser);
        testWallet.setName("Test Wallet");
        testWallet.setType("CASH");
        testWallet.setBalance(new BigDecimal("1000000"));
        testWallet.setInitialBalance(new BigDecimal("500000"));
        testWallet.setIsActive(true);

        testWalletDTO = new WalletDTO();
        testWalletDTO.setId(1L);
        testWalletDTO.setUserId(1L);
        testWalletDTO.setName("Test Wallet");
        testWalletDTO.setType("CASH");
        testWalletDTO.setBalance(new BigDecimal("1000000"));
    }

    @Test
    void testFindAll_Success() {
        // Arrange
        List<Wallet> wallets = Arrays.asList(testWallet);
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(walletMapper.toDto(testWallet)).thenReturn(testWalletDTO);

        // Act
        List<WalletDTO> result = walletService.findAll(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Wallet", result.get(0).getName());
        verify(walletRepository).findByUserId(1L);
    }

    @Test
    void testSave_Success() {
        // Arrange
        when(walletMapper.toEntity(testWalletDTO)).thenReturn(testWallet);
        when(walletRepository.save(testWallet)).thenReturn(testWallet);
        when(walletMapper.toDto(testWallet)).thenReturn(testWalletDTO);

        // Act
        WalletDTO result = walletService.save(testWalletDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Test Wallet", result.getName());
        assertEquals(new BigDecimal("1000000"), result.getBalance());
        verify(walletRepository).save(testWallet);
    }

    @Test
    void testSave_WithoutUserId_ThrowsException() {
        // Arrange
        testWalletDTO.setUserId(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            walletService.save(testWalletDTO);
        });
    }

    @Test
    void testFindById_Success() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(transactionRepository.sumByWalletIdAndType(1L, "income")).thenReturn(new BigDecimal("800000"));
        when(transactionRepository.sumByWalletIdAndType(1L, "expense")).thenReturn(new BigDecimal("300000"));
        when(walletMapper.toDto(testWallet)).thenReturn(testWalletDTO);

        // Act
        WalletDTO result = walletService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Wallet", result.getName());
        verify(walletRepository).findById(1L);
        verify(transactionRepository).sumByWalletIdAndType(1L, "income");
        verify(transactionRepository).sumByWalletIdAndType(1L, "expense");
    }

    @Test
    void testFindById_NotFound_ThrowsException() {
        // Arrange
        when(walletRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            walletService.findById(999L);
        });
        assertTrue(exception.getMessage().contains("Wallet not found"));
    }

    @Test
    void testExistsById_True() {
        // Arrange
        when(walletRepository.existsById(1L)).thenReturn(true);

        // Act
        boolean result = walletService.existsById(1L);

        // Assert
        assertTrue(result);
        verify(walletRepository).existsById(1L);
    }

    @Test
    void testExistsById_False() {
        // Arrange
        when(walletRepository.existsById(999L)).thenReturn(false);

        // Act
        boolean result = walletService.existsById(999L);

        // Assert
        assertFalse(result);
    }

    @Test
    void testUpdateWalletBalance_Success() {
        // Arrange
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(transactionRepository.sumByWalletIdAndType(1L, "income")).thenReturn(new BigDecimal("800000"));
        when(transactionRepository.sumByWalletIdAndType(1L, "expense")).thenReturn(new BigDecimal("300000"));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        walletService.updateWalletBalance(1L);

        // Assert
        verify(walletRepository).findById(1L);
        verify(transactionRepository).sumByWalletIdAndType(1L, "income");
        verify(transactionRepository).sumByWalletIdAndType(1L, "expense");
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testUpdateWalletBalance_CalculatesCorrectly() {
        // Arrange
        testWallet.setInitialBalance(new BigDecimal("500000"));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(transactionRepository.sumByWalletIdAndType(1L, "income")).thenReturn(new BigDecimal("800000"));
        when(transactionRepository.sumByWalletIdAndType(1L, "expense")).thenReturn(new BigDecimal("300000"));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet savedWallet = invocation.getArgument(0);
            // Expected: 500000 (initial) + 800000 (income) - 300000 (expense) = 1000000
            assertEquals(new BigDecimal("1000000"), savedWallet.getBalance());
            return savedWallet;
        });

        // Act
        walletService.updateWalletBalance(1L);

        // Assert
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testGetTotalBalance_Success() {
        // Arrange
        Wallet wallet1 = new Wallet();
        wallet1.setId(1L);
        wallet1.setBalance(new BigDecimal("1000000"));

        Wallet wallet2 = new Wallet();
        wallet2.setId(2L);
        wallet2.setBalance(new BigDecimal("500000"));

        List<Wallet> wallets = Arrays.asList(wallet1, wallet2);
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);

        // Act
        BigDecimal result = walletService.getTotalBalance(1L);

        // Assert
        assertEquals(new BigDecimal("1500000"), result);
        verify(walletRepository).findByUserId(1L);
    }

    @Test
    void testGetTotalBalance_EmptyWallets() {
        // Arrange
        when(walletRepository.findByUserId(1L)).thenReturn(Arrays.asList());

        // Act
        BigDecimal result = walletService.getTotalBalance(1L);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void testCountByUserId_Success() {
        // Arrange
        when(walletRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn(3L);

        // Act
        Long result = walletService.countByUserId(1L);

        // Assert
        assertEquals(3L, result);
        verify(walletRepository).countByUserIdAndIsActiveTrue(1L);
    }

    @Test
    void testFindByUserId_Success() {
        // Arrange
        List<Wallet> wallets = Arrays.asList(testWallet);
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(transactionRepository.sumByWalletIdAndType(1L, "income")).thenReturn(new BigDecimal("800000"));
        when(transactionRepository.sumByWalletIdAndType(1L, "expense")).thenReturn(new BigDecimal("300000"));
        when(walletMapper.toDto(any(Wallet.class))).thenReturn(testWalletDTO);

        // Act
        List<WalletDTO> result = walletService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(walletRepository).findByUserId(1L);
    }

    @Test
    void testUpdate_Success() {
        // Arrange
        when(walletRepository.existsById(1L)).thenReturn(true);
        when(walletMapper.toEntity(testWalletDTO)).thenReturn(testWallet);
        when(walletRepository.save(testWallet)).thenReturn(testWallet);
        when(walletMapper.toDto(testWallet)).thenReturn(testWalletDTO);

        // Act
        WalletDTO result = walletService.update(testWalletDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Test Wallet", result.getName());
        verify(walletRepository).existsById(1L);
        verify(walletRepository).save(testWallet);
    }

    @Test
    void testUpdate_NotFound_ThrowsException() {
        // Arrange
        when(walletRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            walletService.update(testWalletDTO);
        });
        assertTrue(exception.getMessage().contains("Wallet not found"));
    }
}

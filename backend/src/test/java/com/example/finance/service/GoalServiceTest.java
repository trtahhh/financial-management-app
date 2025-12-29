package com.example.finance.service;

import com.example.finance.dto.GoalDTO;
import com.example.finance.entity.Category;
import com.example.finance.entity.Goal;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import com.example.finance.mapper.GoalMapper;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.GoalRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalMapper goalMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private UserService userService;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private GoalService goalService;

    private User testUser;
    private Goal testGoal;
    private GoalDTO testGoalDTO;
    private Wallet testWallet;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Savings");
        testCategory.setType("INCOME");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setUser(testUser);
        testWallet.setName("Main Wallet");
        testWallet.setBalance(new BigDecimal("5000000"));

        testGoal = new Goal();
        testGoal.setId(1L);
        testGoal.setUser(testUser);
        testGoal.setName("Buy a Car");
        testGoal.setTargetAmount(new BigDecimal("200000000"));
        testGoal.setCurrentAmount(new BigDecimal("50000000"));
        testGoal.setDueDate(LocalDate.now().plusMonths(12));
        testGoal.setStatus("IN_PROGRESS");
        testGoal.setIsDeleted(false);
        testGoal.setIsExecuted(false);
        testGoal.setCategories(new HashSet<>());

        testGoalDTO = new GoalDTO();
        testGoalDTO.setId(1L);
        testGoalDTO.setUserId(1L);
        testGoalDTO.setName("Buy a Car");
        testGoalDTO.setTargetAmount(new BigDecimal("200000000"));
        testGoalDTO.setCurrentAmount(new BigDecimal("50000000"));
        testGoalDTO.setCurrentBalance(new BigDecimal("5000000"));
        testGoalDTO.setDueDate(LocalDate.now().plusMonths(12));
        testGoalDTO.setStatus("IN_PROGRESS");
    }

    @Test
    void testFindAll_Success() {
        // Arrange
        List<Goal> goals = Arrays.asList(testGoal);
        when(goalRepository.findAll()).thenReturn(goals);
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);

        // Act
        List<GoalDTO> result = goalService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Buy a Car", result.get(0).getName());
        verify(goalRepository).findAll();
    }

    @Test
    void testFindByUserId_Success() {
        // Arrange
        List<Goal> goals = Arrays.asList(testGoal);
        List<Wallet> wallets = Arrays.asList(testWallet);
        
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(goalRepository.findByUserId(1L)).thenReturn(goals);
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);

        // Act
        List<GoalDTO> result = goalService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        GoalDTO goal = result.get(0);
        assertEquals("Buy a Car", goal.getName());
        assertEquals(new BigDecimal("5000000"), goal.getCurrentBalance());
        assertEquals(2.5, goal.getProgress()); // 5000000 / 200000000 * 100 = 2.5%
        verify(walletRepository).findByUserId(1L);
        verify(goalRepository).findByUserId(1L);
    }

    @Test
    void testFindByUserId_MultipleWallets() {
        // Arrange
        Wallet wallet2 = new Wallet();
        wallet2.setId(2L);
        wallet2.setBalance(new BigDecimal("3000000"));
        
        List<Goal> goals = Arrays.asList(testGoal);
        List<Wallet> wallets = Arrays.asList(testWallet, wallet2);
        
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(goalRepository.findByUserId(1L)).thenReturn(goals);
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);

        // Act
        List<GoalDTO> result = goalService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        GoalDTO goal = result.get(0);
        // Total balance: 5000000 + 3000000 = 8000000
        assertEquals(new BigDecimal("8000000"), goal.getCurrentBalance());
        assertEquals(4.0, goal.getProgress()); // 8000000 / 200000000 * 100 = 4%
    }

    @Test
    void testFindByUserId_ProgressCappedAt100() {
        // Arrange
        testGoal.setTargetAmount(new BigDecimal("1000000"));
        testGoalDTO.setTargetAmount(new BigDecimal("1000000"));
        
        testWallet.setBalance(new BigDecimal("5000000")); // Balance exceeds target
        
        List<Goal> goals = Arrays.asList(testGoal);
        List<Wallet> wallets = Arrays.asList(testWallet);
        
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(goalRepository.findByUserId(1L)).thenReturn(goals);
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);

        // Act
        List<GoalDTO> result = goalService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        GoalDTO goal = result.get(0);
        assertEquals(100.0, goal.getProgress()); // Capped at 100%
    }

    @Test
    void testFindByUserId_ZeroTargetAmount() {
        // Arrange
        testGoal.setTargetAmount(BigDecimal.ZERO);
        testGoalDTO.setTargetAmount(BigDecimal.ZERO);
        
        List<Goal> goals = Arrays.asList(testGoal);
        List<Wallet> wallets = Arrays.asList(testWallet);
        
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(goalRepository.findByUserId(1L)).thenReturn(goals);
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);

        // Act
        List<GoalDTO> result = goalService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        GoalDTO goal = result.get(0);
        assertEquals(0.0, goal.getProgress()); // Cannot calculate with zero target
    }

    @Test
    void testFindByUserId_EmptyWallets() {
        // Arrange
        List<Goal> goals = Arrays.asList(testGoal);
        
        when(walletRepository.findByUserId(1L)).thenReturn(Arrays.asList());
        when(goalRepository.findByUserId(1L)).thenReturn(goals);
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);

        // Act
        List<GoalDTO> result = goalService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        GoalDTO goal = result.get(0);
        assertEquals(BigDecimal.ZERO, goal.getCurrentBalance());
        assertEquals(0.0, goal.getProgress());
    }

    @Test
    void testSave_Success() {
        // Arrange
        when(goalMapper.toEntity(testGoalDTO)).thenReturn(testGoal);
        when(goalRepository.save(testGoal)).thenReturn(testGoal);
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);

        // Act
        GoalDTO result = goalService.save(testGoalDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Buy a Car", result.getName());
        assertEquals(new BigDecimal("200000000"), result.getTargetAmount());
        verify(goalRepository).save(testGoal);
    }

    @Test
    void testFindById_Success() {
        // Arrange
        when(goalRepository.findById(1L)).thenReturn(Optional.of(testGoal));
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);

        // Act
        GoalDTO result = goalService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Buy a Car", result.getName());
        verify(goalRepository).findById(1L);
    }

    @Test
    void testFindById_NotFound() {
        // Arrange
        when(goalRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            goalService.findById(999L);
        });
        verify(goalRepository).findById(999L);
    }

    @Test
    void testPredictNextMonth_ReturnsZero() {
        // Act
        BigDecimal result = goalService.predictNextMonth(1L);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void testFindByUserId_NullWalletBalance() {
        // Arrange
        testWallet.setBalance(null);
        
        List<Goal> goals = Arrays.asList(testGoal);
        List<Wallet> wallets = Arrays.asList(testWallet);
        
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(goalRepository.findByUserId(1L)).thenReturn(goals);
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);

        // Act
        List<GoalDTO> result = goalService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        GoalDTO goal = result.get(0);
        assertEquals(BigDecimal.ZERO, goal.getCurrentBalance());
    }

    @Test
    void testFindByUserId_MultipleGoals() {
        // Arrange
        Goal goal2 = new Goal();
        goal2.setId(2L);
        goal2.setUser(testUser);
        goal2.setName("Emergency Fund");
        goal2.setTargetAmount(new BigDecimal("50000000"));
        goal2.setCurrentAmount(new BigDecimal("10000000"));
        goal2.setDueDate(LocalDate.now().plusMonths(6));
        goal2.setStatus("IN_PROGRESS");
        goal2.setIsDeleted(false);

        GoalDTO goalDTO2 = new GoalDTO();
        goalDTO2.setId(2L);
        goalDTO2.setUserId(1L);
        goalDTO2.setName("Emergency Fund");
        goalDTO2.setTargetAmount(new BigDecimal("50000000"));
        goalDTO2.setCurrentAmount(new BigDecimal("10000000"));
        goalDTO2.setDueDate(LocalDate.now().plusMonths(6));
        goalDTO2.setStatus("IN_PROGRESS");

        List<Goal> goals = Arrays.asList(testGoal, goal2);
        List<Wallet> wallets = Arrays.asList(testWallet);
        
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(goalRepository.findByUserId(1L)).thenReturn(goals);
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);
        when(goalMapper.toDto(goal2)).thenReturn(goalDTO2);

        // Act
        List<GoalDTO> result = goalService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Both goals should have same total balance
        assertEquals(new BigDecimal("5000000"), result.get(0).getCurrentBalance());
        assertEquals(new BigDecimal("5000000"), result.get(1).getCurrentBalance());
        
        // But different progress percentages
        assertEquals(2.5, result.get(0).getProgress()); // 5000000 / 200000000
        assertEquals(10.0, result.get(1).getProgress()); // 5000000 / 50000000
    }

    @Test
    void testSave_WithCategories() {
        // Arrange
        testGoal.getCategories().add(testCategory);
        
        when(goalMapper.toEntity(testGoalDTO)).thenReturn(testGoal);
        when(goalRepository.save(testGoal)).thenReturn(testGoal);
        when(goalMapper.toDto(testGoal)).thenReturn(testGoalDTO);

        // Act
        GoalDTO result = goalService.save(testGoalDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Buy a Car", result.getName());
        verify(goalRepository).save(testGoal);
    }

    @Test
    void testFindByUserId_EmptyGoals() {
        // Arrange
        List<Wallet> wallets = Arrays.asList(testWallet);
        
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(goalRepository.findByUserId(1L)).thenReturn(Arrays.asList());

        // Act
        List<GoalDTO> result = goalService.findByUserId(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(walletRepository).findByUserId(1L);
        verify(goalRepository).findByUserId(1L);
    }
}

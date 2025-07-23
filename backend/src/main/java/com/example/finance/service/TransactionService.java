package com.example.finance.service;

import com.example.finance.dto.TransactionDto;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import com.example.finance.entity.Category;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.WalletRepository;
import com.example.finance.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private BudgetService budgetService;
    
    // Create new transaction
    public TransactionDto createTransaction(TransactionDto transactionDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Wallet wallet = walletRepository.findById(transactionDto.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        Category category = categoryRepository.findById(transactionDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setWallet(wallet);
        transaction.setTransType(transactionDto.getTransType());
        transaction.setCategory(category);
        transaction.setAmount(transactionDto.getAmount());
        transaction.setDescription(transactionDto.getDescription());
        transaction.setTransactionDate(transactionDto.getTransactionDate() != null ? 
                transactionDto.getTransactionDate() : LocalDate.now());
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        return TransactionDto.fromEntity(savedTransaction);
    }
    
    // Get transaction by ID
    public TransactionDto getTransactionById(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        return TransactionDto.fromEntity(transaction);
    }
    
    // Get all transactions for user with pagination
    public Page<TransactionDto> getUserTransactions(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Page<Transaction> transactions = transactionRepository.findByUserAndIsDeletedFalseOrderByTransactionDateDesc(user, pageable);
        return transactions.map(TransactionDto::fromEntity);
    }
    
    // Get recent transactions
    public List<TransactionDto> getRecentTransactions(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Transaction> transactions = transactionRepository.findTop10ByUserAndIsDeletedFalseOrderByTransactionDateDesc(user);
        return transactions.stream()
                .limit(limit)
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get transactions by type
    public List<TransactionDto> getTransactionsByType(String username, String transType) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Transaction> transactions = transactionRepository.findByUserAndTransTypeAndIsDeletedFalseOrderByTransactionDateDesc(user, transType);
        
        return transactions.stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get transactions by category
    public List<TransactionDto> getTransactionsByCategory(String username, Long categoryId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        List<Transaction> transactions = transactionRepository.findByUserAndCategoryAndIsDeletedFalseOrderByTransactionDateDesc(user, category);
        
        return transactions.stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get transactions by wallet
    public List<TransactionDto> getTransactionsByWallet(String username, Long walletId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        List<Transaction> transactions = transactionRepository.findByUserAndWalletAndIsDeletedFalseOrderByTransactionDateDesc(user, wallet);
        
        return transactions.stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Search transactions
    public List<TransactionDto> searchTransactions(String username, String searchTerm) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Transaction> transactions = transactionRepository.findByUserAndDescriptionContainingIgnoreCase(user, searchTerm);
        
        return transactions.stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get transactions by date range
    public List<TransactionDto> getTransactionsByDateRange(String username, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(user, startDate, endDate);
        
        return transactions.stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Update transaction
    public TransactionDto updateTransaction(Long id, TransactionDto transactionDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        Wallet wallet = walletRepository.findById(transactionDto.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        Category category = categoryRepository.findById(transactionDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Update transaction
        transaction.setWallet(wallet);
        transaction.setTransType(transactionDto.getTransType());
        transaction.setCategory(category);
        transaction.setAmount(transactionDto.getAmount());
        transaction.setDescription(transactionDto.getDescription());
        transaction.setTransactionDate(transactionDto.getTransactionDate());
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        return TransactionDto.fromEntity(savedTransaction);
    }
    
    // Delete transaction (soft delete)
    public void deleteTransaction(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        transaction.setIsDeleted(true);
        transactionRepository.save(transaction);
    }
    
    // Get financial summary
    public Map<String, Object> getFinancialSummary(String username, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        BigDecimal totalIncome = transactionRepository.getTotalIncomeByUserAndDateRange(user, startDate, endDate);
        BigDecimal totalExpense = transactionRepository.getTotalExpenseByUserAndDateRange(user, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);
        
        List<Object[]> categoryTotals = transactionRepository.getTotalAmountByCategoryForUserAndDateRange(user, startDate, endDate);
        
        Map<String, Object> summary = Map.of(
            "totalIncome", totalIncome,
            "totalExpense", totalExpense,
            "balance", balance,
            "categoryTotals", categoryTotals
        );
        
        return summary;
    }
    
    // Get current month transactions
    public List<TransactionDto> getCurrentMonthTransactions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate now = LocalDate.now();
        List<Transaction> transactions = transactionRepository.findByUserAndCurrentMonth(user, now);
        
        return transactions.stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get current year transactions
    public List<TransactionDto> getCurrentYearTransactions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate now = LocalDate.now();
        List<Transaction> transactions = transactionRepository.findByUserAndCurrentYear(user, now);
        
        return transactions.stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    // Get transaction statistics
    public Map<String, Object> getTransactionStatistics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate now = LocalDate.now();
        
        Long incomeCount = transactionRepository.countByUserAndTransType(user, "income");
        Long expenseCount = transactionRepository.countByUserAndTransType(user, "expense");
        
        List<Object[]> typeTotals = transactionRepository.getTotalAmountByTypeForUserAndDateRange(user, 
                now.minusMonths(1), now);
        
        Map<String, Object> statistics = Map.of(
            "incomeCount", incomeCount,
            "expenseCount", expenseCount,
            "typeTotals", typeTotals
        );
        
        return statistics;
    }
} 
package com.example.finance.service;

import com.example.finance.dto.TransactionDTO;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import com.example.finance.exception.CustomException;
import com.example.finance.mapper.TransactionMapperImpl;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.WalletRepository;
import com.example.finance.dto.WalletStatDTO;
import com.example.finance.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "transactions")
public class TransactionService {

    private static final Path UPLOAD_DIR = Path.of("uploads");
    private static final String FILE_UPLOAD_ERROR = "Error uploading file for transaction";
    private static final String TRANSACTION_NOT_FOUND = "Transaction not found with id: ";
    
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private BudgetRepository budgetRepository;


    private final TransactionRepository      repo;
    private final TransactionMapperImpl      mapper;
    private final UserRepository             userRepo;
    private final WalletRepository           walletRepo;
    private final CategoryRepository         categoryRepo;

    @Transactional
    public TransactionDTO save(TransactionDTO dto, MultipartFile file) {
        try {
            // Business logic validation
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CustomException("Amount must be greater than 0");
            }
            
            if (dto.getType() == null || (!dto.getType().equals("income") && !dto.getType().equals("expense"))) {
                throw new CustomException("Type must be 'income' or 'expense'");
            }
            
            if (dto.getDate() == null) {
                throw new CustomException("Transaction date is required");
            }
            
            // Validate date range (not in future, not too old)
            LocalDate today = LocalDate.now();
            if (dto.getDate().isAfter(today)) {
                throw new CustomException("Transaction date cannot be in the future");
            }
            
            if (dto.getDate().isBefore(today.minusYears(10))) {
                throw new CustomException("Transaction date cannot be more than 10 years ago");
            }
            
            // File upload handling
            if (!Files.exists(UPLOAD_DIR)) {
                Files.createDirectories(UPLOAD_DIR);
            }
            if (file != null && !file.isEmpty()) {
                String filename = generateUniqueFileName(file);
                Files.copy(file.getInputStream(), UPLOAD_DIR.resolve(filename));
                dto.setFilePath(filename);
            }

            Transaction entity = mapper.toEntity(dto);

            if (entity.getStatus() == null) {
                entity.setStatus("cleared");      
            }

            if (dto.getUserId() == null) {
                throw new CustomException("UserId is required!");
            }
            User user = userRepo.findById(dto.getUserId())
                                .orElseThrow(() -> new CustomException("User not found!"));
            entity.setUser(user);

            if (dto.getWalletId() != null) {
                Wallet wallet = walletRepo.findById(dto.getWalletId())
                                          .orElseThrow(() -> new CustomException("Wallet not found!"));
                entity.setWallet(wallet);
            } else {
                // Auto-assign first wallet for user or create one
                Wallet defaultWallet = walletRepo.findFirstByUserId(dto.getUserId())
                    .orElse(null);
                if (defaultWallet == null) {
                    // Create default wallet if doesn't exist
                    defaultWallet = new Wallet();
                    defaultWallet.setUser(user);
                    defaultWallet.setName("Ví mặc định");
                    defaultWallet.setType("default");
                    defaultWallet.setBalance(BigDecimal.ZERO);
                    defaultWallet = walletRepo.save(defaultWallet);
                }
                entity.setWallet(defaultWallet);
            }

            if (dto.getCategoryId() != null) {
                Category category = categoryRepo.findById(dto.getCategoryId())
                                                .orElseThrow(() -> new CustomException("Category not found!"));
                entity.setCategory(category);
            }

            Transaction saved = repo.save(entity);

            // Cập nhật số dư ví sau khi tạo giao dịch
            if (saved.getWallet() != null) {
                updateWalletBalance(saved.getWallet().getId());
            }

            checkOverBudget(saved);

            if (saved.getWallet() != null && saved.getWallet().getBalance() != null) {
                if (saved.getWallet().getBalance().compareTo(new BigDecimal("50000")) < 0) {
                    notificationService.createLowBalanceNotification(
                        saved.getUser().getId(),
                        saved.getWallet().getId(),
                        saved.getWallet().getBalance()
                    );
                }
            }

            return mapper.toDto(saved);

        } catch (IOException ex) {
            log.error(FILE_UPLOAD_ERROR, ex);
            throw new CustomException(FILE_UPLOAD_ERROR);
        }
    }

    private String generateUniqueFileName(MultipartFile file) {
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        return UUID.randomUUID() + ext;
    }

    @Cacheable
    public TransactionDTO findById(Long id) {
        return repo.findById(id)
                   .map(mapper::toDto)
                   .orElseThrow(() -> new CustomException(TRANSACTION_NOT_FOUND + id));
    }

    public void deleteById(Long id) {
        if (!repo.existsById(id)) {
            throw new CustomException(TRANSACTION_NOT_FOUND + id);
        }
        
        // Lấy thông tin ví trước khi xóa giao dịch để cập nhật số dư
        Transaction transaction = repo.findById(id)
                .orElseThrow(() -> new CustomException(TRANSACTION_NOT_FOUND + id));
        Long walletId = transaction.getWallet() != null ? transaction.getWallet().getId() : null;
        
        repo.deleteById(id);
        
        // Cập nhật số dư ví sau khi xóa giao dịch
        if (walletId != null) {
            updateWalletBalance(walletId);
        }
    }

    /**
     * Tính toán và cập nhật số dư ví dựa trên các giao dịch
     */
    @Transactional
    public void updateWalletBalance(Long walletId) {
        Wallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new CustomException("Wallet not found with id: " + walletId));
        
        // Tính tổng thu nhập (income) và chi tiêu (expense) cho ví này
        BigDecimal totalIncome = repo.sumByWalletIdAndType(walletId, "income");
        BigDecimal totalExpense = repo.sumByWalletIdAndType(walletId, "expense");
        
        // Xử lý trường hợp null
        totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
        
        // Số dư = Thu nhập - Chi tiêu
        BigDecimal newBalance = totalIncome.subtract(totalExpense);
        wallet.setBalance(newBalance);
        
        walletRepo.save(wallet);
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> findAll() {
        return repo.findAllWithDetails()
                   .stream()
                   .map(mapper::toDto)
                   .toList();
    }

    public BigDecimal sumByMonth(YearMonth ym) {
        var list = repo.findAllByDateBetween(ym.atDay(1), ym.atEndOfMonth());
        return list.stream()
                   .map(Transaction::getAmount)
                   .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumByCategoryAndMonth(Long categoryId, int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        var list = repo.findAllByDateBetween(ym.atDay(1), ym.atEndOfMonth());
        return list.stream()
                   .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
                   .filter(t -> "expense".equals(t.getType())) // Only count expenses
                   .map(Transaction::getAmount)
                   .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void checkOverBudget(Transaction transaction) {
        if (transaction == null || transaction.getUser() == null || transaction.getCategory() == null) return;
        Long userId = transaction.getUser().getId();
        Long categoryId = transaction.getCategory().getId();
        int month = transaction.getDate().getMonthValue();
        int year = transaction.getDate().getYear();

        var budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year).stream()
                .filter(b -> b.getCategory().getId().equals(categoryId))
                .toList();
        if (budgets.isEmpty()) return; 

        BigDecimal totalSpending = repo.sumByUserCategoryMonth(userId, categoryId, month, year);
        for (var budget : budgets) {
            if (totalSpending.compareTo(budget.getAmount()) > 0) {
                if (!notificationService.existsOverBudget(userId, categoryId, month, year)) {
                    notificationService.createOverBudgetNotification(userId, categoryId, budget.getId(), totalSpending, budget.getAmount(), month, year);
                }
            }
        }
    }


    public List<Map<String, Object>> sumAmountByCategory(Long userId, Integer month, Integer year) {
        List<Object[]> result = repo.sumAmountByCategory(userId, month, year);
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Object[] row : result) {
            Map<String, Object> map = new HashMap<>();
            map.put("categoryId", row[0]);
            map.put("totalAmount", row[1]);
            list.add(map);
        }
        return list;
    }

    public List<WalletStatDTO> getSumAmountByWallet(Long userId, Long walletId, String type, Integer month, Integer year) {
        List<Object[]> raw = repo.sumAmountByWallet(userId, walletId, type, month, year);
        return raw.stream()
                .map(arr -> new WalletStatDTO(
                    arr[0] == null ? null : ((Number) arr[0]).longValue(),
                    arr[1] == null ? BigDecimal.ZERO : (BigDecimal) arr[1],
                    null 
                ))
                .toList();
    }

    public List<WalletStatDTO> getCountTransactionsByWallet(Long userId, Long walletId, String type, Integer month, Integer year) {
        List<Object[]> raw = repo.countTransactionsByWallet(userId, walletId, type, month, year);
        return raw.stream()
                .map(arr -> new WalletStatDTO(
                    arr[0] == null ? null : ((Number) arr[0]).longValue(),
                    null, 
                    arr[1] == null ? 0L : ((Number) arr[1]).longValue()
                ))
                .toList();
    }

    // Thêm method này vào TransactionService (có thể thêm gần cuối class):

    /**
     * Tính tổng chi tiêu theo category trong tháng
     */
    public BigDecimal getTotalSpentByCategory(Long userId, Long categoryId, int month, int year) {
        return repo.sumByUserCategoryMonth(userId, categoryId, month, year);
    }

    /**
     * Lấy chi tiêu theo category cho dashboard
     */
    public List<Map<String, Object>> getExpensesByCategory(Long userId, Integer month, Integer year) {
        List<Object[]> results = repo.findExpensesByCategory(userId, month, year);
        
        return results.stream().map(result -> {
            Map<String, Object> map = new HashMap<>();
            map.put("categoryName", result[0]);
            map.put("categoryColor", result[1]);
            map.put("totalAmount", result[2]);
            map.put("transactionCount", result[3]);
            return map;
        }).toList();
    }

    /**
     * Lấy giao dịch gần đây
     */
    public List<Map<String, Object>> getRecentTransactions(Long userId, int limit) {
        List<Transaction> transactions = repo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(limit)
                .toList();
                
        return transactions.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("amount", t.getAmount());
            map.put("type", t.getType());
            map.put("note", t.getNote());
            map.put("date", t.getDate());
            map.put("categoryName", t.getCategory() != null ? t.getCategory().getName() : "");
            map.put("walletName", t.getWallet() != null ? t.getWallet().getName() : "");
            return map;
        }).toList();
    }

    /**
     * Tính tổng theo type và date range
     */
    public BigDecimal getTotalByTypeAndDateRange(Long userId, String type, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = repo.sumByUserTypeAndDateRange(userId, type, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Lấy xu hướng chi tiêu theo tháng
     */
    public List<Map<String, Object>> getMonthlySpendingTrend(Long userId, int months) {
        List<Map<String, Object>> trend = new ArrayList<>();
        
        for (int i = months - 1; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.now().minusMonths(i);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            
            BigDecimal monthlyIncome = getTotalByTypeAndDateRange(userId, "income", startDate, endDate);
            BigDecimal monthlyExpense = getTotalByTypeAndDateRange(userId, "expense", startDate, endDate);
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", yearMonth.getMonth().name());
            monthData.put("year", yearMonth.getYear());
            monthData.put("income", monthlyIncome);
            monthData.put("expense", monthlyExpense);
            monthData.put("net", monthlyIncome.subtract(monthlyExpense));
            
            trend.add(monthData);
        }
        
        return trend;
    }

    /**
     * Đếm số giao dịch của user
     */
    public Long countByUserId(Long userId) {
        return repo.countByUserIdAndIsDeletedFalse(userId);
    }

    /**
     * Lấy chi tiêu theo danh mục trong khoảng ngày
     */
    public List<Map<String, Object>> getExpensesByCategoryByDate(Long userId, LocalDate startDate, LocalDate endDate) {
        // Nếu repo có method tương ứng, ví dụ: repo.findExpensesByCategoryByDate(userId, startDate, endDate)
        List<Object[]> results = repo.findExpensesByCategoryByDate(userId, startDate, endDate);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("categoryName", row[0]);
            map.put("categoryColor", row[1]);
            map.put("totalAmount", row[2]);
            map.put("transactionCount", row[3]);
            list.add(map);
        }
        return list;
    }

    /**
     * Lấy giao dịch gần đây trong khoảng ngày
     */
    public List<Map<String, Object>> getRecentTransactionsByDate(Long userId, LocalDate startDate, LocalDate endDate, int limit) {
        List<Transaction> transactions = repo.findByUserIdAndDateBetweenOrderByCreatedAtDesc(userId, startDate, endDate)
                .stream()
                .limit(limit)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Transaction t : transactions) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("amount", t.getAmount());
            map.put("type", t.getType());
            map.put("note", t.getNote());
            map.put("date", t.getDate());
            map.put("categoryName", t.getCategory() != null ? t.getCategory().getName() : "");
            map.put("walletName", t.getWallet() != null ? t.getWallet().getName() : "");
            result.add(map);
        }
        return result;
    }

    public BigDecimal getTotalSpentByCategoryAndDateRange(Long userId, Long categoryId, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = repo.sumByUserCategoryAndDateRange(userId, categoryId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
}

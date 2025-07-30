package com.example.finance.service;

import com.example.finance.dto.TransactionDTO;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import com.example.finance.exception.CustomException;
import com.example.finance.mapper.TransactionMapper;
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
import java.time.YearMonth;
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
    private final TransactionMapper          mapper;
    private final UserRepository             userRepo;
    private final WalletRepository           walletRepo;
    private final CategoryRepository         categoryRepo;

    @Transactional
    public TransactionDTO save(TransactionDTO dto, MultipartFile file) {
        try {
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
            }

            if (dto.getCategoryId() != null) {
                Category category = categoryRepo.findById(dto.getCategoryId())
                                                .orElseThrow(() -> new CustomException("Category not found!"));
                entity.setCategory(category);
            }

            Transaction saved = repo.save(entity);

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
        repo.deleteById(id);
    }

    public List<TransactionDTO> findAll() {
        return repo.findAll()
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

    private void checkOverBudget(Transaction transaction) {
        if (transaction == null || transaction.getUser() == null || transaction.getCategory() == null) return;
        Long userId = transaction.getUser().getId();
        Long categoryId = transaction.getCategory().getId();
        int month = transaction.getDate().getMonthValue();
        int year = transaction.getDate().getYear();

        var budgets = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(userId, categoryId, month, year);
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
                    arr[1] == null ? 0 : ((Number) arr[1]).doubleValue(),
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
                    arr[1] == null ? 0 : ((Number) arr[1]).longValue()
                ))
                .toList();
    }
}

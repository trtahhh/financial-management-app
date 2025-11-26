package com.example.finance.service;

import com.example.finance.dto.RecurringTransactionDTO;
import com.example.finance.dto.TransactionDTO;
import com.example.finance.entity.RecurringTransaction;
import com.example.finance.mapper.RecurringTransactionMapper;
import com.example.finance.repository.RecurringTransactionRepository;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.WalletRepository;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import com.example.finance.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionService {

 private final RecurringTransactionRepository repository;
 private final RecurringTransactionMapper mapper;
 private final TransactionService transactionService;
 private final UserRepository userRepository;
 private final WalletRepository walletRepository;
 private final CategoryRepository categoryRepository;

 public List<RecurringTransactionDTO> findByUserId(Long userId) {
 return repository.findByUser_IdAndIsActiveTrue(userId)
 .stream()
 .map(mapper::toDto)
 .toList();
 }

    @Transactional
    public RecurringTransactionDTO create(RecurringTransactionDTO dto) {
        RecurringTransaction entity = mapDtoToEntity(dto);
        
        // Allow manual nextExecution for testing, otherwise auto-calculate
        if (dto.getNextExecution() != null) {
            entity.setNextExecution(dto.getNextExecution());
        } else {
            LocalDate today = LocalDate.now();
            entity.setNextExecution(calculateNextExecution(today, dto.getFrequency()));
        }
        
        RecurringTransaction saved = repository.save(entity);
        log.info("✅ Created recurring transaction for user {} - Amount: {} - Next: {}", 
                dto.getUserId(), dto.getAmount(), entity.getNextExecution());
        
        return mapper.toDto(saved);
    }    @Transactional
    public RecurringTransactionDTO update(Long id, RecurringTransactionDTO dto) {
        RecurringTransaction existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
        
        // Update basic fields
        if (dto.getAmount() != null) {
            existing.setAmount(dto.getAmount());
        }
        if (dto.getNote() != null) {
            existing.setNote(dto.getNote());
        }
        if (dto.getType() != null) {
            existing.setType(dto.getType());
        }
        if (dto.getIsActive() != null) {
            existing.setIsActive(dto.getIsActive());
        }
        
        // If frequency changed, recalculate next execution
        if (dto.getFrequency() != null && !dto.getFrequency().equals(existing.getFrequency())) {
            existing.setFrequency(dto.getFrequency());
            existing.setNextExecution(calculateNextExecution(LocalDate.now(), dto.getFrequency()));
        }
        
        // Update relationships if changed
        if (dto.getWalletId() != null && !dto.getWalletId().equals(existing.getWallet().getId())) {
            Wallet wallet = walletRepository.findById(dto.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            existing.setWallet(wallet);
        }
        
        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(existing.getCategory().getId())) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            existing.setCategory(category);
        }
        
        RecurringTransaction saved = repository.save(existing);
        log.info("📝 Updated recurring transaction {} - Next: {}", id, saved.getNextExecution());
        return mapper.toDto(saved);
    } @Transactional
 public void delete(Long id) {
 RecurringTransaction existing = repository.findById(id)
 .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
 existing.setIsActive(false);
 repository.save(existing);
 
 log.info("🗑 Deactivated recurring transaction {}", id);
 }

 @Transactional
 public RecurringTransactionDTO toggleActive(Long id) {
 RecurringTransaction existing = repository.findById(id)
 .orElseThrow(() -> new RuntimeException("Recurring transaction not found with id: " + id));
 
 // Toggle active status
 existing.setIsActive(!existing.getIsActive());
 
 // Nếu đang kích hoạt lại và đã quá hạn next_execution, tính lại
 if (existing.getIsActive() && existing.getNextExecution().isBefore(LocalDate.now())) {
 existing.setNextExecution(calculateNextExecution(LocalDate.now(), existing.getFrequency()));
 }
 
 RecurringTransaction saved = repository.save(existing);
 log.info(" Toggled recurring transaction {} - Active: {}", id, saved.getIsActive());
 
 return mapper.toDto(saved);
 }

 /**
 * MANUAL execution - User triggers this to create transactions from recurring
 * NOT automatic - user must click "Execute Now" button
 */
    @Transactional
    public int executeRecurringTransaction(Long recurringId) {
        log.info("🔄 Manual execution of recurring transaction ID: {}", recurringId);
        
        RecurringTransaction recurring = repository.findById(recurringId)
                .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
        
        if (!recurring.getIsActive()) {
            throw new RuntimeException("Recurring transaction is not active");
        }
        
        // Manual execution - allow anytime, no date restriction
        // User clicks "Execute Now" button to create transaction manually
        
        // Create transaction from recurring template
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setUserId(recurring.getUser().getId());
        transactionDTO.setWalletId(recurring.getWallet().getId());
        transactionDTO.setCategoryId(recurring.getCategory().getId());
        transactionDTO.setAmount(recurring.getAmount());
        transactionDTO.setType(recurring.getType().toLowerCase()); // Ensure lowercase for validation
        transactionDTO.setNote("🔄 " + (recurring.getNote() != null ? recurring.getNote() : "Recurring transaction"));
        transactionDTO.setDate(LocalDate.now());
        transactionDTO.setStatus("cleared");
        
        transactionService.save(transactionDTO, null);
        
        // Update next execution date based on frequency
        recurring.setNextExecution(calculateNextExecution(recurring.getNextExecution(), recurring.getFrequency()));
        repository.save(recurring);
        
        log.info("✅ Created transaction from recurring ID: {} - Next execution: {}", 
                recurringId, recurring.getNextExecution());
        return 1;
    } /**
 * Execute ALL due recurring transactions for a specific user
 * User clicks "Execute All My Recurring Transactions" button
 */
 @Transactional
 public int executeAllDueRecurringTransactions(Long userId) {
 log.info("🔄 Executing all due recurring transactions for user: {}", userId);
 
 List<RecurringTransaction> dueTransactions = repository.findDueTransactions(LocalDate.now());
 int created = 0;
 
 for (RecurringTransaction recurring : dueTransactions) {
 // Only process transactions for this user
 if (!recurring.getUser().getId().equals(userId)) {
 continue;
 }
 
 if (!recurring.getIsActive()) {
 continue;
 }
 
 try {
 // Create transaction
                TransactionDTO transactionDTO = new TransactionDTO();
                transactionDTO.setUserId(recurring.getUser().getId());
                transactionDTO.setWalletId(recurring.getWallet().getId());
                transactionDTO.setCategoryId(recurring.getCategory().getId());
                transactionDTO.setAmount(recurring.getAmount());
                transactionDTO.setType(recurring.getType().toLowerCase()); // Ensure lowercase for validation
                transactionDTO.setNote("🔄 [Auto] " + recurring.getNote());
                transactionDTO.setDate(LocalDate.now());
                transactionDTO.setStatus("cleared");                transactionService.save(transactionDTO, null);
                
                // Update next execution date
                recurring.setNextExecution(calculateNextExecution(recurring.getNextExecution(), recurring.getFrequency()));
                repository.save(recurring);
                created++; log.info("✅ Created transaction from recurring ID: {}", recurring.getId());
 
 } catch (Exception e) {
 log.error("❌ Failed to process recurring transaction {}: {}", recurring.getId(), e.getMessage());
 }
 }
 
 log.info("🏁 Executed {} recurring transactions for user {}", created, userId);
 return created;
 }

 /**
 * Tính toán next execution date dựa trên frequency
 */
 private LocalDate calculateNextExecution(LocalDate currentDate, String frequency) {
 return switch (frequency.toLowerCase()) {
 case "daily" -> currentDate.plusDays(1);
 case "weekly" -> currentDate.plusWeeks(1);
 case "monthly" -> currentDate.plusMonths(1);
 case "yearly" -> currentDate.plusYears(1);
 default -> currentDate.plusMonths(1); // Default to monthly
 };
 }

 /**
 * Helper method để map DTO to Entity với relationships
 */
 private RecurringTransaction mapDtoToEntity(RecurringTransactionDTO dto) {
 RecurringTransaction entity = mapper.toEntity(dto);
 
 // Set User
 if (dto.getUserId() != null) {
 User user = userRepository.findById(dto.getUserId())
 .orElseThrow(() -> new RuntimeException("User not found"));
 entity.setUser(user);
 }
 
 // Set Wallet
 if (dto.getWalletId() != null) {
 Wallet wallet = walletRepository.findById(dto.getWalletId())
 .orElseThrow(() -> new RuntimeException("Wallet not found"));
 entity.setWallet(wallet);
 }
 
 // Set Category
 if (dto.getCategoryId() != null) {
 Category category = categoryRepository.findById(dto.getCategoryId())
 .orElseThrow(() -> new RuntimeException("Category not found"));
 entity.setCategory(category);
 }
 
 return entity;
 }
}

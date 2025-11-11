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
 return repository.findByUserIdAndIsActiveTrue(userId)
 .stream()
 .map(mapper::toDto)
 .toList();
 }

 @Transactional
 public RecurringTransactionDTO create(RecurringTransactionDTO dto) {
 RecurringTransaction entity = mapDtoToEntity(dto);
 entity.setNextExecution(calculateNextExecution(dto.getStartDate(), dto.getFrequency()));
 
 RecurringTransaction saved = repository.save(entity);
 log.info(" Created recurring transaction for user {} - Amount: {}", 
 dto.getUserId(), dto.getAmount());
 
 return mapper.toDto(saved);
 }

 @Transactional
 public RecurringTransactionDTO update(Long id, RecurringTransactionDTO dto) {
 RecurringTransaction existing = repository.findById(id)
 .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
 
 // Update fields
 existing.setAmount(dto.getAmount());
 existing.setNote(dto.getNote());
 existing.setFrequency(dto.getFrequency());
 existing.setEndDate(dto.getEndDate());
 existing.setIsActive(dto.getIsActive());
 
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
 return mapper.toDto(saved);
 }

 @Transactional
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
 * Scheduled job để tự động tạo transactions từ recurring transactions
 * Chạy mỗi ngày lúc 00:30
 */
 @Scheduled(cron = "0 30 0 * * ?")
 @Transactional
 public void processRecurringTransactions() {
 log.info(" Processing recurring transactions...");
 
 List<RecurringTransaction> dueTransactions = repository.findDueTransactions(LocalDate.now());
 
 for (RecurringTransaction recurring : dueTransactions) {
 try {
 // Tạo TransactionDTO để sử dụng với method save có sẵn
 TransactionDTO transactionDTO = new TransactionDTO();
 transactionDTO.setUserId(recurring.getUser().getId());
 transactionDTO.setWalletId(recurring.getWallet().getId());
 transactionDTO.setCategoryId(recurring.getCategory().getId());
 transactionDTO.setAmount(recurring.getAmount());
 transactionDTO.setType(recurring.getType());
 transactionDTO.setNote("[Auto] " + recurring.getNote());
 transactionDTO.setDate(LocalDate.now());
 transactionDTO.setStatus("cleared");
 
 // Sử dụng method save có sẵn với file = null
 transactionService.save(transactionDTO, null);
 
 // Cập nhật next execution
 recurring.setNextExecution(calculateNextExecution(recurring.getNextExecution(), recurring.getFrequency()));
 
 // Kiểm tra end date
 if (recurring.getEndDate() != null && recurring.getNextExecution().isAfter(recurring.getEndDate())) {
 recurring.setIsActive(false);
 log.info("🏁 Recurring transaction {} reached end date, deactivated", recurring.getId());
 }
 
 repository.save(recurring);
 
 log.info(" Created recurring transaction for user {} - Amount: {}", 
 recurring.getUser().getId(), recurring.getAmount());
 
 } catch (Exception e) {
 log.error(" Failed to process recurring transaction {}: {}", recurring.getId(), e.getMessage());
 }
 }
 
 log.info("🏁 Processed {} recurring transactions", dueTransactions.size());
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

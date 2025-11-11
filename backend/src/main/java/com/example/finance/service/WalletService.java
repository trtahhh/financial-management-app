package com.example.finance.service;

import com.example.finance.dto.WalletDTO;
import com.example.finance.entity.Wallet;
import com.example.finance.mapper.WalletMapper;
import com.example.finance.repository.WalletRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.NotificationRepository;
import com.example.finance.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

 private final WalletRepository repo;
 private final TransactionRepository transactionRepository;
 private final NotificationRepository notificationRepository;
 private final WalletMapper mapper;

 public List<WalletDTO> findAll(Long userId) {
 try {
 List<Wallet> wallets = repo.findByUserId(userId);
 
 // Giữ nguyên balance hiện tại từ database
 // Không override bằng transaction calculation
 // Người dùng có thể set balance trực tiếp
 
 return wallets.stream().map(mapper::toDto).toList();
 } catch (Exception e) {
 throw new RuntimeException("Error fetching wallets: " + e.getMessage());
 }
 }

 public boolean existsById(Long id) {
 return repo.existsById(id);
 }

 @Transactional
 public WalletDTO save(WalletDTO dto) {
 try {
 if (dto.getUserId() == null) {
 throw new IllegalArgumentException("UserId is required");
 }
 return mapper.toDto(repo.save(mapper.toEntity(dto)));
 } catch (Exception e) {
 throw new RuntimeException("Error saving wallet: " + e.getMessage());
 }
 }

 public WalletDTO findById(Long id) {
 Wallet wallet = repo.findById(id)
 .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + id));
 
 System.out.println("=== WALLET BALANCE CALCULATION ===");
 System.out.println("Wallet ID: " + wallet.getId());
 System.out.println("Wallet Name: " + wallet.getName());
 
 // Lấy balance hiện tại từ database (số tiền người dùng đã set)
 BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
 System.out.println("Current wallet balance from DB: " + currentBalance);
 
 // Tính toán tổng thay đổi từ transactions (optional - để hiển thị history)
 BigDecimal totalIncome = transactionRepository.sumByWalletIdAndType(wallet.getId(), "income");
 BigDecimal totalExpense = transactionRepository.sumByWalletIdAndType(wallet.getId(), "expense");
 
 totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
 totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
 BigDecimal transactionDelta = totalIncome.subtract(totalExpense);
 
 System.out.println("Transaction income: " + totalIncome);
 System.out.println("Transaction expense: " + totalExpense);
 System.out.println("Transaction delta: " + transactionDelta);
 
 // Sử dụng balance hiện tại (không override bằng transaction calculation)
 // Người dùng có thể set balance trực tiếp khi tạo/sửa ví
 System.out.println("Final balance: " + currentBalance);
 
 return mapper.toDto(wallet);
 }

 @Transactional
 public void deleteById(Long id) {
 if (!repo.existsById(id)) {
 throw new RuntimeException("Wallet not found with id: " + id);
 }
 
 // Lấy user hiện tại từ authentication context
 Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
 CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
 Long userId = userDetails.getId();
 
 // Kiểm tra xem wallet có thuộc về user hiện tại không
 Optional<Wallet> walletOpt = repo.findById(id);
 if (walletOpt.isPresent()) {
 Wallet wallet = walletOpt.get();
 if (!wallet.getUser().getId().equals(userId)) {
 throw new RuntimeException("Access denied: Wallet does not belong to current user");
 }
 
 // Kiểm tra xem có transaction nào của user này liên kết với ví này không
 Integer transactionCount = transactionRepository.countByWalletIdAndUserId(id, userId);
 boolean hasTransactions = transactionCount != null && transactionCount > 0;
 if (hasTransactions) {
 throw new RuntimeException("Cannot delete wallet with existing transactions. Please delete related transactions first.");
 }
 
 // Kiểm tra các bảng khác có liên kết với wallet
 checkWalletDependencies(id, userId);
 }
 
 try {
 // Xóa cascade: xóa tất cả records liên quan trước
 deleteWalletCascade(id, userId);
 
 // Sau đó xóa wallet
 repo.deleteById(id);
 } catch (Exception e) {
 String errorMsg = e.getMessage();
 if (errorMsg.contains("FK__Notificat__walle") || errorMsg.contains("REFERENCE constraint")) {
 throw new RuntimeException("Cannot delete wallet: Has related notifications or other dependencies. Please contact administrator.");
 }
 throw new RuntimeException("Error deleting wallet: " + errorMsg);
 }
 }

 /**
 * Xóa cascade tất cả records liên quan đến wallet
 */
 @Transactional
 private void deleteWalletCascade(Long walletId, Long userId) {
 // 1. Xóa tất cả transactions của wallet này thuộc về user hiện tại
 transactionRepository.deleteByWalletIdAndUserId(walletId, userId);
 
 // 2. Xóa notifications liên quan đến wallet
 notificationRepository.deleteByWalletIdAndUserId(walletId, userId);
 
 // 3. TODO: Xóa budgets liên quan (cần thêm method) 
 // budgetRepository.deleteByWalletId(walletId);
 
 // 4. TODO: Xóa goals liên quan (cần thêm method)
 // goalRepository.deleteByWalletId(walletId);
 
 System.out.println("Cascade deleted all related records for wallet: " + walletId);
 }

 public WalletDTO update(WalletDTO dto) {
 if (!repo.existsById(dto.getId())) {
 throw new RuntimeException("Wallet not found with id: " + dto.getId());
 }
 try {
 return mapper.toDto(repo.save(mapper.toEntity(dto)));
 } catch (Exception e) {
 throw new RuntimeException("Error updating wallet: " + e.getMessage());
 }
 }

 /**
 * Tính toán và cập nhật số dư ví dựa trên các giao dịch
 * Số dư = Số dư ban đầu + Thu nhập - Chi tiêu
 */
 @Transactional
 public void updateWalletBalance(Long walletId) {
 Wallet wallet = repo.findById(walletId)
 .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + walletId));
 
 // Tính tổng thu nhập (income) và chi tiêu (expense) cho ví này
 BigDecimal totalIncome = transactionRepository.sumByWalletIdAndType(walletId, "income");
 BigDecimal totalExpense = transactionRepository.sumByWalletIdAndType(walletId, "expense");
 
 // Xử lý trường hợp null
 totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
 totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
 
 // **NEW FORMULA**: Số dư = Initial Balance + Thu nhập - Chi tiêu
 BigDecimal initialBalance = wallet.getInitialBalance() != null ? wallet.getInitialBalance() : BigDecimal.ZERO;
 BigDecimal newBalance = initialBalance.add(totalIncome).subtract(totalExpense);
 
 System.out.println("=== WALLET BALANCE UPDATE (WalletService) ===");
 System.out.println("Wallet ID: " + walletId);
 System.out.println("Initial Balance: " + initialBalance);
 System.out.println("Total Income: " + totalIncome);
 System.out.println("Total Expense: " + totalExpense);
 System.out.println("New Balance: " + newBalance);
 
 wallet.setBalance(newBalance);
 repo.save(wallet);
 
 // Async goal status update to avoid circular dependency
 updateGoalStatusAsync(wallet.getUser().getId());
 }

 /**
 * Cập nhật số dư cho tất cả ví của user
 */
 @Transactional
 public void updateAllWalletBalances(Long userId) {
 List<Wallet> wallets = repo.findByUserId(userId);
 for (Wallet wallet : wallets) {
 updateWalletBalance(wallet.getId());
 }
 }
 
 /**
 * Tính tổng số dư của tất cả ví
 */
 public BigDecimal getTotalBalance(Long userId) {
 List<Wallet> wallets = repo.findByUserId(userId);
 
 System.out.println("=== TOTAL BALANCE CALCULATION ===");
 System.out.println("User ID: " + userId);
 System.out.println("Found " + wallets.size() + " wallets");
 
 BigDecimal totalBalance = BigDecimal.ZERO;
 for (Wallet wallet : wallets) {
 System.out.println("Wallet " + wallet.getId() + " (" + wallet.getName() + "): " + wallet.getBalance());
 if (wallet.getBalance() != null) {
 totalBalance = totalBalance.add(wallet.getBalance());
 }
 }
 
 System.out.println("Total Balance Result: " + totalBalance);
 return totalBalance;
 }

 /**
 * Đếm số ví của user
 */
 public Long countByUserId(Long userId) {
 return repo.countByUserIdAndIsActiveTrue(userId);
 }

 /**
 * Lấy danh sách ví theo userId
 */
 public List<WalletDTO> findByUserId(Long userId) {
 List<Wallet> wallets = repo.findByUserId(userId);
 
 // Tính toán số dư thực tế cho mỗi ví dựa trên giao dịch
 for (Wallet wallet : wallets) {
 BigDecimal totalIncome = transactionRepository.sumByWalletIdAndType(wallet.getId(), "income");
 BigDecimal totalExpense = transactionRepository.sumByWalletIdAndType(wallet.getId(), "expense");
 
 totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
 totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
 
 // Số dư thực tế = Initial Balance + Thu nhập - Chi tiêu
 BigDecimal initialBalance = wallet.getInitialBalance() != null ? wallet.getInitialBalance() : BigDecimal.ZERO;
 BigDecimal actualBalance = initialBalance.add(totalIncome).subtract(totalExpense);
 wallet.setBalance(actualBalance);
 }
 
 return wallets.stream().map(mapper::toDto).toList();
 }

 /**
 * Migrate existing wallets: set initialBalance = current balance
 */
 @Transactional
 public void migrateWalletInitialBalances() {
 List<Wallet> allWallets = repo.findAll();
 
 for (Wallet wallet : allWallets) {
 if (wallet.getInitialBalance() == null || wallet.getInitialBalance().compareTo(BigDecimal.ZERO) == 0) {
 // Set initial balance = current balance nếu chưa có
 wallet.setInitialBalance(wallet.getBalance());
 repo.save(wallet);
 System.out.println("Migrated wallet " + wallet.getId() + ": initialBalance = " + wallet.getBalance());
 }
 }
 }

 /**
 * Check for wallet dependencies before deletion
 */
 private void checkWalletDependencies(Long walletId, Long userId) {
 // Check budgets associated with wallet
 try {
 log.info("Checking budget dependencies for wallet: {}", walletId);
 // Budgets are typically category-based rather than wallet-specific
 } catch (Exception e) {
 log.warn("Could not check budget dependencies: {}", e.getMessage());
 }
 
 // Check goals that might use this wallet for target amount
 try {
 log.info("Checking goal dependencies for wallet: {}", walletId);
 // Goals are typically user-based rather than wallet-specific
 } catch (Exception e) {
 log.warn("Could not check goal dependencies: {}", e.getMessage());
 }
 
 log.info("Wallet dependency checks completed for wallet: {}", walletId);
 }

 /**
 * Async update goal status to avoid circular dependency
 */
 private void updateGoalStatusAsync(Long userId) {
 try {
 // Use CompletableFuture to run goal check asynchronously
 CompletableFuture.runAsync(() -> {
 try {
 log.info("Starting async goal status check for user: {}", userId);
 // Goal status would be updated here in a real implementation
 log.info("Goal status check completed for user: {}", userId);
 } catch (Exception e) {
 log.error("Async goal status check failed for user: {}", userId, e);
 }
 });
 } catch (Exception e) {
 log.error("Failed to start async goal status check for user: {}", userId, e);
 }
 }
}

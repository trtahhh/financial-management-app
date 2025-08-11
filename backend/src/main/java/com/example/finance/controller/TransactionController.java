package com.example.finance.controller;

import com.example.finance.dto.TransactionDTO;
import com.example.finance.dto.CategoryDTO;
import com.example.finance.dto.WalletDTO;
import com.example.finance.dto.WalletStatDTO;
import com.example.finance.service.TransactionService;
import com.example.finance.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.finance.service.WalletService;
import com.example.finance.service.CategoryService;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService service;
    private final UserService userService;
    private final WalletService walletService;
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> list() {
        try {
            // Get current user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Authentication required"));
            }
            
            String username = authentication.getName();
            Long currentUserId = userService.findByUsername(username).getId();
            
            // Get transactions for current user only (limit to 1000 for performance)
            List<Map<String, Object>> transactionMaps = service.getRecentTransactions(currentUserId, 1000);
            List<TransactionDTO> transactions = transactionMaps.stream()
                .map(map -> {
                    TransactionDTO dto = new TransactionDTO();
                    dto.setId(((Number) map.get("id")).longValue());
                    dto.setAmount(new BigDecimal(map.get("amount").toString()));
                    dto.setType((String) map.get("type"));
                    dto.setDate(LocalDate.parse(map.get("date").toString()));
                    dto.setNote((String) map.get("note"));
                    // IDs for frontend usage (edit, etc.)
                    if (map.get("categoryId") != null) {
                        dto.setCategoryId(((Number) map.get("categoryId")).longValue());
                    }
                    if (map.get("walletId") != null) {
                        dto.setWalletId(((Number) map.get("walletId")).longValue());
                    }

                    // Map category name if available so frontend can display it
                    Object categoryName = map.get("categoryName");
                    if (categoryName != null) {
                        CategoryDTO categoryDTO = new CategoryDTO();
                        categoryDTO.setName(categoryName.toString());
                        dto.setCategory(categoryDTO);
                    }

                    // Map wallet name if available (may be used by frontend later)
                    Object walletName = map.get("walletName");
                    if (walletName != null) {
                        WalletDTO walletDTO = new WalletDTO();
                        walletDTO.setName(walletName.toString());
                        dto.setWallet(walletDTO);
                    }
                    return dto;
                })
                .toList();
            log.info("Retrieved {} transactions for user {}", transactions.size(), currentUserId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error getting transactions", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi lấy danh sách giao dịch: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TransactionDTO dto) {
        try {
            // Enhanced input validation
            if (dto == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Transaction data is required"));
            }
            
            // Validate amount
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Amount must be greater than 0"));
            }
            
            // Validate amount range (prevent unrealistic values)
            if (dto.getAmount().compareTo(new BigDecimal("999999999")) > 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Amount is too large"));
            }
            
            // Validate type
            if (dto.getType() == null || (!dto.getType().equals("income") && !dto.getType().equals("expense"))) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Type must be 'income' or 'expense'"));
            }
            
            // Validate date
            if (dto.getDate() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Transaction date is required"));
            }
            
            // Validate date range (not in future, not too old)
            LocalDate today = LocalDate.now();
            if (dto.getDate().isAfter(today)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Transaction date cannot be in the future"));
            }
            
            if (dto.getDate().isBefore(today.minusYears(10))) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Transaction date cannot be more than 10 years ago"));
            }
            
            // Validate note length
            if (dto.getNote() != null && dto.getNote().length() > 500) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Note is too long (max 500 characters)"));
            }
            
            // Get current user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Authentication required"));
            }
            
            // Get user ID from username in security context
            String username = authentication.getName();
            Long currentUserId = userService.findByUsername(username).getId();
            dto.setUserId(currentUserId);
            
            // Validate wallet and category if provided
            if (dto.getWalletId() != null) {
                if (!walletService.existsById(dto.getWalletId())) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid wallet ID"));
                }
            }
            
            if (dto.getCategoryId() != null) {
                if (!categoryService.existsById(dto.getCategoryId())) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid category ID"));
                }
            }
            
            TransactionDTO saved = service.save(dto, null);
            log.info("Created transaction for user {}: amount={}, type={}", 
                    currentUserId, saved.getAmount(), saved.getType());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Transaction created successfully",
                "data", saved
            ));
            
        } catch (Exception e) {
            log.error("Error creating transaction", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Error creating transaction: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id) {
        try {
            // Get current user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Authentication required"));
            }
            
            String username = authentication.getName();
            Long currentUserId = userService.findByUsername(username).getId();
            
            TransactionDTO dto = service.findById(id);
            if (dto == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Không tìm thấy giao dịch"));
            }
            
            // Check if user owns this transaction
            if (!dto.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "You can only view your own transactions"));
            }
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error getting transaction by id: {}", id, e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi lấy thông tin giao dịch: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") Long id,
            @RequestBody TransactionDTO dto
    ) {
        try {
            // Get current user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Authentication required"));
            }
            
            String username = authentication.getName();
            Long currentUserId = userService.findByUsername(username).getId();
            
            // Check if transaction exists and belongs to current user
            TransactionDTO existing = service.findById(id);
            if (existing == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Không tìm thấy giao dịch"));
            }
            
            if (!existing.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "You can only update your own transactions"));
            }
            
            dto.setId(id);
            dto.setUserId(currentUserId); // Ensure user can't change ownership
            TransactionDTO updated = service.save(dto, null);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật giao dịch thành công",
                "data", updated
            ));
        } catch (Exception e) {
            log.error("Error updating transaction", e);
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "message", "Lỗi cập nhật giao dịch: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "transactions", key = "#id")
    public ResponseEntity<?> delete(
            @PathVariable("id") Long id
    ) {
        try {
            // Get current user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Authentication required"));
            }
            
            String username = authentication.getName();
            Long currentUserId = userService.findByUsername(username).getId();
            
            // Check if transaction exists and belongs to current user
            TransactionDTO existing = service.findById(id);
            if (existing == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Không tìm thấy giao dịch"));
            }
            
            if (!existing.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "You can only delete your own transactions"));
            }
            
            service.deleteById(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xóa giao dịch thành công"
            ));
        } catch (Exception e) {
            log.error("Error deleting transaction: {}", id, e);
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "message", "Lỗi xóa giao dịch: " + e.getMessage()));
        }
    }

    @GetMapping("/stats-by-category")
    public ResponseEntity<List<Map<String, Object>>> statsByCategory(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        List<Map<String, Object>> result = service.sumAmountByCategory(userId, month, year);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/wallet")
    public ResponseEntity<List<WalletStatDTO>> getSumAmountByWallet(
            @RequestParam Long userId,
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(service.getSumAmountByWallet(userId, walletId, type, month, year));
    }

    @GetMapping("/stats/wallet/count")
    public ResponseEntity<List<WalletStatDTO>> getCountTransactionsByWallet(
            @RequestParam Long userId,
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(service.getCountTransactionsByWallet(userId, walletId, type, month, year));
    }
}

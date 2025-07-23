package com.example.finance.controller;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    @Autowired 
    TransactionRepository transactionRepo;
    
    @Autowired
    UserRepository userRepo;
    
    @Autowired
    CategoryRepository categoryRepo;
    
    @Autowired
    WalletRepository walletRepo;

    // Lấy danh sách giao dịch của user hiện tại
    @GetMapping
    public ResponseEntity<?> getTransactions(
            @RequestParam(required = false) String transType,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String search) {
        
        String email = getCurrentEmail();
        System.out.println("TransactionController - Current email: " + email);
        
        if (email == null) {
            System.out.println("TransactionController - No email found, returning 401");
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        System.out.println("TransactionController - User found: " + (user != null ? user.getEmail() : "null"));
        
        if (user == null) {
            System.out.println("TransactionController - User not found, returning 404");
            return ResponseEntity.notFound().build();
        }
        
        try {
            // Sử dụng method không có Pageable
            List<Transaction> transactions = transactionRepo.findByUserAndIsDeletedFalseOrderByTransactionDateDesc(user);
            System.out.println("TransactionController - Found " + transactions.size() + " transactions for user: " + user.getEmail());
            
            // Lọc theo loại giao dịch
            if (transType != null && !transType.isEmpty()) {
                transactions = transactions.stream()
                    .filter(t -> t.getTransType().equals(transType))
                    .toList();
                System.out.println("TransactionController - Filtered by type '" + transType + "': " + transactions.size() + " transactions");
            }
            
            // Lọc theo danh mục
            if (categoryId != null) {
                transactions = transactions.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
                    .toList();
                System.out.println("TransactionController - Filtered by category ID " + categoryId + ": " + transactions.size() + " transactions");
            }
            
            // Lọc theo ngày
            if (fromDate != null && !fromDate.isEmpty()) {
                LocalDate from = LocalDate.parse(fromDate);
                transactions = transactions.stream()
                    .filter(t -> t.getTransactionDate().isAfter(from.minusDays(1)))
                    .toList();
                System.out.println("TransactionController - Filtered by from date '" + fromDate + "': " + transactions.size() + " transactions");
            }
            
            if (toDate != null && !toDate.isEmpty()) {
                LocalDate to = LocalDate.parse(toDate);
                transactions = transactions.stream()
                    .filter(t -> t.getTransactionDate().isBefore(to.plusDays(1)))
                    .toList();
                System.out.println("TransactionController - Filtered by to date '" + toDate + "': " + transactions.size() + " transactions");
            }
            
            // Tìm kiếm theo mô tả
            if (search != null && !search.isEmpty()) {
                String searchLower = search.toLowerCase();
                transactions = transactions.stream()
                    .filter(t -> t.getDescription() != null && 
                               t.getDescription().toLowerCase().contains(searchLower))
                    .toList();
                System.out.println("TransactionController - Filtered by search '" + search + "': " + transactions.size() + " transactions");
            }
            
            System.out.println("TransactionController - Returning " + transactions.size() + " transactions");
            return ResponseEntity.ok(transactions);
            
        } catch (Exception e) {
            System.out.println("TransactionController - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi tải giao dịch: " + e.getMessage()));
        }
    }

    // Lấy giao dịch theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable Long id) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Transaction transaction = transactionRepo.findById(id).orElse(null);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu
        if (!transaction.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        return ResponseEntity.ok(transaction);
    }

    // Tạo giao dịch mới
    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody Map<String, Object> request) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setTransType((String) request.get("transType"));
            transaction.setAmount(new BigDecimal(request.get("amount").toString()));
            transaction.setTransactionDate(LocalDate.parse((String) request.get("transactionDate")));
            transaction.setDescription((String) request.get("description"));
            
            // Set category nếu có
            if (request.get("categoryId") != null) {
                Long categoryId = Long.parseLong(request.get("categoryId").toString());
                transaction.setCategory(categoryRepo.findById(categoryId).orElse(null));
            }
            
            // Set wallet nếu có
            if (request.get("walletId") != null) {
                Long walletId = Long.parseLong(request.get("walletId").toString());
                transaction.setWallet(walletRepo.findById(walletId).orElse(null));
            }
            
            Transaction savedTransaction = transactionRepo.save(transaction);
            return ResponseEntity.ok(savedTransaction);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tạo giao dịch: " + e.getMessage()));
        }
    }

    // Cập nhật giao dịch
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Transaction transaction = transactionRepo.findById(id).orElse(null);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu
        if (!transaction.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        try {
            if (request.get("transType") != null) {
                transaction.setTransType((String) request.get("transType"));
            }
            if (request.get("amount") != null) {
                transaction.setAmount(new BigDecimal(request.get("amount").toString()));
            }
            if (request.get("transactionDate") != null) {
                transaction.setTransactionDate(LocalDate.parse((String) request.get("transactionDate")));
            }
            if (request.get("description") != null) {
                transaction.setDescription((String) request.get("description"));
            }
            
            // Update category nếu có
            if (request.get("categoryId") != null) {
                Long categoryId = Long.parseLong(request.get("categoryId").toString());
                transaction.setCategory(categoryRepo.findById(categoryId).orElse(null));
            }
            
            // Update wallet nếu có
            if (request.get("walletId") != null) {
                Long walletId = Long.parseLong(request.get("walletId").toString());
                transaction.setWallet(walletRepo.findById(walletId).orElse(null));
            }
            
            Transaction updatedTransaction = transactionRepo.save(transaction);
            return ResponseEntity.ok(updatedTransaction);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi cập nhật giao dịch: " + e.getMessage()));
        }
    }

    // Xóa giao dịch
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Transaction transaction = transactionRepo.findById(id).orElse(null);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu
        if (!transaction.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        transactionRepo.delete(transaction);
        return ResponseEntity.ok(Map.of("message", "Đã xóa giao dịch thành công"));
    }

    private String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
} 
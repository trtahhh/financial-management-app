package com.example.finance.controller;

import com.example.finance.entity.*;
import com.example.finance.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestDataController {
    @Autowired UserRepository userRepo;
    @Autowired CategoryRepository categoryRepo;
    @Autowired WalletRepository walletRepo;
    @Autowired TransactionRepository transactionRepo;
    @Autowired BudgetRepository budgetRepo;

    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser() {
        String email = getCurrentEmail();
        System.out.println("TestDataController - Get current user email: " + email);
        
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User currentUser = userRepo.findByEmail(email).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        return ResponseEntity.ok(Map.of(
            "email", currentUser.getEmail(),
            "username", currentUser.getUsername(),
            "fullName", currentUser.getFullName(),
            "role", currentUser.getRole()
        ));
    }

    @PostMapping("/sample-data")
    public ResponseEntity<?> createSampleData() {
        String email = getCurrentEmail();
        System.out.println("TestDataController - Current email: " + email);
        
        if (email == null) {
            System.out.println("TestDataController - No email found, returning 401");
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User currentUser = userRepo.findByEmail(email).orElse(null);
        System.out.println("TestDataController - User found: " + (currentUser != null ? currentUser.getEmail() : "null"));
        
        if (currentUser == null) {
            System.out.println("TestDataController - User not found, returning 404");
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        try {
            // Xóa dữ liệu cũ của user này
            System.out.println("TestDataController - Cleaning old data for user: " + currentUser.getEmail());
            transactionRepo.deleteByUser(currentUser);
            budgetRepo.deleteByUser(currentUser);
            categoryRepo.deleteByUser(currentUser);
            walletRepo.deleteByUser(currentUser);
            
            // Tạo categories mẫu
            Category foodCategory = new Category();
            foodCategory.setUser(currentUser);
            foodCategory.setName("Ăn uống");
            foodCategory.setType("expense");
            foodCategory.setIcon("🍽️");
            foodCategory.setIsDefault(false);
            foodCategory.setCreatedAt(LocalDateTime.now());
            foodCategory = categoryRepo.save(foodCategory);

            Category transportCategory = new Category();
            transportCategory.setUser(currentUser);
            transportCategory.setName("Giao thông");
            transportCategory.setType("expense");
            transportCategory.setIcon("🚗");
            transportCategory.setIsDefault(false);
            transportCategory.setCreatedAt(LocalDateTime.now());
            transportCategory = categoryRepo.save(transportCategory);

            Category salaryCategory = new Category();
            salaryCategory.setUser(currentUser);
            salaryCategory.setName("Lương");
            salaryCategory.setType("income");
            salaryCategory.setIcon("💰");
            salaryCategory.setIsDefault(false);
            salaryCategory.setCreatedAt(LocalDateTime.now());
            salaryCategory = categoryRepo.save(salaryCategory);

            // Tạo wallets mẫu
            Wallet cashWallet = new Wallet();
            cashWallet.setUser(currentUser);
            cashWallet.setName("Tiền mặt");
            cashWallet.setType("cash");
            cashWallet.setBalance(new BigDecimal("1000000"));
            cashWallet.setNote("Ví tiền mặt");
            cashWallet.setCreatedAt(LocalDateTime.now());
            cashWallet = walletRepo.save(cashWallet);

            Wallet bankWallet = new Wallet();
            bankWallet.setUser(currentUser);
            bankWallet.setName("Ngân hàng");
            bankWallet.setType("bank");
            bankWallet.setBalance(new BigDecimal("5000000"));
            bankWallet.setNote("Tài khoản ngân hàng");
            bankWallet.setCreatedAt(LocalDateTime.now());
            bankWallet = walletRepo.save(bankWallet);

            // Tạo transactions mẫu
            Transaction transaction1 = new Transaction();
            transaction1.setUser(currentUser);
            transaction1.setWallet(cashWallet);
            transaction1.setCategory(foodCategory);
            transaction1.setAmount(new BigDecimal("150000"));
            transaction1.setTransType("expense");
            transaction1.setDescription("Ăn trưa tại nhà hàng");
            transaction1.setTransactionDate(LocalDate.now());
            transaction1.setCreatedAt(LocalDateTime.now());
            transactionRepo.save(transaction1);

            Transaction transaction2 = new Transaction();
            transaction2.setUser(currentUser);
            transaction2.setWallet(bankWallet);
            transaction2.setCategory(salaryCategory);
            transaction2.setAmount(new BigDecimal("15000000"));
            transaction2.setTransType("income");
            transaction2.setDescription("Lương tháng 7");
            transaction2.setTransactionDate(LocalDate.now().minusDays(5));
            transaction2.setCreatedAt(LocalDateTime.now());
            transactionRepo.save(transaction2);

            Transaction transaction3 = new Transaction();
            transaction3.setUser(currentUser);
            transaction3.setWallet(cashWallet);
            transaction3.setCategory(transportCategory);
            transaction3.setAmount(new BigDecimal("200000"));
            transaction3.setTransType("expense");
            transaction3.setDescription("Mua xăng xe");
            transaction3.setTransactionDate(LocalDate.now().minusDays(2));
            transaction3.setCreatedAt(LocalDateTime.now());
            transactionRepo.save(transaction3);

            // Tạo budgets mẫu
            Budget foodBudget = new Budget();
            foodBudget.setUser(currentUser);
            foodBudget.setName("Ngân sách ăn uống tháng 7");
            foodBudget.setTotal(new BigDecimal("2000000"));
            foodBudget.setStartDate(LocalDate.now().withDayOfMonth(1));
            foodBudget.setEndDate(LocalDate.now().withDayOfMonth(31));
            foodBudget.setStatus("active");
            foodBudget.setCreatedAt(LocalDateTime.now());
            budgetRepo.save(foodBudget);

            Budget transportBudget = new Budget();
            transportBudget.setUser(currentUser);
            transportBudget.setName("Ngân sách giao thông tháng 7");
            transportBudget.setTotal(new BigDecimal("1000000"));
            transportBudget.setStartDate(LocalDate.now().withDayOfMonth(1));
            transportBudget.setEndDate(LocalDate.now().withDayOfMonth(31));
            transportBudget.setStatus("active");
            transportBudget.setCreatedAt(LocalDateTime.now());
            budgetRepo.save(transportBudget);

            return ResponseEntity.ok(Map.of(
                "message", "Đã tạo dữ liệu mẫu thành công",
                "user", currentUser.getEmail(),
                "categories", 3,
                "wallets", 2,
                "transactions", 3,
                "budgets", 2
            ));

        } catch (Exception e) {
            System.out.println("TestDataController - Error creating sample data: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tạo dữ liệu mẫu: " + e.getMessage()));
        }
    }
    
    private String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
} 
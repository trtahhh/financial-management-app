package com.example.finance.controller;

import com.example.finance.entity.Wallet;
import com.example.finance.entity.User;
import com.example.finance.repository.WalletRepository;
import com.example.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {
    @Autowired 
    WalletRepository walletRepo;
    
    @Autowired
    UserRepository userRepo;

    // Lấy danh sách ví
    @GetMapping
    public ResponseEntity<?> getWallets() {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<Wallet> wallets = walletRepo.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(wallets);
    }

    // Lấy ví theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getWallet(@PathVariable Long id) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Wallet wallet = walletRepo.findById(id).orElse(null);
        if (wallet == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu
        if (!wallet.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        return ResponseEntity.ok(wallet);
    }

    // Tạo ví mới
    @PostMapping
    public ResponseEntity<?> createWallet(@RequestBody Map<String, Object> request) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setName((String) request.get("name"));
            wallet.setType((String) request.get("type"));
            wallet.setNote((String) request.get("note"));
            wallet.setBalance(new BigDecimal(request.get("balance").toString()));
            
            Wallet savedWallet = walletRepo.save(wallet);
            return ResponseEntity.ok(savedWallet);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tạo ví: " + e.getMessage()));
        }
    }

    // Cập nhật ví
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWallet(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Wallet wallet = walletRepo.findById(id).orElse(null);
        if (wallet == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu
        if (!wallet.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        try {
            if (request.get("name") != null) {
                wallet.setName((String) request.get("name"));
            }
            if (request.get("type") != null) {
                wallet.setType((String) request.get("type"));
            }
            if (request.get("note") != null) {
                wallet.setNote((String) request.get("note"));
            }
            if (request.get("balance") != null) {
                wallet.setBalance(new BigDecimal(request.get("balance").toString()));
            }
            
            Wallet updatedWallet = walletRepo.save(wallet);
            return ResponseEntity.ok(updatedWallet);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi cập nhật ví: " + e.getMessage()));
        }
    }

    // Xóa ví
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWallet(@PathVariable Long id) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Wallet wallet = walletRepo.findById(id).orElse(null);
        if (wallet == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra quyền sở hữu
        if (!wallet.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        walletRepo.delete(wallet);
        return ResponseEntity.ok(Map.of("message", "Đã xóa ví thành công"));
    }

    private String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
} 
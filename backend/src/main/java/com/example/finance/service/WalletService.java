package com.example.finance.service;

import com.example.finance.entity.Wallet;
import com.example.finance.entity.User;
import com.example.finance.repository.WalletRepository;
import com.example.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WalletService {
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Create new wallet
    public Wallet createWallet(Wallet wallet, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        wallet.setUser(user);
        wallet.setCreatedAt(java.time.LocalDateTime.now());
        
        return walletRepository.save(wallet);
    }
    
    // Get wallet by ID
    public Wallet getWalletById(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        return wallet;
    }
    
    // Get all wallets for user
    public List<Wallet> getUserWallets(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return walletRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user);
    }
    
    // Get active wallets
    public List<Wallet> getActiveWallets(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return walletRepository.findActiveWalletsByUser(user);
    }
    
    // Get wallets by type
    public List<Wallet> getWalletsByType(String username, String type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return walletRepository.findByUserAndTypeAndIsDeletedFalse(user, type);
    }
    
    // Update wallet
    public Wallet updateWallet(Long id, Wallet walletDetails, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        wallet.setName(walletDetails.getName());
        wallet.setType(walletDetails.getType());
        wallet.setBalance(walletDetails.getBalance());
        wallet.setNote(walletDetails.getNote());
        
        return walletRepository.save(wallet);
    }
    
    // Delete wallet (soft delete)
    public void deleteWallet(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        wallet.setIsDeleted(true);
        walletRepository.save(wallet);
    }
    
    // Get wallet summary
    public Map<String, Object> getWalletSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Double totalBalance = walletRepository.getTotalBalanceByUser(user);
        Long walletCount = walletRepository.countWalletsByUser(user);
        
        List<Object[]> typeStatistics = walletRepository.getWalletStatisticsByType(user);
        
        Map<String, Object> summary = Map.of(
            "totalBalance", totalBalance,
            "walletCount", walletCount,
            "typeStatistics", typeStatistics
        );
        
        return summary;
    }
} 
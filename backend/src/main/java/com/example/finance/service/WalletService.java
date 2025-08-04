package com.example.finance.service;

import com.example.finance.dto.WalletDTO;
import com.example.finance.entity.Wallet;
import com.example.finance.mapper.WalletMapper;
import com.example.finance.repository.WalletRepository;
import com.example.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository repo;
    private final TransactionRepository transactionRepository;
    private final WalletMapper mapper;

    public List<WalletDTO> findAll() {
        try {
            // TEMPORARY: Get wallets for userId = 1 for testing
            List<Wallet> wallets = repo.findByUserId(1L);
            
            // Tính toán số dư thực tế cho mỗi ví dựa trên giao dịch
            for (Wallet wallet : wallets) {
                BigDecimal totalIncome = transactionRepository.sumByWalletIdAndType(wallet.getId(), "income");
                BigDecimal totalExpense = transactionRepository.sumByWalletIdAndType(wallet.getId(), "expense");
                
                totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
                totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
                
                // Số dư thực tế = Thu nhập - Chi tiêu (ví ban đầu = 0)
                BigDecimal actualBalance = totalIncome.subtract(totalExpense);
                wallet.setBalance(actualBalance);
            }
            
            return wallets.stream().map(mapper::toDto).toList();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching wallets: " + e.getMessage());
        }
    }

    @Transactional
    public WalletDTO save(WalletDTO dto) {
        try {
            // TEMPORARY: Set userId = 1 for testing
            if (dto.getUserId() == null) {
                dto.setUserId(1L);
            }
            return mapper.toDto(repo.save(mapper.toEntity(dto)));
        } catch (Exception e) {
            throw new RuntimeException("Error saving wallet: " + e.getMessage());
        }
    }

    public WalletDTO findById(Long id) {
        return repo.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + id));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Wallet not found with id: " + id);
        }
        
        // Kiểm tra xem có transaction nào liên kết với ví này không
        boolean hasTransactions = transactionRepository.existsByWalletId(id);
        if (hasTransactions) {
            throw new RuntimeException("Cannot delete wallet with existing transactions. Please delete related transactions first.");
        }
        
        try {
            repo.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting wallet: " + e.getMessage());
        }
    }

    @Transactional
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
        
        // Lưu số dư ban đầu (có thể lấy từ database ban đầu hoặc giá trị được thiết lập)
        // Nếu muốn giữ số dư ban đầu, cần một cách để lưu trữ giá trị này
        // Hiện tại, tôi sẽ sử dụng logic: Số dư hiện tại = Thu nhập - Chi tiêu
        // (Giả định rằng số dư ban đầu đã được tính vào giao dịch đầu tiên)
        
        // Tính tổng thu nhập (income) và chi tiêu (expense) cho ví này
        BigDecimal totalIncome = transactionRepository.sumByWalletIdAndType(walletId, "income");
        BigDecimal totalExpense = transactionRepository.sumByWalletIdAndType(walletId, "expense");
        
        // Xử lý trường hợp null
        totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
        
        // Số dư = Thu nhập - Chi tiêu
        BigDecimal newBalance = totalIncome.subtract(totalExpense);
        wallet.setBalance(newBalance);
        
        repo.save(wallet);
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
     * Cập nhật số dư cho tất cả ví (sử dụng userId = 1 cho testing)
     */
    @Transactional
    public void updateAllWalletBalances() {
        updateAllWalletBalances(1L);
    }
}

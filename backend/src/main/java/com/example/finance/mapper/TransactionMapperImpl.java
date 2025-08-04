package com.example.finance.mapper;

import com.example.finance.dto.CategoryDTO;
import com.example.finance.dto.TransactionDTO;
import com.example.finance.dto.WalletDTO;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapperImpl {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    public TransactionDTO toDto(Transaction entity) {
        if (entity == null) {
            return null;
        }

        TransactionDTO dto = new TransactionDTO();
        dto.setId(entity.getId());
        dto.setAmount(entity.getAmount());
        dto.setType(entity.getType());
        dto.setNote(entity.getNote());
        dto.setDate(entity.getDate());
        dto.setFilePath(entity.getFilePath());
        dto.setStatus(entity.getStatus());
        dto.setTags(entity.getTags());
        dto.setIsDeleted(entity.isDeleted());
        dto.setDeletedAt(entity.getDeletedAt() != null ? entity.getDeletedAt().toLocalDate() : null);
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toLocalDate() : null);
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toLocalDate() : null);

        // Map user
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
        }

        // Map category with full object
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            CategoryDTO categoryDTO = new CategoryDTO();
            categoryDTO.setId(entity.getCategory().getId());
            categoryDTO.setName(entity.getCategory().getName());
            categoryDTO.setType(entity.getCategory().getType());
            dto.setCategory(categoryDTO);
        } else {
            dto.setCategoryId(null);
            dto.setCategory(null);
        }

        // Map wallet with full object
        if (entity.getWallet() != null) {
            dto.setWalletId(entity.getWallet().getId());
            WalletDTO walletDTO = new WalletDTO();
            walletDTO.setId(entity.getWallet().getId());
            walletDTO.setName(entity.getWallet().getName());
            walletDTO.setType(entity.getWallet().getType());
            walletDTO.setBalance(entity.getWallet().getBalance());
            dto.setWallet(walletDTO);
        } else {
            dto.setWalletId(null);
            dto.setWallet(null);
        }

        return dto;
    }

    public Transaction toEntity(TransactionDTO dto) {
        if (dto == null) {
            return null;
        }

        Transaction entity = new Transaction();
        entity.setId(dto.getId());
        entity.setAmount(dto.getAmount());
        entity.setType(dto.getType());
        entity.setNote(dto.getNote());
        entity.setDate(dto.getDate());
        entity.setFilePath(dto.getFilePath());
        entity.setStatus(dto.getStatus());
        entity.setTags(dto.getTags());
        entity.setDeleted(dto.getIsDeleted() != null ? dto.getIsDeleted() : false);

        // Map user
        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId()).orElse(null);
            entity.setUser(user);
        }

        // Map category
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
            entity.setCategory(category);
        }

        // Map wallet
        if (dto.getWalletId() != null) {
            Wallet wallet = walletRepository.findById(dto.getWalletId()).orElse(null);
            entity.setWallet(wallet);
        }

        return entity;
    }
}

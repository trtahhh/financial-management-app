package com.example.finance.dto;

import com.example.finance.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionDto {
    
    private Long id;
    
    @NotNull(message = "Ví không được để trống")
    private Long walletId;
    
    @NotBlank(message = "Loại giao dịch không được để trống")
    private String transType; // income/expense
    
    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;
    
    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;
    
    @Size(max = 255, message = "Mô tả không được quá 255 ký tự")
    private String description;
    
    @NotNull(message = "Ngày giao dịch không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // Constructors
    public TransactionDto() {}
    
    public TransactionDto(Long id, Long walletId, String transType, Long categoryId, BigDecimal amount, 
                        String description, LocalDate transactionDate) {
        this.id = id;
        this.walletId = walletId;
        this.transType = transType;
        this.categoryId = categoryId;
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate;
    }
    
    // Static factory method to create from entity
    public static TransactionDto fromEntity(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setWalletId(transaction.getWallet().getId());
        dto.setTransType(transaction.getTransType());
        dto.setCategoryId(transaction.getCategory().getId());
        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getDescription());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getWalletId() {
        return walletId;
    }
    
    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }
    
    public String getTransType() {
        return transType;
    }
    
    public void setTransType(String transType) {
        this.transType = transType;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 
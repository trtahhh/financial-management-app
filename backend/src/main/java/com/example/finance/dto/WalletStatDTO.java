package com.example.finance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class WalletStatDTO {
 private Long walletId;
 private String walletName;
 private BigDecimal totalAmount;
 private Long transactionCount;

 // Constructor for JPQL: SELECT NEW WalletStatDTO(w.id, w.name, SUM(t.amount), COUNT(t))
 public WalletStatDTO(Long walletId, String walletName, BigDecimal totalAmount, Long transactionCount) {
 this.walletId = walletId;
 this.walletName = walletName;
 this.totalAmount = totalAmount;
 this.transactionCount = transactionCount;
 }

 // Constructor for backward compatibility with TransactionService mapping
 public WalletStatDTO(Long walletId, BigDecimal totalAmount, Long transactionCount) {
 this(walletId, null, totalAmount, transactionCount);
 }
}

package com.example.finance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletStatDTO {
    private Long walletId;
    private Double totalAmount;
    private Long transactionCount; 
}

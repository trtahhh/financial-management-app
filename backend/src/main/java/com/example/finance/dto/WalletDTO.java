package com.example.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletDTO {
    private Long id;
    private String name;
    private BigDecimal balance;
    private Long userId;
}

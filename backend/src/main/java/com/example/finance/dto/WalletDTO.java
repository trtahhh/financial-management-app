package com.example.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletDTO {
 private Long id;
 private String name;
 private String type;
 private BigDecimal balance;
 private String currency;
 private String description;
 private Long userId;
}

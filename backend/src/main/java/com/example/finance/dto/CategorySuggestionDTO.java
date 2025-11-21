package com.example.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CategorySuggestionDTO {
    private Long id;
    private String suggestedName;
    private String suggestedType;
    private String suggestedColor;
    private String suggestedIcon;
    private BigDecimal confidenceScore;
    private String reasoning;
    private List<String> sampleDescriptions;
    private Integer transactionCount;
    private String status;
    private LocalDateTime createdAt;
}

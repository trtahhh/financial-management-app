package com.example.finance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CategoryDTO {
    private Long id;
    private Long userId; 
    private String name;
    private String type; 
    private String color;
    private String icon;
    private LocalDateTime createdAt;
    private Boolean isSystem;
}
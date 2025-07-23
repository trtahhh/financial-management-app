package com.example.finance.dto;

import com.example.finance.entity.Budget;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BudgetDto {
    
    private Long id;
    
    @NotBlank(message = "Tên ngân sách không được để trống")
    private String name;
    
    @NotNull(message = "Số tiền ngân sách không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal total;
    
    @NotNull(message = "Ngày bắt đầu không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @NotNull(message = "Ngày kết thúc không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Helper fields for frontend
    private Boolean isActive;
    private Boolean isExpired;
    private Boolean isUpcoming;
    
    // Constructors
    public BudgetDto() {}
    
    public BudgetDto(Long id, String name, BigDecimal total, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.name = name;
        this.total = total;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Static factory method to create from entity
    public static BudgetDto fromEntity(Budget budget) {
        BudgetDto dto = new BudgetDto();
        dto.setId(budget.getId());
        dto.setName(budget.getName());
        dto.setTotal(budget.getTotal());
        dto.setStartDate(budget.getStartDate());
        dto.setEndDate(budget.getEndDate());
        dto.setStatus(budget.getStatus());
        dto.setCreatedAt(budget.getCreatedAt());
        
        // Calculate helper fields
        dto.setIsActive(budget.isActive());
        dto.setIsExpired(budget.isExpired());
        dto.setIsUpcoming(budget.isUpcoming());
        
        return dto;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BigDecimal getTotal() {
        return total;
    }
    
    public void setTotal(BigDecimal total) {
        this.total = total;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsExpired() {
        return isExpired;
    }
    
    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }
    
    public Boolean getIsUpcoming() {
        return isUpcoming;
    }
    
    public void setIsUpcoming(Boolean isUpcoming) {
        this.isUpcoming = isUpcoming;
    }
} 
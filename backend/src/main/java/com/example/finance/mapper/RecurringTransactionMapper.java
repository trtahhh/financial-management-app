package com.example.finance.mapper;

import com.example.finance.dto.RecurringTransactionDTO;
import com.example.finance.entity.RecurringTransaction;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionMapper {

    public RecurringTransactionDTO toDto(RecurringTransaction entity) {
        if (entity == null) {
            return null;
        }
        
        RecurringTransactionDTO dto = new RecurringTransactionDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        dto.setWalletId(entity.getWallet() != null ? entity.getWallet().getId() : null);
        dto.setCategoryId(entity.getCategory() != null ? entity.getCategory().getId() : null);
        dto.setAmount(entity.getAmount());
        dto.setType(entity.getType());
        dto.setNote(entity.getNote());
        dto.setFrequency(entity.getFrequency());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setNextExecution(entity.getNextExecution());
        dto.setIsActive(entity.getIsActive());
        
        return dto;
    }

    public RecurringTransaction toEntity(RecurringTransactionDTO dto) {
        if (dto == null) {
            return null;
        }
        
        RecurringTransaction entity = new RecurringTransaction();
        entity.setId(dto.getId());
        entity.setAmount(dto.getAmount());
        entity.setType(dto.getType());
        entity.setNote(dto.getNote());
        entity.setFrequency(dto.getFrequency());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setNextExecution(dto.getNextExecution());
        entity.setIsActive(dto.getIsActive());
        
        return entity;
    }
}
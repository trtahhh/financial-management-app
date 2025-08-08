package com.example.finance.service;

import com.example.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.finance.dto.CategoryStatisticDTO;
import com.example.finance.dto.SummaryDTO;


@Service
@RequiredArgsConstructor
public class StatisticService {
    private final TransactionRepository transactionRepository;

    public SummaryDTO getSummary(Long userId, Integer month, Integer year) {
        System.out.println("🔍 StatisticService getSummary: userId=" + userId + ", month=" + month + ", year=" + year);
        
        // Use database transaction types: income and expense
        BigDecimal totalIncome = transactionRepository.sumAmountByUserAndType(userId, "income", month, year);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserAndType(userId, "expense", month, year);

        System.out.println("📊 Raw results: totalIncome=" + totalIncome + ", totalExpense=" + totalExpense);
        
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        BigDecimal balance = totalIncome.subtract(totalExpense);

        System.out.println("✅ Final summary: income=" + totalIncome + ", expense=" + totalExpense + ", balance=" + balance);
        
        return new SummaryDTO(
            totalIncome.doubleValue(), 
            totalExpense.doubleValue(), 
            balance.doubleValue()
        );
    }

    public List<CategoryStatisticDTO> getByCategory(Long userId, Integer month, Integer year) {
        // Use the repository method that returns CategoryStatisticDTO directly
        return transactionRepository.findExpenseStatisticsByCategory(userId, month, year);
    }
}

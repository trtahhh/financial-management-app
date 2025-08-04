package com.example.finance.service;

import com.example.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.finance.dto.CategoryStatisticDTO;
import com.example.finance.dto.SummaryDTO;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.Category;
import com.example.finance.repository.CategoryRepository;


@Service
@RequiredArgsConstructor
public class StatisticService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

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
        List<Category> categories = categoryRepository.findAll();
        List<Transaction> transactions = transactionRepository
            .findByUserIdAndMonthAndYear(userId, month, year);

        Map<Long, CategoryStatisticDTO> map = new HashMap<>();
        for (Category c : categories) {
            CategoryStatisticDTO dto = new CategoryStatisticDTO();
            dto.setCategoryId(c.getId());
            dto.setCategoryName(c.getName());
            dto.setTotalIncome(0.0);
            dto.setTotalExpense(0.0);
            dto.setBalance(0.0);
            map.put(c.getId(), dto);
        }
        for (Transaction t : transactions) {
            CategoryStatisticDTO dto = map.get(t.getCategory().getId());
            if (dto == null) continue;
            
            // Use correct Vietnamese transaction types: THU (income) and CHI (expense)
            if (t.getType().equalsIgnoreCase("THU")) {
                dto.setTotalIncome(dto.getTotalIncome() + t.getAmount().doubleValue());
            } else if (t.getType().equalsIgnoreCase("CHI")) {
                dto.setTotalExpense(dto.getTotalExpense() + t.getAmount().doubleValue());
            }
            dto.setBalance(dto.getTotalIncome() - dto.getTotalExpense());
        }
        return new ArrayList<>(map.values());
    }
}

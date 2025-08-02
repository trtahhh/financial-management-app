package com.example.finance.service;

import com.example.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;

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
        Double totalIncome = transactionRepository.sumAmountByUserAndType(userId, "INCOME", month, year);
        Double totalExpense = transactionRepository.sumAmountByUserAndType(userId, "EXPENSE", month, year);

        if (totalIncome == null) totalIncome = 0.0;
        if (totalExpense == null) totalExpense = 0.0;
        Double balance = totalIncome - totalExpense;

        return new SummaryDTO(totalIncome, totalExpense, balance);
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
            if (t.getType().equalsIgnoreCase("income")) {
                dto.setTotalIncome(dto.getTotalIncome() + t.getAmount().doubleValue());
            } else {
                dto.setTotalExpense(dto.getTotalExpense() + t.getAmount().doubleValue());
            }
            dto.setBalance(dto.getTotalIncome() - dto.getTotalExpense());
        }
        return new ArrayList<>(map.values());
    }
}

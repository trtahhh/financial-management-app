package com.example.finance.controller;

import com.example.finance.dto.BudgetDTO;
import com.example.finance.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService service;

    @GetMapping
    public List<BudgetDTO> getAll() {
        return service.getAllBudgets();
    }

    @GetMapping("/{id}")
    public BudgetDTO getById(@PathVariable Long id) {
        return service.getBudgetById(id);
    }

    @PostMapping
    public BudgetDTO create(@RequestBody BudgetDTO dto) {
        return service.createBudget(dto);
    }

    @PutMapping("/{id}")
    public BudgetDTO update(@PathVariable Long id, @RequestBody BudgetDTO dto) {
        return service.updateBudget(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteBudget(id);
    }
}

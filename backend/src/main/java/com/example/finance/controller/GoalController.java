package com.example.finance.controller;

import com.example.finance.dto.GoalDTO;
import com.example.finance.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService service;

    @GetMapping
    public List<GoalDTO> list() { return service.findAll(); }

    @GetMapping("/predict")
    public Map<String, BigDecimal> predict() {
        return Map.of("nextMonth", service.predictNextMonth());
    }

    @PostMapping
    public GoalDTO create(@RequestBody GoalDTO dto) {
        return service.save(dto);
    }

    @GetMapping("/{id}")
    public GoalDTO getById(@PathVariable("id") Long id) {   
        return service.findById(id);
    }   

    @PutMapping("/{id}")
    public GoalDTO update(@PathVariable("id") Long id, @RequestBody GoalDTO dto) {
        dto.setId(id);
        return service.update(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        service.deleteById(id);
    }
}

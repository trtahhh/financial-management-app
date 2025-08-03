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
    public List<GoalDTO> list() { 
        // Tạm thời lấy goals của user ID = 1
        return service.findByUserId(1L); 
    }

    @GetMapping("/predict")
    public Map<String, String> predict() {
        BigDecimal predicted = service.predictNextMonth();
        String message = String.format("Dự đoán thu nhập tháng tới: %,d VND", predicted.longValue());
        return Map.of("message", message);
    }

    @PostMapping
    public GoalDTO create(@RequestBody GoalDTO dto) {
        // Tạm thời set userId = 1 (user mặc định)
        if (dto.getUserId() == null) {
            dto.setUserId(1L);
        }
        return service.save(dto);
    }

    @GetMapping("/{id}")
    public GoalDTO getById(@PathVariable("id") Long id) {   
        return service.findById(id);
    }   

    @PutMapping("/{id}")
    public GoalDTO update(@PathVariable("id") Long id, @RequestBody GoalDTO dto) {
        dto.setId(id);
        // Tạm thời set userId = 1 (user mặc định) nếu chưa có
        if (dto.getUserId() == null) {
            dto.setUserId(1L);
        }
        return service.update(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        service.deleteById(id);
    }
}

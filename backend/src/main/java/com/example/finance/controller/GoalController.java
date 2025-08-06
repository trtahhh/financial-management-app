package com.example.finance.controller;

import com.example.finance.dto.GoalDTO;
import com.example.finance.service.GoalService;
import lombok.RequiredArgsConstructor;
import com.example.finance.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService service;

    @Autowired
    private JwtUtil jwtUtil;

    private Long extractUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        return jwtUtil.getUserId(token);
    }

    @GetMapping
    public List<GoalDTO> list(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return service.findByUserId(userId);
    }

    @GetMapping("/predict")
    public Map<String, String> predict(HttpServletRequest request) {
        Long userId = extractUserId(request);
        BigDecimal predicted = service.predictNextMonth(userId);
        String message = String.format("Dự đoán thu nhập tháng tới: %,d VND", predicted.longValue());
        return Map.of("message", message);
    }

    @PostMapping
    public GoalDTO create(@RequestBody GoalDTO dto, HttpServletRequest request) {
        if (dto.getUserId() == null) {
            Long userId = extractUserId(request);
            dto.setUserId(userId);
        }
        return service.save(dto);
    }

    @GetMapping("/{id}")
    public GoalDTO getById(@PathVariable("id") Long id) {   
        return service.findById(id);
    }   

    @PutMapping("/{id}")
    public GoalDTO update(@PathVariable("id") Long id, @RequestBody GoalDTO dto, HttpServletRequest request) {
        dto.setId(id);
        if (dto.getUserId() == null) {
            Long userId = extractUserId(request);
            dto.setUserId(userId);
        }
        return service.update(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        service.deleteById(id);
    }
}

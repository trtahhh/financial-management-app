package com.example.finance.controller;

import com.example.finance.dto.CategoryStatisticDTO;
import com.example.finance.dto.SummaryDTO;
import com.example.finance.service.StatisticService;
import lombok.RequiredArgsConstructor;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticController {
    private final StatisticService statisticService;

    @GetMapping("/summary")
    public SummaryDTO getSummary(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        System.out.println("📥 API CALLED: userId=" + userId + ", month=" + month + ", year=" + year);
        return statisticService.getSummary(userId, month, year);
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<CategoryStatisticDTO>> byCategory(
        @RequestParam Long userId,
        @RequestParam(required = false) Integer month,
        @RequestParam(required = false) Integer year
    ) {
        List<CategoryStatisticDTO> stats = statisticService.getByCategory(userId, month, year);
        return ResponseEntity.ok(stats);
    }
}

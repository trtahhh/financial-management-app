package com.example.finance.controller;

import com.example.finance.security.CustomUserDetails;

import com.example.finance.dto.BudgetDTO;
import com.example.finance.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BudgetController {

    private final BudgetService service;

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            List<BudgetDTO> budgets = service.getAllBudgets();
            log.info("Retrieved {} budgets", budgets.size());
            return ResponseEntity.ok(budgets);
        } catch (Exception e) {
            log.error("Error getting budgets", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi lấy danh sách ngân sách: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            BudgetDTO budget = service.getBudgetById(id);
            if (budget == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Không tìm thấy ngân sách"));
            }
            return ResponseEntity.ok(budget);
        } catch (Exception e) {
            log.error("Error getting budget by id: {}", id, e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi lấy thông tin ngân sách: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BudgetDTO dto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            dto.setUserId(userId);

            log.info("Creating budget with data: {}", dto);
            BudgetDTO result = service.createBudget(dto);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tạo ngân sách thành công",
                "data", result
            ));
        } catch (Exception e) {
            log.error("Error creating budget", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi tạo ngân sách: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody BudgetDTO dto) {
        try {
            BudgetDTO result = service.updateBudget(id, dto);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật ngân sách thành công",
                "data", result
            ));
        } catch (Exception e) {
            log.error("Error updating budget: {}", id, e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi cập nhật ngân sách: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.deleteBudget(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xóa ngân sách thành công"
            ));
        } catch (Exception e) {
            log.error("Error deleting budget: {}", id, e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi xóa ngân sách: " + e.getMessage()));
        }
    }
}

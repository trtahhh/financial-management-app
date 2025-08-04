package com.example.finance.controller;

import com.example.finance.dto.TransactionDTO;
import com.example.finance.dto.WalletStatDTO;
import com.example.finance.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService service;

    @GetMapping
    public ResponseEntity<?> list() {
        try {
            List<TransactionDTO> transactions = service.findAll();
            log.info("Retrieved {} transactions", transactions.size());
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error getting transactions", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi lấy danh sách giao dịch: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TransactionDTO dto) {
        try {
            log.info("Creating transaction with data: {}", dto);
            TransactionDTO created = service.save(dto, null);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tạo giao dịch thành công",
                "data", created
            ));
        } catch (Exception e) {
            log.error("Error creating transaction", e);
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "message", "Lỗi tạo giao dịch: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id) {
        try {
            TransactionDTO dto = service.findById(id);
            if (dto == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Không tìm thấy giao dịch"));
            }
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error getting transaction by id: {}", id, e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi lấy thông tin giao dịch: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") Long id,
            @RequestBody TransactionDTO dto
    ) {
        try {
            dto.setId(id);
            TransactionDTO updated = service.save(dto, null);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật giao dịch thành công",
                "data", updated
            ));
        } catch (Exception e) {
            log.error("Error updating transaction", e);
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "message", "Lỗi cập nhật giao dịch: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "transactions", key = "#id")
    public ResponseEntity<Void> delete(
            @PathVariable("id") Long id
    ) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats-by-category")
    public ResponseEntity<List<Map<String, Object>>> statsByCategory(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        List<Map<String, Object>> result = service.sumAmountByCategory(userId, month, year);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/wallet")
    public ResponseEntity<List<WalletStatDTO>> getSumAmountByWallet(
            @RequestParam Long userId,
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(service.getSumAmountByWallet(userId, walletId, type, month, year));
    }

    @GetMapping("/stats/wallet/count")
    public ResponseEntity<List<WalletStatDTO>> getCountTransactionsByWallet(
            @RequestParam Long userId,
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(service.getCountTransactionsByWallet(userId, walletId, type, month, year));
    }
}

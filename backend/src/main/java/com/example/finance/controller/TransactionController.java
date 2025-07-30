package com.example.finance.controller;

import com.example.finance.dto.TransactionDTO;
import com.example.finance.dto.WalletStatDTO;
import com.example.finance.service.TransactionService;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @GetMapping
    public List<TransactionDTO> list() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> create(
            @RequestBody TransactionDTO dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        TransactionDTO created = service.save(dto, file);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getById(
            @PathVariable("id") Long id
    ) {
        TransactionDTO dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDTO> update(
            @PathVariable("id") Long id,
            @RequestBody TransactionDTO dto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        dto.setId(id);
        TransactionDTO updated = service.save(dto, file);
        return ResponseEntity.ok(updated);
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

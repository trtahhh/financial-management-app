package com.example.finance.controller;

import com.example.finance.dto.RecurringTransactionDTO;
import com.example.finance.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
@RequiredArgsConstructor
public class RecurringTransactionController {

 private final RecurringTransactionService service;

 @GetMapping("/user/{userId}")
 public ResponseEntity<List<RecurringTransactionDTO>> getByUserId(@PathVariable Long userId) {
 return ResponseEntity.ok(service.findByUserId(userId));
 }

 @PostMapping
 public ResponseEntity<RecurringTransactionDTO> create(@RequestBody RecurringTransactionDTO dto) {
 return ResponseEntity.ok(service.create(dto));
 }

 @PutMapping("/{id}")
 public ResponseEntity<RecurringTransactionDTO> update(@PathVariable Long id, @RequestBody RecurringTransactionDTO dto) {
 return ResponseEntity.ok(service.update(id, dto));
 }

 @DeleteMapping("/{id}")
 public ResponseEntity<Void> delete(@PathVariable Long id) {
 service.delete(id);
 return ResponseEntity.ok().build();
 }

 @PostMapping("/{id}/toggle")
 public ResponseEntity<RecurringTransactionDTO> toggleActive(@PathVariable Long id) {
 return ResponseEntity.ok(service.toggleActive(id));
 }
}

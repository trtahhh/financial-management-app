package com.example.finance.controller;

import com.example.finance.dto.RecurringTransactionDTO;
import com.example.finance.service.RecurringTransactionService;
import com.example.finance.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recurring-transactions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
@RequiredArgsConstructor
public class RecurringTransactionController {

 private final RecurringTransactionService service;
 private final JwtTokenProvider jwtTokenProvider;

 @GetMapping("/user/{userId}")
 public ResponseEntity<List<RecurringTransactionDTO>> getByUserId(
     @PathVariable Long userId,
     @RequestHeader(value = "Authorization", required = false) String authHeader) {
 try {
   // Extract userId from JWT token
   Long authenticatedUserId = extractUserIdFromToken(authHeader);
   if (authenticatedUserId == null) {
     return ResponseEntity.status(401).build();
   }
   // Security: Only allow users to access their own data
   if (!userId.equals(authenticatedUserId)) {
     return ResponseEntity.status(403).build();
   }
   List<RecurringTransactionDTO> result = service.findByUserId(userId);
   return ResponseEntity.ok(result);
 } catch (Exception e) {
   // Log error for debugging
   System.err.println("Error in getByUserId: " + e.getMessage());
   e.printStackTrace();
   throw e;
 }
 }

 @PostMapping
 public ResponseEntity<RecurringTransactionDTO> create(
     @RequestBody RecurringTransactionDTO dto,
     @RequestHeader(value = "Authorization", required = false) String authHeader) {
 // Extract userId from JWT token
 Long authenticatedUserId = extractUserIdFromToken(authHeader);
 if (authenticatedUserId == null) {
   return ResponseEntity.status(401).build();
 }
 // Set userId from authenticated user
 dto.setUserId(authenticatedUserId);
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

 /**
 * USER TRIGGERED: Execute a single recurring transaction NOW
 * User clicks "Execute Now" button for a specific recurring transaction
 */
 @PostMapping("/{id}/execute")
 public ResponseEntity<Map<String, Object>> executeNow(
     @PathVariable Long id,
     @RequestHeader(value = "Authorization", required = false) String authHeader) {
 
 Long authenticatedUserId = extractUserIdFromToken(authHeader);
 if (authenticatedUserId == null) {
   return ResponseEntity.status(401).build();
 }
 
 try {
   int created = service.executeRecurringTransaction(id);
   Map<String, Object> response = new HashMap<>();
   response.put("success", true);
   response.put("message", "✅ Transaction created successfully!");
   response.put("transactionsCreated", created);
   return ResponseEntity.ok(response);
 } catch (Exception e) {
   Map<String, Object> response = new HashMap<>();
   response.put("success", false);
   response.put("error", e.getMessage());
   return ResponseEntity.badRequest().body(response);
 }
 }

 /**
 * USER TRIGGERED: Execute ALL due recurring transactions for current user
 * User clicks "Execute All My Recurring Transactions" button
 */
 @PostMapping("/execute-all")
 public ResponseEntity<Map<String, Object>> executeAllDue(
     @RequestHeader(value = "Authorization", required = false) String authHeader) {
 
 Long authenticatedUserId = extractUserIdFromToken(authHeader);
 if (authenticatedUserId == null) {
   return ResponseEntity.status(401).build();
 }
 
 try {
   int created = service.executeAllDueRecurringTransactions(authenticatedUserId);
   
   Map<String, Object> response = new HashMap<>();
   response.put("success", true);
   response.put("message", String.format("✅ Executed %d recurring transaction(s)!", created));
   response.put("transactionsCreated", created);
   
   return ResponseEntity.ok(response);
 } catch (Exception e) {
   Map<String, Object> response = new HashMap<>();
   response.put("success", false);
   response.put("error", e.getMessage());
   return ResponseEntity.status(500).body(response);
 }
 }

 private Long extractUserIdFromToken(String authHeader) {
 if (authHeader == null || !authHeader.startsWith("Bearer ")) {
 return null;
 }
 String token = authHeader.substring(7);
 return jwtTokenProvider.getUserIdFromJWT(token);
 }
}

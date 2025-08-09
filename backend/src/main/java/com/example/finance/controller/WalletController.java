package com.example.finance.controller;

import com.example.finance.dto.WalletDTO;
import com.example.finance.security.CustomUserDetails;
import com.example.finance.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
@RequiredArgsConstructor
public class WalletController {

    private final WalletService service;

    @GetMapping
    public ResponseEntity<List<WalletDTO>> list() { 
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();

            List<WalletDTO> wallets = service.findAll(userId);
            System.out.println("Found " + wallets.size() + " wallets for user " + userId);
            return ResponseEntity.ok(wallets); 
        } catch (Exception e) {
            System.err.println("Error in WalletController.list(): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody WalletDTO dto) { 
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            // Set userId from authentication
            dto.setUserId(userId);
            
            WalletDTO savedWallet = service.save(dto);
            return ResponseEntity.ok(savedWallet); 
        } catch (Exception e) {
            System.err.println("Error creating wallet: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating wallet: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletDTO> get(@PathVariable("id") Long id) { 
        try {
            return ResponseEntity.ok(service.findById(id));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) { 
        try {
            service.deleteById(id); 
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error deleting wallet: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error deleting wallet: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<WalletDTO> update(@PathVariable("id") Long id, @RequestBody WalletDTO dto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            dto.setId(id);
            dto.setUserId(userId); // Set userId from authentication
            return ResponseEntity.ok(service.update(dto)); 
        } catch (Exception e) {
            System.err.println("Error updating wallet: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/update-balances")
    public ResponseEntity<?> updateAllBalances() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();

            service.updateAllWalletBalances(userId);
            return ResponseEntity.ok().body("All wallet balances updated successfully");
        } catch (Exception e) {
            System.err.println("Error updating wallet balances: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating wallet balances: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/update-balance")
    public ResponseEntity<?> updateBalance(@PathVariable("id") Long id) {
        try {
            service.updateWalletBalance(id);
            return ResponseEntity.ok().body("Wallet balance updated successfully");
        } catch (Exception e) {
            System.err.println("Error updating wallet balance: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating wallet balance: " + e.getMessage());
        }
    }
}

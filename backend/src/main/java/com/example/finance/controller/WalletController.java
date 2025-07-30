package com.example.finance.controller;

import com.example.finance.dto.WalletDTO;
import com.example.finance.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService service;

    @GetMapping
    public List<WalletDTO> list() { return service.findAll(); }

    @PostMapping
    public WalletDTO create(@RequestBody WalletDTO dto) { return service.save(dto); }

    @GetMapping("/{id}")
    public WalletDTO get(@PathVariable("id") Long id) { return service.findById(id);}

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) { service.deleteById(id); }

    @PutMapping("/{id}")
    public WalletDTO update(@PathVariable("id") Long id, @RequestBody WalletDTO dto) {
        dto.setId(id);
        return service.update(dto); 
    }
}

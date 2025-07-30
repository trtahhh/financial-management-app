package com.example.finance.service;

import com.example.finance.dto.WalletDTO;
import com.example.finance.mapper.WalletMapper;
import com.example.finance.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository repo;
    private final WalletMapper mapper;

    public List<WalletDTO> findAll() {
        return repo.findAll().stream().map(mapper::toDto).toList();
    }

    public WalletDTO save(WalletDTO dto) {
        return mapper.toDto(repo.save(mapper.toEntity(dto)));
    }

    public WalletDTO findById(Long id) {
        return repo.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + id));
    }

    public void deleteById(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Wallet not found with id: " + id);
        }
        repo.deleteById(id);
    }

    public WalletDTO update(WalletDTO dto) {
        if (!repo.existsById(dto.getId())) {
            throw new RuntimeException("Wallet not found with id: " + dto.getId());
        }
        return mapper.toDto(repo.save(mapper.toEntity(dto)));
    }
}

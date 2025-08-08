package com.example.finance.service;

import com.example.finance.dto.CategoryDTO;
import com.example.finance.exception.CustomException;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "categories")
public class CategoryService {

    private final CategoryRepository repo;
    private final CategoryMapper mapper;
    private static final String CATEGORY_NOT_FOUND = "Category not found with id: ";

    @Cacheable(cacheNames = "categories")
    public List<CategoryDTO> findAll() {
        return repo.findAll().stream().map(mapper::toDto).toList();
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    public CategoryDTO save(CategoryDTO dto) {
        return mapper.toDto(repo.save(mapper.toEntity(dto)));
    }

    @Cacheable
    public CategoryDTO findById(Long id) {
        return repo.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new CustomException(CATEGORY_NOT_FOUND + id));
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    public void deleteById(Long id) {
        if (!repo.existsById(id)) {
            throw new CustomException(CATEGORY_NOT_FOUND + id);
        }
        repo.deleteById(id);
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    public CategoryDTO update(CategoryDTO dto) {
        if (!repo.existsById(dto.getId()))
            throw new CustomException("Category not found with id: " + dto.getId());
        return mapper.toDto(repo.save(mapper.toEntity(dto)));
    }

    public boolean existsById(Long id) {
        return repo.existsById(id);
    }
}

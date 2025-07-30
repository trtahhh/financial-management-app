package com.example.finance.controller;

import com.example.finance.dto.CategoryDTO;
import com.example.finance.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public List<CategoryDTO> list() { return service.findAll(); }

    @PostMapping
    public CategoryDTO create(@RequestBody CategoryDTO dto) { return service.save(dto); }

    @GetMapping("/{id}")
    public CategoryDTO getById(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public CategoryDTO update(@PathVariable("id") Long id, @RequestBody CategoryDTO dto) {
        dto.setId(id);
        return service.update(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        service.deleteById(id);
    }
}

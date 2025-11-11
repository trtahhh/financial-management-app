package com.example.finance.service;

import com.example.finance.exception.CustomException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Base service class to reduce code duplication
 * @param <T> Entity type
 * @param <ID> ID type
 * @param <DTO> DTO type
 */
public abstract class BaseService<T, ID, DTO> {
 
 protected final JpaRepository<T, ID> repository;
 
 protected BaseService(JpaRepository<T, ID> repository) {
 this.repository = repository;
 }
 
 @Transactional(readOnly = true)
 public List<DTO> findAll() {
 return repository.findAll().stream()
 .map(this::toDto)
 .toList();
 }
 
 @Transactional(readOnly = true)
 public Optional<DTO> findById(ID id) {
 return repository.findById(id)
 .map(this::toDto);
 }
 
 @Transactional
 public DTO save(DTO dto) {
 T entity = toEntity(dto);
 T saved = repository.save(entity);
 return toDto(saved);
 }
 
 @Transactional
 public void deleteById(ID id) {
 if (!repository.existsById(id)) {
 throw new CustomException("Entity not found with id: " + id);
 }
 repository.deleteById(id);
 }
 
 public boolean existsById(ID id) {
 return repository.existsById(id);
 }
 
 // Abstract methods to be implemented by subclasses
 protected abstract DTO toDto(T entity);
 protected abstract T toEntity(DTO dto);
}

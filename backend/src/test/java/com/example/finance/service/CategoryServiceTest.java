package com.example.finance.service;

import com.example.finance.dto.CategoryDTO;
import com.example.finance.entity.Category;
import com.example.finance.exception.CustomException;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;
    private CategoryDTO testCategoryDTO;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");
        testCategory.setType("expense");
        testCategory.setIcon("food");
        testCategory.setColor("#FF5733");

        testCategoryDTO = new CategoryDTO();
        testCategoryDTO.setId(1L);
        testCategoryDTO.setName("Food");
        testCategoryDTO.setType("expense");
        testCategoryDTO.setIcon("food");
        testCategoryDTO.setColor("#FF5733");
    }

    @Test
    @DisplayName("Should find all categories successfully")
    void testFindAll_Success() {
        // Arrange
        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Transport");

        CategoryDTO categoryDTO2 = new CategoryDTO();
        categoryDTO2.setId(2L);
        categoryDTO2.setName("Transport");

        when(categoryRepository.findAll()).thenReturn(Arrays.asList(testCategory, category2));
        when(categoryMapper.toDto(testCategory)).thenReturn(testCategoryDTO);
        when(categoryMapper.toDto(category2)).thenReturn(categoryDTO2);

        // Act
        List<CategoryDTO> categories = categoryService.findAll();

        // Assert
        assertNotNull(categories);
        assertEquals(2, categories.size());
        assertEquals("Food", categories.get(0).getName());
        assertEquals("Transport", categories.get(1).getName());
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no categories exist")
    void testFindAll_Empty() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<CategoryDTO> categories = categoryService.findAll();

        // Assert
        assertNotNull(categories);
        assertTrue(categories.isEmpty());
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("Should save category successfully")
    void testSave_Success() {
        // Arrange
        when(categoryMapper.toEntity(testCategoryDTO)).thenReturn(testCategory);
        when(categoryRepository.save(testCategory)).thenReturn(testCategory);
        when(categoryMapper.toDto(testCategory)).thenReturn(testCategoryDTO);

        // Act
        CategoryDTO result = categoryService.save(testCategoryDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Food", result.getName());
        verify(categoryRepository).save(testCategory);
    }

    @Test
    @DisplayName("Should find category by id successfully")
    void testFindById_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryMapper.toDto(testCategory)).thenReturn(testCategoryDTO);

        // Act
        CategoryDTO result = categoryService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Food", result.getName());
        verify(categoryRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when category not found by id")
    void testFindById_NotFound() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            categoryService.findById(99L);
        });
        verify(categoryRepository).findById(99L);
    }

    @Test
    @DisplayName("Should delete category successfully")
    void testDeleteById_Success() {
        // Arrange
        when(categoryRepository.existsById(1L)).thenReturn(true);
        doNothing().when(categoryRepository).deleteById(1L);

        // Act & Assert
        assertDoesNotThrow(() -> {
            categoryService.deleteById(1L);
        });
        verify(categoryRepository).existsById(1L);
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent category")
    void testDeleteById_NotFound() {
        // Arrange
        when(categoryRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            categoryService.deleteById(99L);
        });
        verify(categoryRepository).existsById(99L);
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when deleting category with transactions")
    void testDeleteById_HasTransactions() {
        // Arrange
        when(categoryRepository.existsById(1L)).thenReturn(true);
        doThrow(new RuntimeException("constraint violation"))
                .when(categoryRepository).deleteById(1L);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categoryService.deleteById(1L);
        });
        assertTrue(exception.getMessage().contains("associated transactions"));
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should update category successfully")
    void testUpdate_Success() {
        // Arrange
        testCategoryDTO.setName("Updated Food");
        Category updatedCategory = new Category();
        updatedCategory.setId(1L);
        updatedCategory.setName("Updated Food");

        CategoryDTO updatedDTO = new CategoryDTO();
        updatedDTO.setId(1L);
        updatedDTO.setName("Updated Food");

        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(categoryMapper.toEntity(testCategoryDTO)).thenReturn(updatedCategory);
        when(categoryRepository.save(updatedCategory)).thenReturn(updatedCategory);
        when(categoryMapper.toDto(updatedCategory)).thenReturn(updatedDTO);

        // Act
        CategoryDTO result = categoryService.update(testCategoryDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Food", result.getName());
        verify(categoryRepository).existsById(1L);
        verify(categoryRepository).save(updatedCategory);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent category")
    void testUpdate_NotFound() {
        // Arrange
        testCategoryDTO.setId(99L);
        when(categoryRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            categoryService.update(testCategoryDTO);
        });
        verify(categoryRepository).existsById(99L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should check if category exists by id")
    void testExistsById_True() {
        // Arrange
        when(categoryRepository.existsById(1L)).thenReturn(true);

        // Act
        boolean exists = categoryService.existsById(1L);

        // Assert
        assertTrue(exists);
        verify(categoryRepository).existsById(1L);
    }

    @Test
    @DisplayName("Should return false when category does not exist")
    void testExistsById_False() {
        // Arrange
        when(categoryRepository.existsById(99L)).thenReturn(false);

        // Act
        boolean exists = categoryService.existsById(99L);

        // Assert
        assertFalse(exists);
        verify(categoryRepository).existsById(99L);
    }
}

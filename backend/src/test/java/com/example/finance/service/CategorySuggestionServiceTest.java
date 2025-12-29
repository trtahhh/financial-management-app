package com.example.finance.service;

import com.example.finance.dto.CategorySuggestionDTO;
import com.example.finance.entity.Category;
import com.example.finance.entity.CategorySuggestion;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.CategorySuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for CategorySuggestionService - Group 6
 * Tests AI-powered category suggestion functionality
 */
@ExtendWith(MockitoExtension.class)
public class CategorySuggestionServiceTest {

    @Mock
    private CategorySuggestionRepository suggestionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FuzzyMatchingService fuzzyMatchingService;

    @InjectMocks
    private CategorySuggestionService categorySuggestionService;

    private CategorySuggestion testSuggestion;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testSuggestion = new CategorySuggestion();
        testSuggestion.setId(1L);
        testSuggestion.setUserId(1L);
        testSuggestion.setSuggestedName("Thú cưng");
        testSuggestion.setSuggestedType("expense");
        testSuggestion.setSuggestedColor("#dc3545");
        testSuggestion.setSuggestedIcon("fa-paw");
        testSuggestion.setConfidenceScore(new BigDecimal("0.65"));
        testSuggestion.setReasoning("Test reasoning");
        testSuggestion.setSampleDescriptions("Mua thức ăn cho chó");
        testSuggestion.setTransactionCount(1);
        testSuggestion.setStatus("pending");
        testSuggestion.setCreatedAt(LocalDateTime.now());

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Ăn uống");
        testCategory.setType("expense");
        testCategory.setIsActive(true);
    }

    /**
     * Test analyzeAndSuggest - Success (New Suggestion)
     */
    @Test
    void testAnalyzeAndSuggest_NewSuggestion() {
        // Arrange
        String description = "Mua thức ăn cho chó";
        Long userId = 1L;
        String type = "expense";
        Long currentCategoryId = 14L; // "Khác" category

        lenient().when(fuzzyMatchingService.fullNormalize(description)).thenReturn("mua thuc an cho cho");
        lenient().when(fuzzyMatchingService.calculateSimilarity(anyString(), anyString())).thenReturn(0.3); // No similar category
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(suggestionRepository.findPendingSuggestionByName(userId, "Thú cưng")).thenReturn(Optional.empty());
        when(suggestionRepository.save(any(CategorySuggestion.class))).thenReturn(testSuggestion);

        // Act
        Optional<CategorySuggestion> result = categorySuggestionService.analyzeAndSuggest(
            description, userId, type, currentCategoryId
        );

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Thú cưng", result.get().getSuggestedName());
        verify(suggestionRepository, times(1)).save(any(CategorySuggestion.class));
    }

    /**
     * Test analyzeAndSuggest - Not "Khác" Category (Skip)
     */
    @Test
    void testAnalyzeAndSuggest_NotOtherCategory() {
        // Arrange
        String description = "Mua cà phê";
        Long userId = 1L;
        String type = "expense";
        Long currentCategoryId = 5L; // "Ăn uống" category, not "Khác"

        // Act
        Optional<CategorySuggestion> result = categorySuggestionService.analyzeAndSuggest(
            description, userId, type, currentCategoryId
        );

        // Assert
        assertTrue(result.isEmpty());
        verify(suggestionRepository, never()).save(any());
    }

    /**
     * Test analyzeAndSuggest - Update Existing Suggestion
     */
    @Test
    void testAnalyzeAndSuggest_UpdateExisting() {
        // Arrange
        String description = "Tiêm phòng cho mèo";
        Long userId = 1L;
        String type = "expense";
        Long currentCategoryId = 14L;

        lenient().when(fuzzyMatchingService.fullNormalize(description)).thenReturn("tiem phong cho meo");
        lenient().when(fuzzyMatchingService.calculateSimilarity(anyString(), anyString())).thenReturn(0.3);
        lenient().when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(suggestionRepository.findPendingSuggestionByName(userId, "Thú cưng"))
            .thenReturn(Optional.of(testSuggestion));
        when(suggestionRepository.save(any(CategorySuggestion.class))).thenReturn(testSuggestion);

        // Act
        Optional<CategorySuggestion> result = categorySuggestionService.analyzeAndSuggest(
            description, userId, type, currentCategoryId
        );

        // Assert
        assertTrue(result.isPresent());
        verify(suggestionRepository, times(1)).save(any(CategorySuggestion.class));
        // Confidence should be boosted by 0.05
    }

    /**
     * Test analyzeAndSuggest - Similar Category Exists (Skip)
     */
    @Test
    void testAnalyzeAndSuggest_SimilarCategoryExists() {
        // Arrange
        String description = "Spa cho chó";
        Long userId = 1L;
        String type = "expense";
        Long currentCategoryId = 14L;

        Category existingCategory = new Category();
        existingCategory.setId(20L);
        existingCategory.setName("Thú cưng");
        existingCategory.setType("expense");

        when(fuzzyMatchingService.fullNormalize(anyString())).thenReturn("thu cung");
        when(fuzzyMatchingService.calculateSimilarity("thu cung", "thu cung")).thenReturn(1.0); // Perfect match
        when(categoryRepository.findAll()).thenReturn(List.of(existingCategory));

        // Act
        Optional<CategorySuggestion> result = categorySuggestionService.analyzeAndSuggest(
            description, userId, type, currentCategoryId
        );

        // Assert
        assertTrue(result.isEmpty());
        verify(suggestionRepository, never()).save(any());
    }

    /**
     * Test getPendingSuggestions - Success
     */
    @Test
    void testGetPendingSuggestions_Success() {
        // Arrange
        Long userId = 1L;
        List<CategorySuggestion> suggestions = Arrays.asList(testSuggestion);

        when(suggestionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "pending"))
            .thenReturn(suggestions);

        // Act
        List<CategorySuggestionDTO> result = categorySuggestionService.getPendingSuggestions(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Thú cưng", result.get(0).getSuggestedName());
        verify(suggestionRepository, times(1)).findByUserIdAndStatusOrderByCreatedAtDesc(userId, "pending");
    }

    /**
     * Test getPendingSuggestions - Empty List
     */
    @Test
    void testGetPendingSuggestions_EmptyList() {
        // Arrange
        Long userId = 1L;
        when(suggestionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "pending"))
            .thenReturn(Collections.emptyList());

        // Act
        List<CategorySuggestionDTO> result = categorySuggestionService.getPendingSuggestions(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test approveSuggestion - Success
     */
    @Test
    void testApproveSuggestion_Success() {
        // Arrange
        Long suggestionId = 1L;
        Long userId = 1L;

        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.of(testSuggestion));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
        when(suggestionRepository.save(any(CategorySuggestion.class))).thenReturn(testSuggestion);

        // Act
        Category result = categorySuggestionService.approveSuggestion(suggestionId, userId);

        // Assert
        assertNotNull(result);
        assertEquals("Ăn uống", result.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
        verify(suggestionRepository, times(1)).save(any(CategorySuggestion.class));
    }

    /**
     * Test approveSuggestion - Not Found
     */
    @Test
    void testApproveSuggestion_NotFound() {
        // Arrange
        Long suggestionId = 999L;
        Long userId = 1L;

        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categorySuggestionService.approveSuggestion(suggestionId, userId);
        });

        assertEquals("Suggestion not found", exception.getMessage());
    }

    /**
     * Test approveSuggestion - Unauthorized
     */
    @Test
    void testApproveSuggestion_Unauthorized() {
        // Arrange
        Long suggestionId = 1L;
        Long userId = 2L; // Different user

        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.of(testSuggestion));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categorySuggestionService.approveSuggestion(suggestionId, userId);
        });

        assertEquals("Unauthorized", exception.getMessage());
    }

    /**
     * Test approveSuggestion - Already Processed
     */
    @Test
    void testApproveSuggestion_AlreadyProcessed() {
        // Arrange
        Long suggestionId = 1L;
        Long userId = 1L;

        testSuggestion.setStatus("approved"); // Already approved

        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.of(testSuggestion));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categorySuggestionService.approveSuggestion(suggestionId, userId);
        });

        assertEquals("Suggestion already processed", exception.getMessage());
    }

    /**
     * Test rejectSuggestion - Success
     */
    @Test
    void testRejectSuggestion_Success() {
        // Arrange
        Long suggestionId = 1L;
        Long userId = 1L;
        String reason = "Not needed";

        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.of(testSuggestion));
        when(suggestionRepository.save(any(CategorySuggestion.class))).thenReturn(testSuggestion);

        // Act
        categorySuggestionService.rejectSuggestion(suggestionId, userId, reason);

        // Assert
        verify(suggestionRepository, times(1)).save(any(CategorySuggestion.class));
    }

    /**
     * Test mergeSuggestion - Success
     */
    @Test
    void testMergeSuggestion_Success() {
        // Arrange
        Long suggestionId = 1L;
        Long userId = 1L;
        Long existingCategoryId = 5L;

        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.of(testSuggestion));
        when(suggestionRepository.save(any(CategorySuggestion.class))).thenReturn(testSuggestion);

        // Act
        categorySuggestionService.mergeSuggestion(suggestionId, userId, existingCategoryId);

        // Assert
        verify(suggestionRepository, times(1)).save(any(CategorySuggestion.class));
    }

    /**
     * Test analyzeAndSuggest - Beauty Pattern
     */
    @Test
    void testAnalyzeAndSuggest_BeautyPattern() {
        // Arrange
        String description = "Cắt tóc tại salon";
        Long userId = 1L;
        String type = "expense";
        Long currentCategoryId = 14L;

        lenient().when(fuzzyMatchingService.fullNormalize(description)).thenReturn("cat toc tai salon");
        lenient().when(fuzzyMatchingService.calculateSimilarity(anyString(), anyString())).thenReturn(0.3);
        lenient().when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(suggestionRepository.findPendingSuggestionByName(anyLong(), anyString())).thenReturn(Optional.empty());

        testSuggestion.setSuggestedName("Làm đẹp");
        when(suggestionRepository.save(any(CategorySuggestion.class))).thenReturn(testSuggestion);

        // Act
        Optional<CategorySuggestion> result = categorySuggestionService.analyzeAndSuggest(
            description, userId, type, currentCategoryId
        );

        // Assert
        assertTrue(result.isPresent());
    }

    /**
     * Test analyzeAndSuggest - No Pattern Match
     */
    @Test
    void testAnalyzeAndSuggest_NoPatternMatch() {
        // Arrange
        String description = "Random expense xyz";
        Long userId = 1L;
        String type = "expense";
        Long currentCategoryId = 14L;

        when(fuzzyMatchingService.fullNormalize(description)).thenReturn("random expense xyz");

        // Act
        Optional<CategorySuggestion> result = categorySuggestionService.analyzeAndSuggest(
            description, userId, type, currentCategoryId
        );

        // Assert
        assertTrue(result.isEmpty());
        verify(suggestionRepository, never()).save(any());
    }
}

package com.example.finance.service;

import com.example.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AICategorizationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserCategorizationPreferenceRepository userPreferenceRepository;

    @Mock
    private OpenRouterService openRouterService;

    @Mock
    private FuzzyMatchingService fuzzyMatchingService;

    @Mock
    private CategorySuggestionService categorySuggestionService;

    @InjectMocks
    private AICategorizationService aiCategorizationService;

    private static final Long USER_ID = 1L;
    private static final String DESCRIPTION_FOOD = "Mua com ga KFC";
    private static final String DESCRIPTION_TRANSPORT = "Grab di lam";
    private static final Double AMOUNT = 100000.0;

    @BeforeEach
    void setUp() {
        // Service initialization happens in @PostConstruct
    }

    @Test
    @DisplayName("Should return CategorizationResult with category ID")
    void testCategorizeExpense_ReturnsResult() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(DESCRIPTION_FOOD, AMOUNT, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
        assertTrue(result.getCategory() >= 1L && result.getCategory() <= 14L);
    }

    @Test
    @DisplayName("Should categorize food description")
    void testCategorizeExpense_FoodCategory() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(DESCRIPTION_FOOD, AMOUNT, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
        // Category should be between 1-14
        assertTrue(result.getCategory() >= 1L && result.getCategory() <= 14L);
    }

    @Test
    @DisplayName("Should categorize transport description")
    void testCategorizeExpense_TransportCategory() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(DESCRIPTION_TRANSPORT, AMOUNT, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
        assertTrue(result.getCategory() >= 1L && result.getCategory() <= 14L);
    }

    @Test
    @DisplayName("Should categorize without userId")
    void testCategorizeExpense_WithoutUserId() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(DESCRIPTION_FOOD, AMOUNT);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
        assertTrue(result.getCategory() >= 1L && result.getCategory() <= 14L);
    }

    @Test
    @DisplayName("Should handle minimal description")
    void testCategorizeExpense_MinimalDescription() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense("test", AMOUNT, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
        // Should return a valid category
        assertTrue(result.getCategory() >= 1L && result.getCategory() <= 14L);
    }

    @Test
    @DisplayName("Should handle null amount")
    void testCategorizeExpense_NullAmount() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(DESCRIPTION_FOOD, null, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
    }

    @Test
    @DisplayName("Should return result with confidence value")
    void testCategorizeExpense_HasConfidence() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(DESCRIPTION_FOOD, AMOUNT, USER_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0);
    }

    @Test
    @DisplayName("Should return result with category name")
    void testCategorizeExpense_HasCategoryName() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(DESCRIPTION_FOOD, AMOUNT, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategoryName());
        assertFalse(result.getCategoryName().isEmpty());
    }

    @Test
    @DisplayName("Should categorize Vietnamese text with diacritics")
    void testCategorizeExpense_VietnameseNormalization() {
        // Arrange - Description with diacritics
        String descriptionWithDiacritics = "Mua cơm gà KFC";

        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(descriptionWithDiacritics, AMOUNT, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
        assertTrue(result.getCategory() >= 1L && result.getCategory() <= 14L);
    }

    @Test
    @DisplayName("Should handle various expense descriptions")
    void testCategorizeExpense_VariousDescriptions() {
        // Test multiple categories
        String[] descriptions = {
            "Nhan luong thang 1",      // Salary
            "Xe buyt",                  // Transport
            "Xem phim",                 // Entertainment
            "Kham benh",                // Healthcare
            "Hoc phi",                  // Education
            "Mua sam",                  // Shopping
            "Tien dien",                // Utilities
            "Qua tang"                  // Gifts
        };

        for (String description : descriptions) {
            AICategorizationService.CategorizationResult result = 
                aiCategorizationService.categorizeExpense(description, AMOUNT, USER_ID);

            assertNotNull(result, "Result should not be null for: " + description);
            assertNotNull(result.getCategory(), "Category should not be null for: " + description);
            assertTrue(result.getCategory() >= 1L && result.getCategory() <= 14L, 
                "Category ID should be between 1 and 14 for: " + description);
        }
    }

    @Test
    @DisplayName("Should handle special characters in description")
    void testCategorizeExpense_SpecialCharacters() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense("Mua @#$%^&* 123", AMOUNT, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
    }

    @Test
    @DisplayName("Should return category key for icon mapping")
    void testCategorizeExpense_HasCategoryKey() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(DESCRIPTION_FOOD, AMOUNT, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategoryKey());
    }

    @Test
    @DisplayName("Should handle very long description")
    void testCategorizeExpense_LongDescription() {
        // Arrange
        String longDescription = "Mua com ga KFC ".repeat(20);

        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(longDescription, AMOUNT, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
    }

    @Test
    @DisplayName("Should categorize with large amount")
    void testCategorizeExpense_LargeAmount() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(DESCRIPTION_FOOD, 10000000.0, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
    }

    @Test
    @DisplayName("Should categorize with small amount")
    void testCategorizeExpense_SmallAmount() {
        // Act
        AICategorizationService.CategorizationResult result = 
            aiCategorizationService.categorizeExpense(DESCRIPTION_FOOD, 1000.0, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategory());
    }
}

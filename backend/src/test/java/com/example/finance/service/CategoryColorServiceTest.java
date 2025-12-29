package com.example.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for CategoryColorService
 * Simple service that maps category names to color codes
 */
@ExtendWith(MockitoExtension.class)
public class CategoryColorServiceTest {

    private CategoryColorService categoryColorService;

    @BeforeEach
    public void setUp() {
        categoryColorService = new CategoryColorService();
        // Reset backup color index before each test
        categoryColorService.resetBackupColorIndex();
    }

    @Test
    public void testGetCategoryColor_ExactMatch() {
        // When: Get color for exact category name
        String color = categoryColorService.getCategoryColor("Lương");
        
        // Then: Returns predefined color
        assertEquals("#27AE60", color);
    }

    @Test
    public void testGetCategoryColor_CaseInsensitiveMatch() {
        // When: Get color with different case
        String color1 = categoryColorService.getCategoryColor("lương");
        String color2 = categoryColorService.getCategoryColor("LƯƠNG");
        String color3 = categoryColorService.getCategoryColor("LưƠng");
        
        // Then: All return same predefined color (case-insensitive)
        assertEquals("#27AE60", color1);
        assertEquals("#27AE60", color2);
        assertEquals("#27AE60", color3);
    }

    @Test
    public void testGetCategoryColor_ExpenseCategory() {
        // When: Get colors for expense categories
        String foodColor = categoryColorService.getCategoryColor("Ăn uống");
        String transportColor = categoryColorService.getCategoryColor("Giao thông");
        String entertainmentColor = categoryColorService.getCategoryColor("Giải trí");
        
        // Then: Returns predefined colors
        assertEquals("#FF6B6B", foodColor);
        assertEquals("#4ECDC4", transportColor);
        assertEquals("#45B7D1", entertainmentColor);
    }

    @Test
    public void testGetCategoryColor_UnknownCategory() {
        // When: Get color for unknown category
        String color1 = categoryColorService.getCategoryColor("Unknown Category");
        String color2 = categoryColorService.getCategoryColor("Another Unknown");
        
        // Then: Returns backup colors
        assertNotNull(color1);
        assertNotNull(color2);
        assertTrue(color1.startsWith("#"));
        assertTrue(color2.startsWith("#"));
        // Second call should get different backup color
        assertNotEquals(color1, color2);
    }

    @Test
    public void testGetCategoryColor_NullInput() {
        // When: Get color for null category
        String color = categoryColorService.getCategoryColor(null);
        
        // Then: Returns backup color
        assertNotNull(color);
        assertTrue(color.startsWith("#"));
    }

    @Test
    public void testGetCategoryColor_EmptyString() {
        // When: Get color for empty category
        String color = categoryColorService.getCategoryColor("");
        
        // Then: Returns backup color
        assertNotNull(color);
        assertTrue(color.startsWith("#"));
    }

    @Test
    public void testGetAllCategoryColors() {
        // When: Get all predefined colors
        Map<String, String> colors = categoryColorService.getAllCategoryColors();
        
        // Then: Returns map with all predefined categories
        assertNotNull(colors);
        assertFalse(colors.isEmpty());
        assertTrue(colors.containsKey("Lương"));
        assertTrue(colors.containsKey("Ăn uống"));
        assertTrue(colors.containsKey("Giao thông"));
        assertEquals("#27AE60", colors.get("Lương"));
        assertEquals("#FF6B6B", colors.get("Ăn uống"));
    }

    @Test
    public void testAddCategoryColor() {
        // Given: Add new category color
        categoryColorService.addCategoryColor("Custom Category", "#ABCDEF");
        
        // When: Get color for new category
        String color = categoryColorService.getCategoryColor("Custom Category");
        
        // Then: Returns newly added color
        assertEquals("#ABCDEF", color);
    }

    @Test
    public void testAddCategoryColor_NullValues() {
        // Given: Get initial color count
        int initialSize = categoryColorService.getAllCategoryColors().size();
        
        // When: Try to add null values
        categoryColorService.addCategoryColor(null, "#ABCDEF");
        categoryColorService.addCategoryColor("Test", null);
        categoryColorService.addCategoryColor(null, null);
        
        // Then: Map size unchanged (null values not added)
        int finalSize = categoryColorService.getAllCategoryColors().size();
        assertEquals(initialSize, finalSize);
    }

    @Test
    public void testResetBackupColorIndex() {
        // Given: Get several backup colors
        String color1 = categoryColorService.getCategoryColor("Unknown1");
        String color2 = categoryColorService.getCategoryColor("Unknown2");
        categoryColorService.getCategoryColor("Unknown3");
        
        // When: Reset index
        categoryColorService.resetBackupColorIndex();
        String colorAfterReset1 = categoryColorService.getCategoryColor("Unknown4");
        String colorAfterReset2 = categoryColorService.getCategoryColor("Unknown5");
        
        // Then: Backup colors restart from beginning
        assertEquals(color1, colorAfterReset1);
        assertEquals(color2, colorAfterReset2);
    }

    @Test
    public void testBackupColorRotation() {
        // Given: Create new service instance to have clean state
        CategoryColorService freshService = new CategoryColorService();
        
        // When: Get more colors than available in backup array (20 colors)
        String color1 = freshService.getCategoryColor("Unknown1");
        
        // Get 19 more colors to fill the array (total 20 unique colors)
        for (int i = 2; i <= 20; i++) {
            freshService.getCategoryColor("Unknown" + i);
        }
        
        String color21 = freshService.getCategoryColor("Unknown21");
        
        // Then: Color 21 should be same as color 1 (rotation starts over)
        assertEquals(color1, color21);
    }

    @Test
    public void testIncomeCategories() {
        // When: Get colors for income categories
        String salaryColor = categoryColorService.getCategoryColor("Lương");
        String otherIncomeColor = categoryColorService.getCategoryColor("Thu nhập khác");
        String investmentColor = categoryColorService.getCategoryColor("Đầu tư");
        String businessColor = categoryColorService.getCategoryColor("Kinh doanh");
        
        // Then: All return predefined income colors (green/blue shades)
        assertEquals("#27AE60", salaryColor); // Green
        assertEquals("#2980B9", otherIncomeColor); // Blue
        assertEquals("#8E44AD", investmentColor); // Purple
        assertEquals("#E67E22", businessColor); // Orange
    }

    @Test
    public void testAllPredefinedCategories() {
        // When: Test all predefined categories
        String[] categoryNames = {
            "Lương", "Thu nhập khác", "Đầu tư", "Kinh doanh",
            "Ăn uống", "Giao thông", "Giải trí", "Sức khỏe",
            "Giáo dục", "Mua sắm", "Tiện ích", "Du lịch",
            "Thể thao", "Khác"
        };
        
        // Then: All categories have predefined colors
        for (String categoryName : categoryNames) {
            String color = categoryColorService.getCategoryColor(categoryName);
            assertNotNull(color);
            assertTrue(color.startsWith("#"));
            assertEquals(7, color.length()); // #RRGGBB format
        }
    }
}

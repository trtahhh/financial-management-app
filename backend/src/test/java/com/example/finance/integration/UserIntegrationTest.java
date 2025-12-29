package com.example.finance.integration;

import com.example.finance.entity.User;
import com.example.finance.entity.UserProfile;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.UserProfileRepository;
import com.example.finance.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for User functionality
 * Tests the full stack from service to database
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean database
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("integrationtest");
        testUser.setEmail("integration@test.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setRole("USER");
        testUser.setIsActive(true);
    }

    @Test
    void testCreateUser_Success() {
        // Act
        User savedUser = userService.save(testUser);

        // Assert
        assertNotNull(savedUser.getId());
        assertEquals("integrationtest", savedUser.getUsername());
        assertTrue(userService.existsByUsername("integrationtest"));
    }

    @Test
    void testFindUserByUsername_Success() {
        // Arrange
        userService.save(testUser);

        // Act
        User foundUser = userService.findByUsername("integrationtest");

        // Assert
        assertNotNull(foundUser);
        assertEquals("integrationtest", foundUser.getUsername());
        assertEquals("integration@test.com", foundUser.getEmail());
    }

    @Test
    void testCreateUserProfile_Success() {
        // Arrange
        User savedUser = userService.save(testUser);

        Map<String, Object> profileData = Map.of(
            "fullName", "Integration Test User",
            "phone", "0123456789",
            "gender", "Male"
        );

        // Act
        UserProfile profile = userService.saveProfile(savedUser.getId(), profileData);

        // Assert
        assertNotNull(profile);
        assertEquals("Integration Test User", profile.getFullName());
        assertEquals("0123456789", profile.getPhone());
        assertEquals("Male", profile.getGender());
    }

    @Test
    void testUpdateUser_Success() {
        // Arrange
        User savedUser = userService.save(testUser);
        savedUser.setEmail("updated@test.com");

        // Act
        User updatedUser = userService.updateUser(savedUser);

        // Assert
        assertEquals("updated@test.com", updatedUser.getEmail());
        
        User foundUser = userService.findByUsername("integrationtest");
        assertEquals("updated@test.com", foundUser.getEmail());
    }

    @Test
    void testDeleteUser_Success() {
        // Arrange
        User savedUser = userService.save(testUser);
        Long userId = savedUser.getId();

        // Act
        userService.deleteUser(userId);

        // Assert
        Optional<User> deletedUser = userService.findById(userId);
        assertFalse(deletedUser.isPresent());
    }

    @Test
    void testExistsByEmail_Success() {
        // Arrange
        userService.save(testUser);

        // Act & Assert
        assertTrue(userService.existsByEmail("integration@test.com"));
        assertFalse(userService.existsByEmail("notexist@test.com"));
    }

    @Test
    void testSaveProfile_UpdateExisting() {
        // Arrange
        User savedUser = userService.save(testUser);
        
        Map<String, Object> initialProfile = Map.of(
            "fullName", "Initial Name",
            "phone", "111111111"
        );
        userService.saveProfile(savedUser.getId(), initialProfile);

        // Act - Update profile
        Map<String, Object> updatedProfile = Map.of(
            "fullName", "Updated Name",
            "phone", "222222222",
            "address", "New Address"
        );
        UserProfile profile = userService.saveProfile(savedUser.getId(), updatedProfile);

        // Assert
        assertEquals("Updated Name", profile.getFullName());
        assertEquals("222222222", profile.getPhone());
        assertEquals("New Address", profile.getAddress());
    }
}

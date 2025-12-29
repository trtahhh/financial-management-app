package com.example.finance.service;

import com.example.finance.entity.User;
import com.example.finance.repository.UserRepository;
import com.example.finance.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomUserDetailsService - Group 6
 * Tests Spring Security UserDetailsService implementation
 */
@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    /**
     * Test loadUserByUsername - Success
     */
    @Test
    void testLoadUserByUsername_Success() {
        // Arrange
        String username = "testuser";
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash("$2a$10$hashedPassword");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails instanceof CustomUserDetails);
        assertEquals(username, userDetails.getUsername());
        assertEquals("$2a$10$hashedPassword", userDetails.getPassword());
        
        CustomUserDetails customDetails = (CustomUserDetails) userDetails;
        assertEquals(1L, customDetails.getId());
        assertEquals(1L, customDetails.getUserId());

        verify(userRepository, times(1)).findByUsername(username);
    }

    /**
     * Test loadUserByUsername - User Not Found
     */
    @Test
    void testLoadUserByUsername_UserNotFound() {
        // Arrange
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(username);
        });

        assertEquals("User not found with username: nonexistent", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(username);
    }

    /**
     * Test loadUserByUsername - Null Username
     */
    @Test
    void testLoadUserByUsername_NullUsername() {
        // Arrange
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(null);
        });

        verify(userRepository, times(1)).findByUsername(null);
    }

    /**
     * Test loadUserByUsername - Empty Username
     */
    @Test
    void testLoadUserByUsername_EmptyUsername() {
        // Arrange
        String username = "";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(username);
        });

        verify(userRepository, times(1)).findByUsername(username);
    }

    /**
     * Test loadUserByUsername - Multiple Users with Same Username (Edge Case)
     */
    @Test
    void testLoadUserByUsername_ReturnsFirstMatch() {
        // Arrange
        String username = "testuser";
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername(username);
        user1.setPasswordHash("password1");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user1));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Assert
        assertNotNull(userDetails);
        CustomUserDetails customDetails = (CustomUserDetails) userDetails;
        assertEquals(1L, customDetails.getId());
        assertEquals("password1", userDetails.getPassword());
    }

    /**
     * Test loadUserByUsername - Repository Throws Exception
     */
    @Test
    void testLoadUserByUsername_RepositoryException() {
        // Arrange
        String username = "testuser";
        when(userRepository.findByUsername(username))
            .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customUserDetailsService.loadUserByUsername(username);
        });

        assertEquals("Database connection error", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(username);
    }

    /**
     * Test CustomUserDetails - Authorities
     */
    @Test
    void testCustomUserDetails_Authorities() {
        // Arrange
        String username = "testuser";
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash("hashedPassword");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Assert
        assertNotNull(userDetails.getAuthorities());
        assertTrue(userDetails.getAuthorities().isEmpty()); // No authorities set
    }

    /**
     * Test CustomUserDetails - Account Status
     */
    @Test
    void testCustomUserDetails_AccountStatus() {
        // Arrange
        String username = "testuser";
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash("hashedPassword");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Assert
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
    }
}

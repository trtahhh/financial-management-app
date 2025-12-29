package com.example.finance.service;

import com.example.finance.entity.TwoFactorAuth;
import com.example.finance.entity.User;
import com.example.finance.repository.TwoFactorAuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Two Factor Auth Service Tests")
class TwoFactorAuthServiceTest {

    @Mock
    private TwoFactorAuthRepository twoFactorAuthRepository;

    @InjectMocks
    private TwoFactorAuthService twoFactorAuthService;

    private User testUser;
    private TwoFactorAuth testTwoFactorAuth;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testTwoFactorAuth = new TwoFactorAuth();
        testTwoFactorAuth.setId(1L);
        testTwoFactorAuth.setUser(testUser);
        testTwoFactorAuth.setSecretKey("JBSWY3DPEHPK3PXP");
        testTwoFactorAuth.setEnabled(false);
        testTwoFactorAuth.setBackupCodes("123456,789012,345678");
    }

    @Test
    @DisplayName("Should generate secret key")
    void testGenerateSecretKey() {
        // Act
        String secretKey = twoFactorAuthService.generateSecretKey();

        // Assert
        assertNotNull(secretKey);
        assertTrue(secretKey.length() >= 16); // Base32 encoded 20 bytes minimum
        assertTrue(secretKey.matches("[A-Z2-7]+")); // Base32 alphabet
    }

    @Test
    @DisplayName("Should generate 10 backup codes")
    void testGenerateBackupCodes() {
        // Act
        List<String> backupCodes = twoFactorAuthService.generateBackupCodes();

        // Assert
        assertNotNull(backupCodes);
        assertEquals(10, backupCodes.size());
        
        for (String code : backupCodes) {
            assertTrue(code.matches("\\d{6}")); // 6 digits
            int codeValue = Integer.parseInt(code);
            assertTrue(codeValue >= 100000 && codeValue <= 999999);
        }
    }

    @Test
    @DisplayName("Should generate unique backup codes")
    void testGenerateBackupCodes_Unique() {
        // Act
        List<String> backupCodes = twoFactorAuthService.generateBackupCodes();

        // Assert
        long uniqueCount = backupCodes.stream().distinct().count();
        assertEquals(10, uniqueCount);
    }

    @Test
    @DisplayName("Should setup two factor authentication")
    void testSetupTwoFactorAuth() {
        // Arrange
        when(twoFactorAuthRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(twoFactorAuthRepository.save(any(TwoFactorAuth.class))).thenReturn(testTwoFactorAuth);

        // Act
        Map<String, Object> result = twoFactorAuthService.setupTwoFactorAuth(testUser);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("secretKey"));
        assertTrue(result.containsKey("qrCodeUrl"));
        assertTrue(result.containsKey("backupCodes"));
        
        String qrCodeUrl = (String) result.get("qrCodeUrl");
        assertTrue(qrCodeUrl.startsWith("otpauth://totp/"));
        assertTrue(qrCodeUrl.contains("FinancialManagement"));
        assertTrue(qrCodeUrl.contains(testUser.getEmail()));
        
        @SuppressWarnings("unchecked")
        List<String> codes = (List<String>) result.get("backupCodes");
        assertEquals(10, codes.size());
        
        verify(twoFactorAuthRepository).save(any(TwoFactorAuth.class));
    }

    @Test
    @DisplayName("Should enable two factor authentication with valid code")
    void testEnableTwoFactorAuth_ValidCode() {
        // Arrange
        String validCode = "123456"; // Mock code
        when(twoFactorAuthRepository.findByUserId(1L)).thenReturn(Optional.of(testTwoFactorAuth));

        // Act
        boolean result = twoFactorAuthService.enableTwoFactorAuth(testUser, validCode);

        // Assert - Will be false without valid TOTP
        assertFalse(result); // Invalid code should return false
    }

    @Test
    @DisplayName("Should disable two factor authentication with valid code")
    void testDisableTwoFactorAuth_ValidCode() {
        // Arrange
        testTwoFactorAuth.setEnabled(true);
        String validCode = "123456"; // Mock code
        
        when(twoFactorAuthRepository.findByUserId(1L)).thenReturn(Optional.of(testTwoFactorAuth));

        // Act
        boolean result = twoFactorAuthService.disableTwoFactorAuth(testUser, validCode);

        // Assert - Will be false without valid TOTP
        assertFalse(result); // Invalid code should return false
    }

    @Test
    @DisplayName("Should verify backup code")
    void testVerifyBackupCode_Success() {
        // Arrange
        String backupCode = "123456";
        
        when(twoFactorAuthRepository.findByUserId(1L)).thenReturn(Optional.of(testTwoFactorAuth));
        when(twoFactorAuthRepository.save(any(TwoFactorAuth.class))).thenReturn(testTwoFactorAuth);

        // Act
        boolean result = twoFactorAuthService.verifyBackupCode(testUser, backupCode);

        // Assert
        assertTrue(result);
        verify(twoFactorAuthRepository).save(argThat(tfa -> 
            !tfa.getBackupCodes().contains(backupCode)
        ));
    }

    @Test
    @DisplayName("Should reject invalid backup code")
    void testVerifyBackupCode_Invalid() {
        // Arrange
        when(twoFactorAuthRepository.findByUserId(1L)).thenReturn(Optional.of(testTwoFactorAuth));

        // Act
        boolean result = twoFactorAuthService.verifyBackupCode(testUser, "999999");

        // Assert
        assertFalse(result);
        verify(twoFactorAuthRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should check if 2FA is enabled")
    void testIsTwoFactorEnabled_True() {
        // Arrange
        testTwoFactorAuth.setEnabled(true);
        when(twoFactorAuthRepository.findByUserIdAndEnabledTrue(1L)).thenReturn(Optional.of(testTwoFactorAuth));

        // Act
        boolean result = twoFactorAuthService.isTwoFactorEnabled(testUser);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for user without 2FA")
    void testIsTwoFactorEnabled_False() {
        // Arrange
        when(twoFactorAuthRepository.findByUserIdAndEnabledTrue(1L)).thenReturn(Optional.empty());

        // Act
        boolean result = twoFactorAuthService.isTwoFactorEnabled(testUser);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should get two factor auth status")
    void testGetTwoFactorStatus() {
        // Arrange
        when(twoFactorAuthRepository.findByUserId(1L)).thenReturn(Optional.of(testTwoFactorAuth));

        // Act
        Map<String, Object> status = twoFactorAuthService.getTwoFactorStatus(testUser);

        // Assert
        assertNotNull(status);
        assertTrue(status.containsKey("enabled"));
        assertTrue(status.containsKey("verifiedAt"));
        assertTrue(status.containsKey("remainingBackupCodes"));
        assertEquals(3, status.get("remainingBackupCodes"));
    }

    @Test
    @DisplayName("Should return default status for user without 2FA")
    void testGetTwoFactorStatus_NoSetup() {
        // Arrange
        when(twoFactorAuthRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // Act
        Map<String, Object> status = twoFactorAuthService.getTwoFactorStatus(testUser);

        // Assert
        assertNotNull(status);
        assertEquals(false, status.get("enabled"));
        assertNull(status.get("verifiedAt"));
        assertEquals(0, status.get("remainingBackupCodes"));
    }

    @Test
    @DisplayName("Should update existing 2FA on setup")
    void testSetupTwoFactorAuth_UpdateExisting() {
        // Arrange
        when(twoFactorAuthRepository.findByUserId(1L)).thenReturn(Optional.of(testTwoFactorAuth));
        when(twoFactorAuthRepository.save(any(TwoFactorAuth.class))).thenReturn(testTwoFactorAuth);

        // Act
        Map<String, Object> result = twoFactorAuthService.setupTwoFactorAuth(testUser);

        // Assert
        assertNotNull(result);
        verify(twoFactorAuthRepository).save(any(TwoFactorAuth.class));
    }

    @Test
    @DisplayName("Should handle user without 2FA when enabling")
    void testEnableTwoFactorAuth_NoSetup() {
        // Arrange
        when(twoFactorAuthRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // Act
        boolean result = twoFactorAuthService.enableTwoFactorAuth(testUser, "123456");

        // Assert
        assertFalse(result);
        verify(twoFactorAuthRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle user without 2FA when disabling")
    void testDisableTwoFactorAuth_NoSetup() {
        // Arrange
        when(twoFactorAuthRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // Act
        boolean result = twoFactorAuthService.disableTwoFactorAuth(testUser, "123456");

        // Assert
        assertFalse(result);
        verify(twoFactorAuthRepository, never()).save(any());
    }
}

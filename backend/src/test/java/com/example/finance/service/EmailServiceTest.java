package com.example.finance.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService - Group 6
 * Tests email sending functionality with JavaMailSender
 */
@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Set email configuration using reflection
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Financial Management App");
        ReflectionTestUtils.setField(emailService, "emailVerificationEnabled", true);
    }

    /**
     * Test sendVerificationEmail - Success
     */
    @Test
    void testSendVerificationEmail_Success() throws MessagingException {
        // Arrange
        String toEmail = "user@example.com";
        String username = "testuser";
        String verificationUrl = "http://example.com/verify?token=abc123";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendVerificationEmail(toEmail, username, verificationUrl);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * Test sendVerificationEmail - Disabled
     */
    @Test
    void testSendVerificationEmail_Disabled() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailVerificationEnabled", false);
        String toEmail = "user@example.com";
        String username = "testuser";
        String verificationUrl = "http://example.com/verify?token=abc123";

        // Act
        emailService.sendVerificationEmail(toEmail, username, verificationUrl);

        // Assert
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    /**
     * Test sendNotificationEmail - Success
     */
    @Test
    void testSendNotificationEmail_Success() throws MessagingException {
        // Arrange
        String toEmail = "user@example.com";
        String subject = "Test Notification";
        String message = "This is a test notification";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendNotificationEmail(toEmail, subject, message);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * Test sendBudgetAlertEmail - Success
     */
    @Test
    void testSendBudgetAlertEmail_Success() throws MessagingException {
        // Arrange
        String toEmail = "user@example.com";
        String username = "testuser";
        String budgetName = "Food Budget";
        double currentAmount = 2500000;
        double limitAmount = 2000000;

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendBudgetAlertEmail(toEmail, username, budgetName, currentAmount, limitAmount);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * Test sendEmail (Generic) - Success
     */
    @Test
    void testSendEmail_Success() throws MessagingException {
        // Arrange
        String to = "user@example.com";
        String subject = "Test Subject";
        String htmlContent = "<html><body>Test email</body></html>";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendEmail(to, subject, htmlContent);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * Test sendGoalCompletionEmail - Success
     */
    @Test
    void testSendGoalCompletionEmail_Success() throws MessagingException {
        // Arrange
        String to = "user@example.com";
        String username = "testuser";
        String goalName = "Buy a Car";
        double targetAmount = 500000000;
        String completionTime = "2025-12-29 10:30";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendGoalCompletionEmail(to, username, goalName, targetAmount, completionTime);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * Test sendGoalMilestoneEmail - Success
     */
    @Test
    void testSendGoalMilestoneEmail_Success() throws MessagingException {
        // Arrange
        String to = "user@example.com";
        String username = "testuser";
        String goalName = "Buy a Car";
        int milestone = 50;
        double progress = 0.5;
        double currentAmount = 250000000;
        double remainingAmount = 250000000;

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendGoalMilestoneEmail(to, username, goalName, milestone, progress, currentAmount, remainingAmount);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * Test sendPasswordResetEmail - Success
     */
    @Test
    void testSendPasswordResetEmail_Success() {
        // Arrange
        String to = "user@example.com";
        String username = "testuser";
        String resetUrl = "http://example.com/reset?token=xyz789";

        // Act
        emailService.sendPasswordResetEmail(to, username, resetUrl);

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    /**
     * Test sendEmail - Missing fromEmail Configuration
     */
    @Test
    void testSendEmail_MissingFromEmail() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "fromEmail", "");
        String to = "user@example.com";
        String subject = "Test Subject";
        String htmlContent = "<html><body>Test</body></html>";

        // Act
        emailService.sendEmail(to, subject, htmlContent);

        // Assert - should skip sending
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    /**
     * Test sendEmail - MessagingException
     */
    @Test
    void testSendEmail_MessagingException() {
        // Arrange
        String to = "user@example.com";
        String subject = "Test Subject";
        String htmlContent = "<html><body>Test</body></html>";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Mail server error"))
            .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailService.sendEmail(to, subject, htmlContent);
        });

        assertTrue(exception.getMessage().contains("Không thể gửi email") || 
                   exception.getMessage().contains("Mail server error"));
    }

    /**
     * Test sendPasswordResetEmail - Exception
     */
    @Test
    void testSendPasswordResetEmail_Exception() {
        // Arrange
        String to = "user@example.com";
        String username = "testuser";
        String resetUrl = "http://example.com/reset?token=xyz789";

        doThrow(new RuntimeException("Mail server down"))
            .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailService.sendPasswordResetEmail(to, username, resetUrl);
        });

        assertTrue(exception.getMessage().contains("Không thể gửi email đặt lại mật khẩu"));
    }
}

package com.example.finance.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${email.from}")
    private String fromEmail;

    @Value("${email.from.name}")
    private String fromName;

    /**
     * Gửi email verification
     */
    public void sendVerificationEmail(String toEmail, String username, String verificationUrl) {
        String subject = "Xác thực email - Financial Management App";
        
        String htmlContent = generateVerificationEmailTemplate(username, verificationUrl);
        
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Gửi email thông báo
     */
    public void sendNotificationEmail(String toEmail, String subject, String message) {
        String htmlContent = generateNotificationEmailTemplate(subject, message);
        
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Gửi email thông báo budget
     */
    public void sendBudgetAlertEmail(String toEmail, String username, String budgetName, double currentAmount, double limitAmount) {
        String subject = "Cảnh báo Budget - Financial Management App";
        
        String htmlContent = generateBudgetAlertEmailTemplate(username, budgetName, currentAmount, limitAmount);
        
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Gửi email với template HTML
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo template email verification
     */
    private String generateVerificationEmailTemplate(String username, String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Xác thực Email</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; background: #667eea; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎯 Financial Management App</h1>
                        <p>Xác thực tài khoản của bạn</p>
                    </div>
                    <div class="content">
                        <h2>Xin chào %s!</h2>
                        <p>Cảm ơn bạn đã đăng ký tài khoản tại Financial Management App. Để hoàn tất quá trình đăng ký, vui lòng xác thực email của bạn.</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">Xác thực Email</a>
                        </div>
                        
                        <p><strong>Lưu ý:</strong> Link xác thực này sẽ hết hạn sau 24 giờ.</p>
                        
                        <p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>
                        
                        <p>Trân trọng,<br>Đội ngũ Financial Management App</p>
                    </div>
                    <div class="footer">
                        <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, verificationUrl);
    }

    /**
     * Gửi email đặt lại mật khẩu
     */
    public void sendPasswordResetEmail(String to, String username, String resetUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Đặt lại mật khẩu - Financial Management App");
            message.setText(String.format(
                "Xin chào %s,\n\n" +
                "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Financial Management App.\n\n" +
                "Để đặt lại mật khẩu, vui lòng click vào link sau:\n" +
                "%s\n\n" +
                "Link này sẽ hết hạn sau 1 giờ.\n\n" +
                "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Financial Management App Team",
                username, resetUrl
            ));
            
            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
            
        } catch (Exception e) {
            log.error("Error sending password reset email to: {}", to, e);
            throw new RuntimeException("Không thể gửi email đặt lại mật khẩu: " + e.getMessage());
        }
    }

    /**
     * Tạo template email thông báo
     */
    private String generateNotificationEmailTemplate(String subject, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Thông báo</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📢 Financial Management App</h1>
                        <p>Thông báo mới</p>
                    </div>
                    <div class="content">
                        <h2>%s</h2>
                        <p>%s</p>
                        
                        <p>Trân trọng,<br>Đội ngũ Financial Management App</p>
                    </div>
                    <div class="footer">
                        <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(subject, message);
    }

    /**
     * Tạo template email cảnh báo budget
     */
    private String generateBudgetAlertEmailTemplate(String username, String budgetName, double currentAmount, double limitAmount) {
        double percentage = (currentAmount / limitAmount) * 100;
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Cảnh báo Budget</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #ff6b6b 0%%, #ee5a24 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .alert { background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>⚠️ Cảnh báo Budget</h1>
                        <p>Financial Management App</p>
                    </div>
                    <div class="content">
                        <h2>Xin chào %s!</h2>
                        
                        <div class="alert">
                            <h3>🚨 Cảnh báo Budget: %s</h3>
                            <p><strong>Chi tiêu hiện tại:</strong> %.2f VND</p>
                            <p><strong>Giới hạn:</strong> %.2f VND</p>
                            <p><strong>Phần trăm sử dụng:</strong> %.1f%%</p>
                        </div>
                        
                        <p>Budget của bạn đang gần đạt giới hạn. Hãy kiểm tra và điều chỉnh chi tiêu để tránh vượt quá ngân sách đã định.</p>
                        
                        <p>Trân trọng,<br>Đội ngũ Financial Management App</p>
                    </div>
                    <div class="footer">
                        <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, budgetName, currentAmount, limitAmount, percentage);
    }
}

package com.example.finance.service;

import com.example.finance.entity.Goal;
import com.example.finance.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

 private final EmailService emailService;
 private final GoalService goalService;
 private final UserService userService;

 @Value("${notification.email.enabled:true}")
 private boolean emailNotificationsEnabled;

 @Value("${notification.email.goal-completion:true}")
 private boolean goalCompletionEmailsEnabled;

 @Value("${notification.email.goal-milestones:true}")
 private boolean goalMilestoneEmailsEnabled;

 @Value("${notification.email.milestone-percentages:25,50,80}")
 private String milestonePercentages;

 /**
 * Gửi email thông báo hoàn thành mục tiêu
 */
 public void sendGoalCompletionEmail(User user, Goal goal) {
 if (!emailNotificationsEnabled || !goalCompletionEmailsEnabled) {
 log.info("Email notifications are disabled for goal completion");
 return;
 }

 try {
 String subject = " Chúc mừng! Bạn đã hoàn thành mục tiêu tài chính";
 String content = createGoalCompletionEmailContent(user, goal);
 
 emailService.sendEmail(user.getEmail(), subject, content);
 log.info(" Goal completion email sent successfully to user: {} for goal: {}", user.getUsername(), goal.getName());
 } catch (Exception e) {
 log.error(" Failed to send goal completion email to user: {} for goal: {}", user.getUsername(), goal.getName(), e);
 }
 }

 /**
 * Gửi email thông báo đạt mốc mục tiêu
 */
 public void sendGoalMilestoneEmail(User user, Goal goal, int milestone, double progress) {
 if (!emailNotificationsEnabled || !goalMilestoneEmailsEnabled) {
 log.info("Email notifications are disabled for goal milestones");
 return;
 }

 try {
 String subject = String.format(" Mục tiêu '%s' đã đạt %d%%!", goal.getName(), milestone);
 String content = createGoalMilestoneEmailContent(user, goal, milestone, progress);
 
 emailService.sendEmail(user.getEmail(), subject, content);
 log.info(" Goal milestone email sent successfully to user: {} for goal: {} at {}%", user.getUsername(), goal.getName(), milestone);
 } catch (Exception e) {
 log.error(" Failed to send goal milestone email to user: {} for goal: {} at {}%", user.getUsername(), goal.getName(), milestone, e);
 }
 }

 /**
 * Scheduled task to send weekly goal progress emails to users who have opted in
 */
 @Scheduled(fixedRate = 7 * 24 * 60 * 60 * 1000) // Mỗi 7 ngày (1 tuần)
 public void sendWeeklyGoalProgressEmails() {
 if (!emailNotificationsEnabled) {
 log.info("📧 Email notifications are disabled globally");
 return;
 }

 try {
 log.info("📧 Starting weekly goal progress email notifications...");
 
 // Lấy tất cả user có mục tiêu đang hoạt động và đã bật thông báo email
 List<User> usersWithGoals = userService.getUsersWithActiveGoals();
 
 // Lọc ra chỉ những user có email hợp lệ và đã bật thông báo
 List<User> eligibleUsers = usersWithGoals.stream()
 .filter(this::isEligibleForEmailNotifications)
 .toList();
 
 log.info("📧 Found {} users with active goals, {} are eligible for email notifications", 
 usersWithGoals.size(), eligibleUsers.size());
 
 for (User user : eligibleUsers) {
 try {
 sendWeeklyGoalProgressEmail(user);
 Thread.sleep(1000); // Tránh spam email server
 } catch (Exception e) {
 log.error("❌ Failed to send weekly goal progress email to user: {}", user.getUsername(), e);
 }
 }
 
 log.info("✅ Weekly goal progress email notifications completed for {} users", eligibleUsers.size());
 } catch (Exception e) {
 log.error("❌ Failed to send weekly goal progress emails", e);
 }
 }

 /**
 * Check if user is eligible for email notifications
 */
 private boolean isEligibleForEmailNotifications(User user) {
 // Kiểm tra email hợp lệ
 if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
 return false;
 }
 
 String email = user.getEmail().toLowerCase();
 
 // Loại bỏ email test/demo
 if (email.contains("example.com") || 
     email.contains("test.com") ||
     email.contains("demo.com") ||
     email.contains("localhost") ||
     !email.contains("@")) {
 return false;
 }
 
 // Kiểm tra user có bật thông báo email không (mặc định là bật)
 // Có thể thêm field emailNotificationsEnabled trong User entity
 // Hiện tại mặc định là true
 return true;
 }

 /**
 * Gửi email thông báo hàng tuần về tiến độ mục tiêu
 */
 private void sendWeeklyGoalProgressEmail(User user) {
 try {
 String subject = " Báo cáo tiến độ mục tiêu tuần này";
 String content = createWeeklyGoalProgressEmailContent(user);
 
 emailService.sendEmail(user.getEmail(), subject, content);
 log.info(" Weekly goal progress email sent to user: {}", user.getUsername());
 } catch (Exception e) {
 log.error(" Failed to send weekly goal progress email to user: {}", user.getUsername(), e);
 }
 }

 /**
 * Tạo nội dung email hoàn thành mục tiêu
 */
 private String createGoalCompletionEmailContent(User user, Goal goal) {
 return String.format("""
 <!DOCTYPE html>
 <html lang="vi">
 <head>
 <meta charset="UTF-8">
 <meta name="viewport" content="width=device-width, initial-scale=1.0">
 <title>Chúc mừng hoàn thành mục tiêu!</title>
 <style>
 body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
 .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }
 .header { background: linear-gradient(135deg, #28a745, #20c997); color: white; padding: 40px 30px; text-align: center; }
 .header h1 { margin: 0; font-size: 28px; font-weight: 300; }
 .header p { margin: 10px 0 0 0; font-size: 16px; opacity: 0.9; }
 .content { padding: 40px 30px; }
 .greeting { font-size: 20px; color: #333; margin-bottom: 20px; }
 .goal-details { background: #f8f9fa; border-radius: 15px; padding: 25px; margin: 25px 0; border-left: 5px solid #28a745; }
 .goal-name { font-size: 22px; font-weight: bold; color: #28a745; margin-bottom: 15px; }
 .goal-info { display: flex; justify-content: space-between; margin: 10px 0; }
 .goal-label { font-weight: 600; color: #666; }
 .goal-value { font-weight: bold; color: #333; }
 .cta-section { text-align: center; margin: 35px 0; }
 .cta-button { display: inline-block; background: #28a745; color: white; padding: 15px 35px; text-decoration: none; border-radius: 25px; font-weight: bold; font-size: 16px; transition: all 0.3s ease; }
 .cta-button:hover { background: #218838; transform: translateY(-2px); box-shadow: 0 5px 15px rgba(40, 167, 69, 0.3); }
 .footer { background: #f8f9fa; padding: 25px 30px; text-align: center; color: #666; font-size: 14px; }
 .social-links { margin-top: 15px; }
 .social-links a { color: #28a745; text-decoration: none; margin: 0 10px; }
 @media (max-width: 600px) { .container { margin: 10px; } .header, .content, .footer { padding: 20px; } }
 </style>
 </head>
 <body>
 <div class="container">
 <div class="header">
 <h1> Chúc mừng!</h1>
 <p>Bạn đã hoàn thành mục tiêu tài chính</p>
 </div>
 
 <div class="content">
 <div class="greeting">Xin chào <strong>%s</strong>!</div>
 
 <p>Chúng tôi rất vui mừng thông báo rằng bạn đã đạt được mục tiêu tài chính của mình! Đây là một thành tựu tuyệt vời đáng để ăn mừng.</p>
 
 <div class="goal-details">
 <div class="goal-name"> %s</div>
 <div class="goal-info">
 <span class="goal-label">Số tiền tiết kiệm:</span>
 <span class="goal-value">%,.0f VNĐ</span>
 </div>
 <div class="goal-info">
 <span class="goal-label">Thời gian hoàn thành:</span>
 <span class="goal-value">%s</span>
 </div>
 </div>
 
 <p>Bạn đã chứng minh rằng với sự kiên trì và kế hoạch tài chính tốt, mọi mục tiêu đều có thể đạt được. Hãy tiếp tục duy trì thói quen tài chính tuyệt vời này!</p>
 
 <div class="cta-section">
 <a href="http://localhost:3000/goals" class="cta-button">Xem mục tiêu của bạn</a>
 </div>
 
 <p><strong>Lời khuyên:</strong> Hãy xem xét đặt mục tiêu mới để tiếp tục hành trình tài chính thành công của bạn.</p>
 </div>
 
 <div class="footer">
 <p>Trân trọng,<br><strong>Đội ngũ Finance AI</strong></p>
 <div class="social-links">
 <a href="#">Website</a> | <a href="#">Hỗ trợ</a> | <a href="#">Hướng dẫn</a>
 </div>
 <p style="margin-top: 15px; font-size: 12px; color: #999;">
 Email này được gửi tự động. Vui lòng không trả lời email này.
 </p>
 </div>
 </div>
 </body>
 </html>
 """, 
 user.getUsername(),
 goal.getName(),
 goal.getTargetAmount(),
 LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'lúc' HH:mm"))
 );
 }

 /**
 * Tạo nội dung email đạt mốc mục tiêu
 */
 private String createGoalMilestoneEmailContent(User user, Goal goal, int milestone, double progress) {
 String milestoneText = "";
 String encouragementText = "";
 
 switch (milestone) {
 case 25:
 milestoneText = "Bắt đầu tốt!";
 encouragementText = "Bạn đã có một khởi đầu tuyệt vời. Hãy tiếp tục duy trì động lực!";
 break;
 case 50:
 milestoneText = "Nửa chặng đường!";
 encouragementText = "Bạn đã đi được nửa chặng đường. Hãy kiên trì để đạt đến đích!";
 break;
 case 80:
 milestoneText = "Gần hoàn thành!";
 encouragementText = "Chỉ còn một chút nữa thôi! Bạn đang rất gần với mục tiêu của mình.";
 break;
 }
 
 return String.format("""
 <!DOCTYPE html>
 <html lang="vi">
 <head>
 <meta charset="UTF-8">
 <meta name="viewport" content="width=device-width, initial-scale=1.0">
 <title>Mục tiêu đạt mốc %d%%</title>
 <style>
 body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
 .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }
 .header { background: linear-gradient(135deg, #ffc107, #fd7e14); color: white; padding: 40px 30px; text-align: center; }
 .header h1 { margin: 0; font-size: 28px; font-weight: 300; }
 .header p { margin: 10px 0 0 0; font-size: 16px; opacity: 0.9; }
 .content { padding: 40px 30px; }
 .greeting { font-size: 20px; color: #333; margin-bottom: 20px; }
 .milestone-badge { display: inline-block; background: #ffc107; color: #333; padding: 8px 20px; border-radius: 20px; font-weight: bold; font-size: 18px; margin: 20px 0; }
 .goal-details { background: #fff3cd; border-radius: 15px; padding: 25px; margin: 25px 0; border-left: 5px solid #ffc107; }
 .goal-name { font-size: 22px; font-weight: bold; color: #856404; margin-bottom: 15px; }
 .progress-bar { background: #e9ecef; border-radius: 10px; height: 20px; margin: 15px 0; overflow: hidden; }
 .progress-fill { background: linear-gradient(90deg, #ffc107, #fd7e14); height: 100%%; width: %.1f%%; transition: width 0.3s ease; }
 .goal-info { display: flex; justify-content: space-between; margin: 10px 0; }
 .goal-label { font-weight: 600; color: #666; }
 .goal-value { font-weight: bold; color: #333; }
 .cta-section { text-align: center; margin: 35px 0; }
 .cta-button { display: inline-block; background: #ffc107; color: #333; padding: 15px 35px; text-decoration: none; border-radius: 25px; font-weight: bold; font-size: 16px; transition: all 0.3s ease; }
 .cta-button:hover { background: #e0a800; transform: translateY(-2px); box-shadow: 0 5px 15px rgba(255, 193, 7, 0.3); }
 .footer { background: #f8f9fa; padding: 25px 30px; text-align: center; color: #666; font-size: 14px; }
 @media (max-width: 600px) { .container { margin: 10px; } .header, .content, .footer { padding: 20px; } }
 </style>
 </head>
 <body>
 <div class="container">
 <div class="header">
 <h1> Mục tiêu đạt mốc!</h1>
 <p>%s</p>
 </div>
 
 <div class="content">
 <div class="greeting">Xin chào <strong>%s</strong>!</div>
 
 <div style="text-align: center;">
 <div class="milestone-badge">%d%% Hoàn thành</div>
 </div>
 
 <p>%s</p>
 
 <div class="goal-details">
 <div class="goal-name"> %s</div>
 <div class="progress-bar">
 <div class="progress-fill"></div>
 </div>
 <div class="goal-info">
 <span class="goal-label">Tiến độ hiện tại:</span>
 <span class="goal-value">%.1f%%</span>
 </div>
 <div class="goal-info">
 <span class="goal-label">Số tiền đã tiết kiệm:</span>
 <span class="goal-value">%,.0f VNĐ</span>
 </div>
 <div class="goal-info">
 <span class="goal-label">Còn lại:</span>
 <span class="goal-value">%,.0f VNĐ</span>
 </div>
 </div>
 
 <p><strong>Lời khuyên:</strong> Hãy duy trì thói quen tiết kiệm hiện tại. Bạn đang làm rất tốt!</p>
 
 <div class="cta-section">
 <a href="http://localhost:3000/goals" class="cta-button">Xem tiến độ mục tiêu</a>
 </div>
 </div>
 
 <div class="footer">
 <p>Trân trọng,<br><strong>Đội ngũ Finance AI</strong></p>
 <p style="margin-top: 15px; font-size: 12px; color: #999;">
 Email này được gửi tự động. Vui lòng không trả lời email này.
 </p>
 </div>
 </div>
 </body>
 </html>
 """, 
 milestone,
 milestoneText,
 user.getUsername(),
 milestone,
 encouragementText,
 goal.getName(),
 progress,
 goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO,
 goal.getTargetAmount().subtract(goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO)
 );
 }

 /**
 * Tạo nội dung email báo cáo hàng tuần
 */
 private String createWeeklyGoalProgressEmailContent(User user) {
 return String.format("""
 <!DOCTYPE html>
 <html lang="vi">
 <head>
 <meta charset="UTF-8">
 <meta name="viewport" content="width=device-width, initial-scale=1.0">
 <title>Báo cáo tiến độ mục tiêu tuần này</title>
 <style>
 body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
 .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }
 .header { background: linear-gradient(135deg, #667eea, #764ba2); color: white; padding: 40px 30px; text-align: center; }
 .header h1 { margin: 0; font-size: 28px; font-weight: 300; }
 .header p { margin: 10px 0 0 0; font-size: 16px; opacity: 0.9; }
 .content { padding: 40px 30px; }
 .greeting { font-size: 20px; color: #333; margin-bottom: 20px; }
 .cta-section { text-align: center; margin: 35px 0; }
 .cta-button { display: inline-block; background: #667eea; color: white; padding: 15px 35px; text-decoration: none; border-radius: 25px; font-weight: bold; font-size: 16px; transition: all 0.3s ease; }
 .cta-button:hover { background: #5a6fd8; transform: translateY(-2px); box-shadow: 0 5px 15px rgba(102, 126, 234, 0.3); }
 .footer { background: #f8f9fa; padding: 25px 30px; text-align: center; color: #666; font-size: 14px; }
 @media (max-width: 600px) { .container { margin: 10px; } .header, .content, .footer { padding: 20px; } }
 </style>
 </head>
 <body>
 <div class="container">
 <div class="header">
 <h1> Báo cáo tiến độ mục tiêu</h1>
 <p>Tuần này của bạn</p>
 </div>
 
 <div class="content">
 <div class="greeting">Xin chào <strong>%s</strong>!</div>
 
 <p>Đây là báo cáo tiến độ mục tiêu tài chính của bạn trong tuần này.</p>
 
 <p>Hãy kiểm tra tiến độ và tiếp tục phấn đấu để đạt được mục tiêu của mình!</p>
 
 <div class="cta-section">
 <a href="http://localhost:3000/goals" class="cta-button">Xem tiến độ mục tiêu</a>
 </div>
 
 <p><strong>Lời khuyên:</strong> Duy trì thói quen tiết kiệm đều đặn sẽ giúp bạn đạt được mục tiêu nhanh hơn.</p>
 </div>
 
 <div class="footer">
 <p>Trân trọng,<br><strong>Đội ngũ Finance AI</strong></p>
 <p style="margin-top: 15px; font-size: 12px; color: #999;">
 Email này được gửi tự động. Vui lòng không trả lời email này.
 </p>
 </div>
 </div>
 </body>
 </html>
 """, 
 user.getUsername()
 );
 }
}

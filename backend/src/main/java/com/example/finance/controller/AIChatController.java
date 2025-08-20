package com.example.finance.controller;

import com.example.finance.dto.AIChatRequest;
import com.example.finance.dto.AIChatResponse;
import com.example.finance.service.AIFinanceService;
import com.example.finance.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/ai")
@Slf4j
@CrossOrigin(origins = "*")
public class AIChatController {

    @Autowired
    private AIFinanceService aiFinanceService;

    @Autowired
    private ReportService reportService;

    @PostMapping("/chat")
    public AIChatResponse chat(@RequestBody AIChatRequest request) {
        try {
            log.info("Received AI chat request: {}", request.getMessage());
            
            String answer = aiFinanceService.processMessage(request.getMessage());
            
            AIChatResponse resp = new AIChatResponse();
            resp.setAnswer(answer);
            return resp;
        } catch (Exception e) {
            log.error("Error in AI chat", e);
            AIChatResponse resp = new AIChatResponse();
            resp.setAnswer("Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.");
            return resp;
        }
    }

    @PostMapping("/export-report")
    public ResponseEntity<String> exportReport(@RequestBody AIChatRequest request) {
        try {
            log.info("=== EXPORT REPORT ENDPOINT CALLED ===");
            log.info("Received export report request: {}", request.getMessage());
            
            // Tạm thời sử dụng username mặc định, trong thực tế sẽ lấy từ JWT token
            String username = "admin"; // Sẽ được cập nhật sau
            log.info("Using username: {}", username);
            
            Long userId = reportService.getUserIdByUsername(username);
            log.info("Found userId: {}", userId);
            
            // Phân tích yêu cầu xuất báo cáo
            String reportType = determineReportType(request.getMessage());
            String format = determineExportFormat(request.getMessage());
            log.info("Report type: {}, Format: {}", reportType, format);
            
            // Tạo báo cáo
            String reportContent = reportService.generateTextReport(
                userId,
                reportType,
                null, // dateFrom
                null, // dateTo
                null, // month
                null  // year
            );
            log.info("Report generated successfully, length: {}", reportContent.length());
            
                                    // Định dạng response
                        String response = "📊 **BÁO CÁO ĐÃ ĐƯỢC TẠO**\n\n" +
                                        "**Loại báo cáo**: " + getReportTypeName(reportType) + "\n" +
                                        "**Định dạng**: " + getFormatName(format) + "\n\n" +
                                        "**Nội dung báo cáo**:\n" +
                                        "```\n" + reportContent + "\n```\n\n" +
                                        "💡 **Lưu ý**: Báo cáo này được tạo trong AI Chat.\n\n" +
                                        "📄 **HƯỚNG DẪN XUẤT FILE**:\n" +
                                        "**Excel (.xlsx)**:\n" +
                                        "1. Copy toàn bộ nội dung báo cáo\n" +
                                        "2. Mở Microsoft Excel\n" +
                                        "3. Paste vào ô A1\n" +
                                        "4. Chọn File → Save As → Excel Workbook (.xlsx)\n\n" +
                                        "**PDF (.pdf)**:\n" +
                                        "1. Copy toàn bộ nội dung báo cáo\n" +
                                        "2. Mở Microsoft Word\n" +
                                        "3. Paste vào trang mới\n" +
                                        "4. Chọn File → Save As → PDF (.pdf)\n\n" +
                                        "**Text (.txt)**:\n" +
                                        "• Sử dụng nút 'Tải về (.txt)' bên dưới\n" +
                                        "• Hoặc copy và paste vào Notepad\n\n" +
                                        "🔧 **Tính năng nâng cao**:\n" +
                                        "• Copy báo cáo: Sử dụng nút 'Copy báo cáo'\n" +
                                        "• In báo cáo: Sử dụng nút 'In báo cáo'\n" +
                                        "• Tải về: Sử dụng nút 'Tải về (.txt)'";
            
            log.info("=== EXPORT REPORT SUCCESS ===");
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE + "; charset=UTF-8")
                .body(response);
            
        } catch (Exception e) {
            log.error("=== EXPORT REPORT ERROR ===", e);
            return ResponseEntity.badRequest()
                .body("❌ Xin lỗi, tôi không thể xuất báo cáo lúc này. Vui lòng thử lại sau.\n\n" +
                      "Chi tiết lỗi: " + e.getMessage());
        }
    }

    // Test endpoint để kiểm tra controller có hoạt động không
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("Test endpoint called");
        return ResponseEntity.ok("AIChatController is working!");
    }

    private String determineReportType(String message) {
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("tổng hợp") || lowerMessage.contains("summary")) {
            return "summary";
        } else if (lowerMessage.contains("giao dịch") || lowerMessage.contains("transaction")) {
            return "transactions";
        } else if (lowerMessage.contains("ngân sách") || lowerMessage.contains("budget")) {
            return "budgets";
        } else {
            return "summary"; // Mặc định
        }
    }

    private String determineExportFormat(String message) {
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("excel") || lowerMessage.contains("xlsx")) {
            return "excel";
        } else if (lowerMessage.contains("pdf")) {
            return "pdf";
        } else {
            return "text"; // Mặc định
        }
    }

    private String getReportTypeName(String reportType) {
        switch (reportType) {
            case "summary": return "Báo cáo tổng hợp";
            case "transactions": return "Báo cáo giao dịch";
            case "budgets": return "Báo cáo ngân sách";
            default: return "Báo cáo tổng hợp";
        }
    }

    private String getFormatName(String format) {
        switch (format) {
            case "excel": return "Excel (.xlsx)";
            case "pdf": return "PDF (.pdf)";
            case "text": return "Văn bản";
            default: return "Văn bản";
        }
    }
}

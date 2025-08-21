package com.example.finance.controller;

import com.example.finance.dto.AIChatRequest;
import com.example.finance.dto.AIChatResponse;
import com.example.finance.service.AIFinanceService;
import com.example.finance.service.ReportService;
import com.example.finance.service.ExportService;
import com.example.finance.service.AIFinancialAnalysisService;
import com.example.finance.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Slf4j
@CrossOrigin(origins = "*")
public class AIChatController {

    @Autowired
    private AIFinanceService aiFinanceService;
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ExportService exportService;
    
    @Autowired
    private AIFinancialAnalysisService aiFinancialAnalysisService;

    /**
     * AI Chat chính - xử lý tin nhắn và trả về phản hồi thông minh
     */
    @PostMapping("/chat")
    public AIChatResponse chat(@RequestBody AIChatRequest request) {
        try {
            log.info("Received AI chat request: {}", request.getMessage());
            
            // Lấy userId từ JWT token
            Long userId = getCurrentUserId();
            if (userId == null) {
                AIChatResponse resp = new AIChatResponse();
                resp.setAnswer("❌ Bạn cần đăng nhập để sử dụng AI Chat. Vui lòng đăng nhập và thử lại.");
                return resp;
            }
            
            String answer = aiFinanceService.processMessage(request.getMessage(), userId);
            
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

    /**
     * Xuất báo cáo dạng text (cho AI Chat)
     */
    @PostMapping("/export-report")
    public ResponseEntity<String> exportReport(@RequestBody AIChatRequest request) {
        try {
            log.info("=== EXPORT REPORT ENDPOINT CALLED ===");
            log.info("Received export report request: {}", request.getMessage());
            
            // Lấy userId từ JWT token
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Bạn cần đăng nhập để xuất báo cáo.");
            }
            
            log.info("Using userId: {}", userId);
            
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
                            "3. Paste vào document\n" +
                            "4. Chọn File → Save As → PDF Document (.pdf)\n\n" +
                            "🚀 **HOẶC**: Sử dụng nút xuất file trực tiếp bên dưới để tải về ngay!";
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in export report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("❌ Đã có lỗi xảy ra khi tạo báo cáo: " + e.getMessage());
        }
    }

    /**
     * Xuất file Excel trực tiếp
     */
    @PostMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody Map<String, Object> request) {
        try {
            log.info("=== EXPORT EXCEL ENDPOINT CALLED ===");
            
            // Lấy userId từ JWT token
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");
            
            log.info("Exporting Excel for userId: {}, from: {} to: {}", userId, startDate, endDate);
            
            // Tạo file Excel
            byte[] excelContent = exportService.generateExcelReport(userId, startDate, endDate);
            
            // Tạo tên file
            String fileName = "bao_cao_tai_chinh_" + 
                (startDate != null ? startDate.replace("-", "") : "all") + "_" +
                (endDate != null ? endDate.replace("-", "") : "all") + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            
            log.info("Excel file generated successfully, size: {} bytes", excelContent.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelContent);
                
        } catch (Exception e) {
            log.error("Error in export Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Xuất file PDF trực tiếp
     */
    @PostMapping("/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody Map<String, Object> request) {
        try {
            log.info("=== EXPORT PDF ENDPOINT CALLED ===");
            
            // Lấy userId từ JWT token
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");
            
            log.info("Exporting PDF for userId: {}, from: {} to: {}", userId, startDate, endDate);
            
            // Tạo file PDF
            byte[] pdfContent = exportService.generatePdfReport(userId, startDate, endDate);
            
            // Tạo tên file
            String fileName = "bao_cao_tai_chinh_" + 
                (startDate != null ? startDate.replace("-", "") : "all") + "_" +
                (endDate != null ? endDate.replace("-", "") : "all") + ".pdf";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            
            log.info("PDF file generated successfully, size: {} bytes", pdfContent.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
                
        } catch (Exception e) {
            log.error("Error in export PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Phân tích tài chính nâng cao
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeFinance(@RequestBody Map<String, Object> request) {
        try {
            log.info("=== AI FINANCIAL ANALYSIS ENDPOINT CALLED ===");
            
            // Lấy userId từ JWT token
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String analysisType = (String) request.get("type");
            log.info("Financial analysis request for userId: {}, type: {}", userId, analysisType);
            
            Map<String, Object> analysisResult = new HashMap<>();
            
            switch (analysisType) {
                case "comprehensive":
                    analysisResult = aiFinancialAnalysisService.comprehensiveAnalysis(userId);
                    break;
                case "prediction":
                    analysisResult = aiFinancialAnalysisService.financialPrediction(userId);
                    break;
                case "trend":
                    analysisResult = aiFinancialAnalysisService.spendingTrendAnalysis(userId);
                    break;
                case "budget":
                    analysisResult = aiFinancialAnalysisService.budgetOptimization(userId);
                    break;
                case "risk":
                    analysisResult = aiFinancialAnalysisService.riskAssessment(userId);
                    break;
                case "investment":
                    analysisResult = aiFinancialAnalysisService.investmentAdvice(userId);
                    break;
                default:
                    analysisResult.put("error", "Loại phân tích không được hỗ trợ");
                    break;
            }
            
            log.info("Analysis completed successfully for type: {}", analysisType);
            
            return ResponseEntity.ok(analysisResult);
            
        } catch (Exception e) {
            log.error("Error in financial analysis", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Đã có lỗi xảy ra khi phân tích: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Kiểm tra trạng thái AI
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("available", aiFinanceService.isAvailable());
            status.put("provider", "OpenRouter AI");
            status.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting AI status", e);
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("available", false);
            errorStatus.put("error", e.getMessage());
            return ResponseEntity.ok(errorStatus);
        }
    }

    // Helper methods
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                return userDetails.getId();
            }
        } catch (Exception e) {
            log.warn("Could not extract user ID from authentication", e);
        }
        return null;
    }

    private String determineReportType(String message) {
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("tổng hợp") || lowerMessage.contains("tổng quan")) {
            return "summary";
        } else if (lowerMessage.contains("giao dịch") || lowerMessage.contains("transaction")) {
            return "transactions";
        } else if (lowerMessage.contains("ngân sách") || lowerMessage.contains("budget")) {
            return "budget";
        } else if (lowerMessage.contains("mục tiêu") || lowerMessage.contains("goal")) {
            return "goals";
        } else {
            return "summary"; // Default
        }
    }

    private String determineExportFormat(String message) {
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("excel") || lowerMessage.contains("xlsx")) {
            return "excel";
        } else if (lowerMessage.contains("pdf")) {
            return "pdf";
        } else {
            return "text"; // Default
        }
    }

    private String getReportTypeName(String reportType) {
        switch (reportType) {
            case "summary": return "Tổng hợp tài chính";
            case "transactions": return "Giao dịch chi tiết";
            case "budget": return "Ngân sách";
            case "goals": return "Mục tiêu tài chính";
            default: return "Tổng hợp";
        }
    }

    private String getFormatName(String format) {
        switch (format) {
            case "excel": return "Excel (.xlsx)";
            case "pdf": return "PDF (.pdf)";
            default: return "Văn bản";
        }
    }
}

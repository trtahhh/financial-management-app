package com.example.finance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class AIFinanceService {

    @Autowired
    private ReportService reportService;

    // Từ khóa cho các chủ đề tài chính
    private static final Map<String, List<String>> KEYWORDS = new HashMap<>();
    
    static {
        KEYWORDS.put("saving", Arrays.asList("tiết kiệm", "tiết kiệm tiền", "tiết kiệm chi phí", "tiết kiệm hiệu quả", "tiết kiệm thông minh"));
        KEYWORDS.put("investment", Arrays.asList("đầu tư", "đầu tư tiền", "đầu tư thông minh", "đầu tư an toàn", "đầu tư sinh lời"));
        KEYWORDS.put("budget", Arrays.asList("ngân sách", "quản lý ngân sách", "lập ngân sách", "kiểm soát chi tiêu", "kế hoạch tài chính"));
        KEYWORDS.put("debt", Arrays.asList("nợ", "vay tiền", "trả nợ", "quản lý nợ", "giảm nợ"));
        KEYWORDS.put("income", Arrays.asList("thu nhập", "tăng thu nhập", "kiếm tiền", "lương", "lợi nhuận"));
        KEYWORDS.put("expense", Arrays.asList("chi tiêu", "chi phí", "tiêu tiền", "quản lý chi tiêu", "kiểm soát chi phí"));
        KEYWORDS.put("report", Arrays.asList("báo cáo", "report", "thống kê", "tổng hợp", "xuất báo cáo", "tạo báo cáo", "excel", "pdf"));
        KEYWORDS.put("analysis", Arrays.asList("phân tích", "đánh giá", "so sánh", "xu hướng", "dự báo", "dự đoán"));
        KEYWORDS.put("advice", Arrays.asList("tư vấn", "lời khuyên", "gợi ý", "hướng dẫn", "cách làm"));
        KEYWORDS.put("thanks", Arrays.asList("cảm ơn", "thanks", "thank you", "cảm ơn bạn", "tốt"));
        KEYWORDS.put("help", Arrays.asList("giúp", "help", "hỗ trợ", "làm sao", "cách nào"));
    }

    public String processMessage(String message) {
        String normalizedMessage = message.toLowerCase().trim();

        // Kiểm tra xem có phải yêu cầu báo cáo không
        if (isReportRequest(normalizedMessage)) {
            return processReportRequest(message);
        }

        // Kiểm tra xem có phải yêu cầu phân tích tài chính không
        if (isFinancialAnalysisRequest(normalizedMessage)) {
            return processFinancialAnalysisRequest(message);
        }

        // Phân loại tin nhắn
        String category = classifyMessage(normalizedMessage);
        return generateResponse(category, normalizedMessage);
    }

    private boolean isReportRequest(String message) {
        String[] reportKeywords = {"báo cáo", "report", "thống kê", "tổng hợp", "xuất", "tạo báo cáo", "excel", "pdf"};
        for (String keyword : reportKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFinancialAnalysisRequest(String message) {
        String[] analysisKeywords = {"phân tích", "đánh giá", "so sánh", "xu hướng", "dự báo", "dự đoán", "tư vấn", "lời khuyên"};
        for (String keyword : analysisKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String processReportRequest(String message) {
        try {
            // Tạm thời sử dụng username mặc định, trong thực tế sẽ lấy từ JWT token
            String username = "admin"; // Sẽ được cập nhật sau
            Long userId = reportService.getUserIdByUsername(username);

            // Phân tích loại báo cáo từ tin nhắn
            String reportType = determineReportType(message);

            // Phân tích tham số từ tin nhắn
            Map<String, Object> params = extractReportParams(message);

            // Tạo báo cáo
            String report = reportService.generateTextReport(
                userId,
                reportType,
                (String) params.get("dateFrom"),
                (String) params.get("dateTo"),
                (Integer) params.get("month"),
                (Integer) params.get("year")
            );

            // Kiểm tra xem có yêu cầu xuất Excel/PDF không
            if (message.toLowerCase().contains("excel") || message.toLowerCase().contains("pdf")) {
                return report + "\n\n💡 **Lưu ý**: Bạn đã yêu cầu xuất " + 
                       (message.toLowerCase().contains("excel") ? "Excel" : "PDF") + 
                       ".\n\n" +
                       "📊 **Hướng dẫn xuất file**:\n" +
                       "• **Excel**: Copy nội dung báo cáo → Paste vào Excel → Lưu với định dạng .xlsx\n" +
                       "• **PDF**: Copy nội dung báo cáo → Paste vào Word → Lưu với định dạng .pdf\n\n" +
                       "🔧 **Tính năng nâng cao**:\n" +
                       "• Báo cáo tổng hợp: 'tạo báo cáo tổng hợp tháng này'\n" +
                       "• Báo cáo giao dịch: 'báo cáo giao dịch từ 01/01 đến 31/01'\n" +
                       "• Báo cáo ngân sách: 'báo cáo ngân sách tháng 12 năm 2024'";
            }

            return report + "\n\n💡 **Lưu ý**: Báo cáo này được tạo tự động. Bạn có thể yêu cầu:\n" +
                   "• Báo cáo tổng hợp: 'tạo báo cáo tổng hợp tháng này'\n" +
                   "• Báo cáo giao dịch: 'báo cáo giao dịch từ 01/01 đến 31/01'\n" +
                   "• Báo cáo ngân sách: 'báo cáo ngân sách tháng 12 năm 2024'\n" +
                   "• Xuất Excel: 'tạo báo cáo Excel tháng này'\n" +
                   "• Xuất PDF: 'tạo báo cáo PDF tháng này'";

        } catch (Exception e) {
            return "❌ Xin lỗi, tôi không thể tạo báo cáo lúc này. Vui lòng thử lại sau.\n\n" +
                   "💡 **Gợi ý**: Bạn có thể yêu cầu:\n" +
                   "• 'Tạo báo cáo tổng hợp'\n" +
                   "• 'Báo cáo giao dịch tháng này'\n" +
                   "• 'Báo cáo ngân sách tháng 12'\n" +
                   "• 'Xuất báo cáo Excel'\n" +
                   "• 'Xuất báo cáo PDF'";
        }
    }

    private String processFinancialAnalysisRequest(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("phân tích") || lowerMessage.contains("đánh giá")) {
            if (lowerMessage.contains("chi tiêu") || lowerMessage.contains("chi phí")) {
                return analyzeExpenses(message);
            } else if (lowerMessage.contains("thu nhập") || lowerMessage.contains("kiếm tiền")) {
                return analyzeIncome(message);
            } else if (lowerMessage.contains("ngân sách")) {
                return analyzeBudget(message);
            } else {
                return provideGeneralFinancialAnalysis();
            }
        } else if (lowerMessage.contains("tư vấn") || lowerMessage.contains("lời khuyên")) {
            return provideFinancialAdvice(message);
        } else if (lowerMessage.contains("dự báo") || lowerMessage.contains("dự đoán")) {
            return provideFinancialForecast(message);
        }
        
        return provideGeneralFinancialAnalysis();
    }

    private String analyzeExpenses(String message) {
        return "📊 **PHÂN TÍCH CHI TIÊU THÔNG MINH**\n\n" +
               "🔍 **Cách phân tích chi tiêu hiệu quả**:\n" +
               "1. **Phân loại chi tiêu**:\n" +
               "   • Chi tiêu cần thiết (ăn uống, đi lại, nhà ở)\n" +
               "   • Chi tiêu mong muốn (giải trí, mua sắm)\n" +
               "   • Chi tiêu đầu tư (học tập, phát triển bản thân)\n\n" +
               "2. **Nguyên tắc 50/30/20**:\n" +
               "   • 50% cho nhu cầu cơ bản\n" +
               "   • 30% cho mong muốn cá nhân\n" +
               "   • 20% cho tiết kiệm và đầu tư\n\n" +
               "3. **Công cụ theo dõi**:\n" +
               "   • Sử dụng ứng dụng quản lý tài chính\n" +
               "   • Ghi chép chi tiêu hàng ngày\n" +
               "   • Đặt mục tiêu chi tiêu hàng tháng\n\n" +
               "💡 **Lời khuyên**: Hãy yêu cầu 'tạo báo cáo chi tiêu tháng này' để xem chi tiết!";
    }

    private String analyzeIncome(String message) {
        return "💰 **PHÂN TÍCH THU NHẬP VÀ TĂNG TRƯỞNG**\n\n" +
               "📈 **Cách tăng thu nhập hiệu quả**:\n" +
               "1. **Phát triển kỹ năng**:\n" +
               "   • Học thêm chứng chỉ chuyên môn\n" +
               "   • Tham gia khóa học online\n" +
               "   • Đọc sách về lĩnh vực chuyên môn\n\n" +
               "2. **Tạo nguồn thu nhập phụ**:\n" +
               "   • Freelance online\n" +
               "   • Bán hàng online\n" +
               "   • Đầu tư chứng khoán\n\n" +
               "3. **Tối ưu hóa công việc hiện tại**:\n" +
               "   • Đàm phán tăng lương\n" +
               "   • Tìm kiếm cơ hội thăng tiến\n" +
               "   • Chuyển việc với mức lương tốt hơn\n\n" +
               "💡 **Lời khuyên**: Hãy yêu cầu 'tạo báo cáo thu nhập tháng này' để xem chi tiết!";
    }

    private String analyzeBudget(String message) {
        return "📊 **PHÂN TÍCH NGÂN SÁCH VÀ KẾ HOẠCH TÀI CHÍNH**\n\n" +
               "🎯 **Cách lập ngân sách thông minh**:\n" +
               "1. **Xác định thu nhập cố định**:\n" +
               "   • Lương cơ bản\n" +
               "   • Thu nhập phụ\n" +
               "   • Thu nhập từ đầu tư\n\n" +
               "2. **Phân bổ ngân sách**:\n" +
               "   • 50% cho nhu cầu cơ bản\n" +
               "   • 30% cho mong muốn cá nhân\n" +
               "   • 20% cho tiết kiệm và đầu tư\n\n" +
               "3. **Theo dõi và điều chỉnh**:\n" +
               "   • Kiểm tra ngân sách hàng tuần\n" +
               "   • Điều chỉnh khi cần thiết\n" +
               "   • Đặt mục tiêu tiết kiệm rõ ràng\n\n" +
               "💡 **Lời khuyên**: Hãy yêu cầu 'tạo báo cáo ngân sách tháng này' để xem chi tiết!";
    }

    private String provideFinancialAdvice(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("tiết kiệm")) {
            return "💡 **LỜI KHUYÊN VỀ TIẾT KIỆM**\n\n" +
                   "1. **Đặt mục tiêu rõ ràng**:\n" +
                   "   • Tiết kiệm cho mục đích cụ thể\n" +
                   "   • Đặt thời hạn hoàn thành\n" +
                   "   • Theo dõi tiến độ thường xuyên\n\n" +
                   "2. **Phương pháp tiết kiệm**:\n" +
                   "   • Tiết kiệm tự động (trích lương)\n" +
                   "   • Tiết kiệm theo quy tắc 52 tuần\n" +
                   "   • Tiết kiệm theo phần trăm thu nhập\n\n" +
                   "3. **Tối ưu hóa chi tiêu**:\n" +
                   "   • Mua sắm thông minh\n" +
                   "   • Sử dụng mã giảm giá\n" +
                   "   • So sánh giá trước khi mua";
        } else if (lowerMessage.contains("đầu tư")) {
            return "💡 **LỜI KHUYÊN VỀ ĐẦU TƯ**\n\n" +
                   "1. **Nguyên tắc cơ bản**:\n" +
                   "   • Đầu tư dài hạn\n" +
                   "   • Đa dạng hóa danh mục\n" +
                   "   • Không đầu tư tất cả tiền\n\n" +
                   "2. **Các kênh đầu tư**:\n" +
                   "   • Gửi tiết kiệm ngân hàng\n" +
                   "   • Đầu tư chứng khoán\n" +
                   "   • Đầu tư bất động sản\n" +
                   "   • Đầu tư vàng\n\n" +
                   "3. **Quản lý rủi ro**:\n" +
                   "   • Chỉ đầu tư số tiền có thể mất\n" +
                   "   • Tìm hiểu kỹ trước khi đầu tư\n" +
                   "   • Tham khảo chuyên gia tài chính";
        } else {
            return "💡 **LỜI KHUYÊN TÀI CHÍNH TỔNG QUÁT**\n\n" +
                   "1. **Xây dựng nền tảng vững chắc**:\n" +
                   "   • Tiết kiệm khẩn cấp (3-6 tháng chi tiêu)\n" +
                   "   • Bảo hiểm cơ bản\n" +
                   "   • Quản lý nợ hiệu quả\n\n" +
                   "2. **Phát triển bền vững**:\n" +
                   "   • Tăng thu nhập thường xuyên\n" +
                   "   • Đầu tư cho giáo dục\n" +
                   "   • Xây dựng kế hoạch dài hạn\n\n" +
                   "3. **Thói quen tốt**:\n" +
                   "   • Theo dõi tài chính hàng ngày\n" +
                   "   • Đặt mục tiêu rõ ràng\n" +
                   "   • Kiên trì và nhẫn nại";
        }
    }

    private String provideFinancialForecast(String message) {
        return "🔮 **DỰ BÁO TÀI CHÍNH VÀ XU HƯỚNG**\n\n" +
               "📊 **Cách dự báo tài chính cá nhân**:\n" +
               "1. **Phân tích dữ liệu quá khứ**:\n" +
               "   • Thu nhập và chi tiêu 6-12 tháng gần đây\n" +
               "   • Xu hướng tăng/giảm\n" +
               "   • Mùa vụ và chu kỳ\n\n" +
               "2. **Dự báo thu nhập**:\n" +
               "   • Lương cơ bản và thưởng\n" +
               "   • Thu nhập từ đầu tư\n" +
               "   • Thu nhập phụ dự kiến\n\n" +
               "3. **Dự báo chi tiêu**:\n" +
               "   • Chi tiêu cố định hàng tháng\n" +
               "   • Chi tiêu biến động\n" +
               "   • Chi tiêu dự kiến (du lịch, mua sắm)\n\n" +
               "💡 **Lời khuyên**: Hãy yêu cầu 'tạo báo cáo dự báo tài chính' để xem chi tiết!";
    }

    private String provideGeneralFinancialAnalysis() {
        return "📊 **PHÂN TÍCH TÀI CHÍNH TỔNG QUÁT**\n\n" +
               "🔍 **Các khía cạnh cần phân tích**:\n" +
               "1. **Thu nhập**:\n" +
               "   • Nguồn thu nhập chính và phụ\n" +
               "   • Xu hướng tăng trưởng\n" +
               "   • Tiềm năng phát triển\n\n" +
               "2. **Chi tiêu**:\n" +
               "   • Phân loại chi tiêu\n" +
               "   • Tỷ lệ chi tiêu so với thu nhập\n" +
               "   • Cơ hội tiết kiệm\n\n" +
               "3. **Tài sản và nợ**:\n" +
               "   • Tổng tài sản hiện có\n" +
               "   • Nợ phải trả\n" +
               "   • Tỷ lệ nợ/tài sản\n\n" +
               "💡 **Lời khuyên**: Hãy yêu cầu cụ thể:\n" +
               "• 'Phân tích chi tiêu của tôi'\n" +
               "• 'Đánh giá thu nhập hiện tại'\n" +
               "• 'Tư vấn đầu tư cơ bản'\n" +
               "• 'Lời khuyên tiết kiệm'";
    }

    private String determineReportType(String message) {
        if (message.contains("tổng hợp") || message.contains("summary")) {
            return "summary";
        } else if (message.contains("giao dịch") || message.contains("transaction")) {
            return "transactions";
        } else if (message.contains("ngân sách") || message.contains("budget")) {
            return "budgets";
        } else {
            // Mặc định là báo cáo tổng hợp
            return "summary";
        }
    }

    private Map<String, Object> extractReportParams(String message) {
        Map<String, Object> params = new HashMap<>();

        // Mặc định
        params.put("dateFrom", null);
        params.put("dateTo", null);
        params.put("month", null);
        params.put("year", null);

        // Xử lý thời gian
        if (message.contains("tháng này") || message.contains("this month")) {
            LocalDate now = LocalDate.now();
            params.put("month", now.getMonthValue());
            params.put("year", now.getYear());
        } else if (message.contains("tháng trước") || message.contains("last month")) {
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            params.put("month", lastMonth.getMonthValue());
            params.put("year", lastMonth.getYear());
        } else if (message.contains("năm nay") || message.contains("this year")) {
            params.put("year", LocalDate.now().getYear());
        } else if (message.contains("năm trước") || message.contains("last year")) {
            params.put("year", LocalDate.now().getYear() - 1);
        }

        // Xử lý khoảng thời gian cụ thể
        if (message.contains("từ") && message.contains("đến")) {
            // Tìm ngày từ và đến trong tin nhắn
            // Đây là logic đơn giản, có thể cải thiện sau
            String[] parts = message.split("từ|đến");
            if (parts.length >= 3) {
                String dateFromStr = parts[1].trim();
                String dateToStr = parts[2].trim();

                // Chuyển đổi định dạng ngày (cần cải thiện)
                try {
                    if (dateFromStr.contains("/")) {
                        params.put("dateFrom", dateFromStr);
                    }
                    if (dateToStr.contains("/")) {
                        params.put("dateTo", dateToStr);
                    }
                } catch (Exception e) {
                    // Bỏ qua nếu không parse được
                }
            }
        }

        return params;
    }

    private String classifyMessage(String normalizedMessage) {
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (normalizedMessage.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return "general";
    }

    private String generateResponse(String category, String normalizedMessage) {
        switch (category) {
            case "saving":
                return "💰 **TIẾT KIỆM THÔNG MINH**\n\n" +
                       "**Nguyên tắc cơ bản**:\n" +
                       "• Tiết kiệm 20% thu nhập hàng tháng\n" +
                       "• Đặt mục tiêu tiết kiệm cụ thể\n" +
                       "• Sử dụng tài khoản tiết kiệm riêng biệt\n\n" +
                       "**Phương pháp hiệu quả**:\n" +
                       "• Tiết kiệm tự động (trích lương)\n" +
                       "• Tiết kiệm theo quy tắc 52 tuần\n" +
                       "• Tiết kiệm theo phần trăm thu nhập\n\n" +
                       "💡 **Gợi ý**: Hãy yêu cầu 'phân tích chi tiêu' để tìm cơ hội tiết kiệm!";

            case "investment":
                return "📈 **ĐẦU TƯ THÔNG MINH**\n\n" +
                       "**Nguyên tắc cơ bản**:\n" +
                       "• Chỉ đầu tư số tiền có thể mất\n" +
                       "• Đa dạng hóa danh mục đầu tư\n" +
                       "• Đầu tư dài hạn, không đầu cơ\n\n" +
                       "**Các kênh đầu tư**:\n" +
                       "• Gửi tiết kiệm ngân hàng (an toàn)\n" +
                       "• Đầu tư chứng khoán (rủi ro trung bình)\n" +
                       "• Đầu tư bất động sản (rủi ro cao)\n" +
                       "• Đầu tư vàng (bảo vệ tài sản)\n\n" +
                       "💡 **Gợi ý**: Hãy yêu cầu 'tư vấn đầu tư cơ bản' để biết thêm!";

            case "budget":
                return "📊 **QUẢN LÝ NGÂN SÁCH THÔNG MINH**\n\n" +
                       "**Nguyên tắc 50/30/20**:\n" +
                       "• 50% cho nhu cầu cơ bản (ăn, ở, đi lại)\n" +
                       "• 30% cho mong muốn cá nhân (giải trí, mua sắm)\n" +
                       "• 20% cho tiết kiệm và đầu tư\n\n" +
                       "**Cách lập ngân sách**:\n" +
                       "• Xác định thu nhập cố định\n" +
                       "• Liệt kê tất cả chi tiêu\n" +
                       "• Phân bổ theo tỷ lệ\n" +
                       "• Theo dõi và điều chỉnh\n\n" +
                       "💡 **Gợi ý**: Hãy yêu cầu 'tạo báo cáo ngân sách' để xem chi tiết!";

            case "debt":
                return "💳 **QUẢN LÝ NỢ THÔNG MINH**\n\n" +
                       "**Nguyên tắc cơ bản**:\n" +
                       "• Không vay để tiêu xài\n" +
                       "• Ưu tiên trả nợ lãi cao trước\n" +
                       "• Duy trì tỷ lệ nợ/tài sản dưới 30%\n\n" +
                       "**Chiến lược trả nợ**:\n" +
                       "• Phương pháp Snowball (nợ nhỏ trước)\n" +
                       "• Phương pháp Avalanche (lãi cao trước)\n" +
                       "• Tăng thu nhập để trả nợ nhanh hơn\n\n" +
                       "💡 **Gợi ý**: Hãy yêu cầu 'phân tích tài chính' để đánh giá tình hình nợ!";

            case "income":
                return "💵 **TĂNG THU NHẬP THÔNG MINH**\n\n" +
                       "**Phát triển kỹ năng**:\n" +
                       "• Học thêm chứng chỉ chuyên môn\n" +
                       "• Tham gia khóa học online\n" +
                       "• Đọc sách về lĩnh vực chuyên môn\n\n" +
                       "**Tạo nguồn thu nhập phụ**:\n" +
                       "• Freelance online\n" +
                       "• Bán hàng online\n" +
                       "• Đầu tư chứng khoán\n" +
                       "• Cho thuê tài sản\n\n" +
                       "💡 **Gợi ý**: Hãy yêu cầu 'phân tích thu nhập' để xem cơ hội tăng trưởng!";

            case "expense":
                return "💸 **KIỂM SOÁT CHI TIÊU THÔNG MINH**\n\n" +
                       "**Phân loại chi tiêu**:\n" +
                       "• Chi tiêu cần thiết (ăn uống, đi lại, nhà ở)\n" +
                       "• Chi tiêu mong muốn (giải trí, mua sắm)\n" +
                       "• Chi tiêu đầu tư (học tập, phát triển bản thân)\n\n" +
                       "**Cách tiết kiệm**:\n" +
                       "• Mua sắm thông minh\n" +
                       "• Sử dụng mã giảm giá\n" +
                       "• So sánh giá trước khi mua\n" +
                       "• Tránh mua sắm bốc đồng\n\n" +
                       "💡 **Gợi ý**: Hãy yêu cầu 'phân tích chi tiêu' để xem chi tiết!";

            case "report":
                return "📊 Tôi có thể giúp bạn tạo các loại báo cáo tài chính:\n\n" +
                       "**Báo cáo tổng hợp**:\n" +
                       "• 'Tạo báo cáo tổng hợp tháng này'\n" +
                       "• 'Báo cáo tổng hợp từ 01/01 đến 31/01'\n\n" +
                       "**Báo cáo giao dịch**:\n" +
                       "• 'Báo cáo giao dịch tháng này'\n" +
                       "• 'Báo cáo giao dịch từ 01/01 đến 31/01'\n\n" +
                       "**Báo cáo ngân sách**:\n" +
                       "• 'Báo cáo ngân sách tháng 12'\n" +
                       "• 'Báo cáo ngân sách tháng 12 năm 2024'\n\n" +
                       "**Xuất file**:\n" +
                       "• 'Xuất báo cáo Excel tháng này'\n" +
                       "• 'Xuất báo cáo PDF tháng này'\n\n" +
                       "Hãy cho tôi biết bạn muốn loại báo cáo nào!";

            case "analysis":
                return "🔍 **PHÂN TÍCH TÀI CHÍNH THÔNG MINH**\n\n" +
                       "Tôi có thể giúp bạn phân tích:\n\n" +
                       "**📊 Chi tiêu**:\n" +
                       "• 'Phân tích chi tiêu của tôi'\n" +
                       "• 'Đánh giá xu hướng chi tiêu'\n" +
                       "• 'Tìm cơ hội tiết kiệm'\n\n" +
                       "**💰 Thu nhập**:\n" +
                       "• 'Phân tích thu nhập hiện tại'\n" +
                       "• 'Đánh giá tiềm năng tăng trưởng'\n" +
                       "• 'So sánh thu nhập theo thời gian'\n\n" +
                       "**📈 Ngân sách**:\n" +
                       "• 'Phân tích ngân sách hàng tháng'\n" +
                       "• 'Đánh giá hiệu quả ngân sách'\n" +
                       "• 'Dự báo ngân sách tương lai'\n\n" +
                       "Hãy cho tôi biết bạn muốn phân tích khía cạnh nào!";

            case "advice":
                return "💡 **TƯ VẤN TÀI CHÍNH THÔNG MINH**\n\n" +
                       "Tôi có thể tư vấn về:\n\n" +
                       "**💰 Tiết kiệm**:\n" +
                       "• 'Tư vấn tiết kiệm hiệu quả'\n" +
                       "• 'Lời khuyên tiết kiệm cho người mới bắt đầu'\n" +
                       "• 'Cách tiết kiệm cho mục tiêu cụ thể'\n\n" +
                       "**📈 Đầu tư**:\n" +
                       "• 'Tư vấn đầu tư cơ bản'\n" +
                       "• 'Lời khuyên đầu tư an toàn'\n" +
                       "• 'Cách đầu tư cho người mới bắt đầu'\n\n" +
                       "**📊 Quản lý tài chính**:\n" +
                       "• 'Tư vấn quản lý ngân sách'\n" +
                       "• 'Lời khuyên quản lý nợ'\n" +
                       "• 'Cách lập kế hoạch tài chính'\n\n" +
                       "Hãy cho tôi biết bạn cần tư vấn về vấn đề gì!";

            case "thanks":
                return getRandomResponse(Arrays.asList(
                   "Rất vui được giúp bạn! Nếu có thêm câu hỏi gì về tài chính, đừng ngại hỏi nhé! 😊",
                   "Cảm ơn bạn đã tin tưởng! Tôi luôn sẵn sàng hỗ trợ bạn về các vấn đề tài chính.",
                   "Không có gì! Chúc bạn quản lý tài chính thật tốt. Hẹn gặp lại! 🎉"
                ));

            case "help":
                return "🔧 Tôi có thể giúp bạn với các chủ đề sau:\n\n" +
                       "**💰 Quản lý tài chính**:\n" +
                       "• Tiết kiệm và đầu tư thông minh\n" +
                       "• Quản lý ngân sách và chi tiêu hiệu quả\n" +
                       "• Xử lý nợ và vay an toàn\n" +
                       "• Tăng thu nhập bền vững\n\n" +
                       "**📊 Báo cáo và phân tích**:\n" +
                       "• Báo cáo tổng hợp tài chính\n" +
                       "• Báo cáo giao dịch và ngân sách\n" +
                       "• Phân tích chi tiêu và thu nhập thông minh\n" +
                       "• Dự báo tài chính tương lai\n\n" +
                       "**🔍 Phân tích nâng cao**:\n" +
                       "• Phân tích xu hướng chi tiêu\n" +
                       "• Đánh giá hiệu quả ngân sách\n" +
                       "• So sánh thu nhập theo thời gian\n" +
                       "• Tìm cơ hội tiết kiệm và đầu tư\n\n" +
                       "**💡 Tư vấn thông minh**:\n" +
                       "• Chiến lược tiết kiệm hiệu quả\n" +
                       "• Kế hoạch đầu tư an toàn\n" +
                       "• Quản lý rủi ro tài chính\n" +
                       "• Lập kế hoạch tài chính dài hạn\n\n" +
                       "**📄 Xuất file đa dạng**:\n" +
                       "• Xuất báo cáo Excel (.xlsx)\n" +
                       "• Xuất báo cáo PDF (.pdf)\n" +
                       "• Tải về file text (.txt)\n" +
                       "• In báo cáo trực tiếp\n\n" +
                       "**🎯 Ví dụ sử dụng**:\n" +
                       "• 'Phân tích chi tiêu của tôi'\n" +
                       "• 'Tư vấn đầu tư cơ bản'\n" +
                       "• 'Tạo báo cáo Excel tháng này'\n" +
                       "• 'Lời khuyên tiết kiệm hiệu quả'\n\n" +
                       "Hãy hỏi bất kỳ điều gì bạn quan tâm!";

            default:
                return "Tôi hiểu bạn đang tìm kiếm thông tin tài chính. Bạn có thể hỏi tôi về:\n\n" +
                       "• 💰 Tiết kiệm và đầu tư\n" +
                       "• 📊 Quản lý ngân sách\n" +
                       "• 💳 Xử lý nợ và vay\n" +
                       "• 📈 Tăng thu nhập\n" +
                       "• 📋 Tạo báo cáo tài chính\n" +
                       "• 🔍 Phân tích tài chính\n" +
                       "• 💡 Tư vấn tài chính\n" +
                       "• 📄 Xuất file Excel/PDF\n\n" +
                       "Hoặc gõ 'giúp' để xem tất cả các chủ đề tôi có thể hỗ trợ!";
        }
    }

    private String getRandomResponse(List<String> responses) {
        Random random = new Random();
        return responses.get(random.nextInt(responses.size()));
    }
}

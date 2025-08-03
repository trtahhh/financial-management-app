package com.example.finance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class ChatGPTService {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getChatResponse(String message) {
        // If no API key is provided, return fallback response
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return getFallbackResponse(message);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            List<Map<String, String>> messages = new ArrayList<>();
            
            // System message - more general assistant
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Bạn là một trợ lý AI thông minh, hữu ích và thân thiện. Hãy trả lời bằng tiếng Việt một cách tự nhiên, chính xác và hữu ích. Bạn có thể trả lời mọi câu hỏi, không chỉ về tài chính. Hãy giữ câu trả lời rõ ràng, súc tích và dễ hiểu.");
            messages.add(systemMessage);

            // User message
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
                    return (String) messageObj.get("content");
                }
            }

            return getFallbackResponse(message);

        } catch (Exception e) {
            log.error("Error calling ChatGPT API: ", e);
            return getFallbackResponse(message);
        }
    }

    private String getFallbackResponse(String message) {
        String lowerMessage = message.toLowerCase();

        // Greetings
        if (lowerMessage.contains("xin chào") || lowerMessage.contains("hello") || lowerMessage.contains("hi") || lowerMessage.contains("chào")) {
            return "👋 Xin chào! Tôi là trợ lý AI của bạn. Tôi có thể giúp bạn trả lời các câu hỏi về tài chính, cuộc sống, công nghệ, giáo dục và nhiều chủ đề khác. Bạn muốn hỏi gì?";
        }

        // How are you
        if (lowerMessage.contains("khỏe không") || lowerMessage.contains("thế nào") || lowerMessage.contains("how are you")) {
            return "😊 Cảm ơn bạn đã hỏi! Tôi đang hoạt động tốt và sẵn sàng hỗ trợ bạn. Bạn có câu hỏi nào tôi có thể giúp không?";
        }

        // Time and date
        if (lowerMessage.contains("mấy giờ") || lowerMessage.contains("thời gian") || lowerMessage.contains("time")) {
            return "⏰ Hiện tại tôi không thể truy cập thời gian thực, nhưng bạn có thể xem thời gian trên thiết bị của mình. Tôi có thể giúp bạn về những vấn đề khác không?";
        }

        // Weather
        if (lowerMessage.contains("thời tiết") || lowerMessage.contains("weather") || lowerMessage.contains("mưa") || lowerMessage.contains("nắng")) {
            return "🌤️ Tôi không thể cung cấp thông tin thời tiết thời gian thực. Bạn nên kiểm tra ứng dụng thời tiết hoặc trang web dự báo thời tiết để có thông tin chính xác nhất!";
        }

        // Technology questions
        if (lowerMessage.contains("công nghệ") || lowerMessage.contains("máy tính") || lowerMessage.contains("điện thoại") || lowerMessage.contains("technology")) {
            return "💻 Tôi có thể giúp bạn về các câu hỏi công nghệ cơ bản! Bạn có thể hỏi về cách sử dụng phần mềm, lựa chọn thiết bị, hoặc các khái niệm công nghệ. Bạn muốn biết gì cụ thể?";
        }

        // Health questions
        if (lowerMessage.contains("sức khỏe") || lowerMessage.contains("bệnh") || lowerMessage.contains("health") || lowerMessage.contains("khỏe mạnh")) {
            return "🏥 Đối với các câu hỏi về sức khỏe, tôi khuyên bạn nên tham khảo ý kiến bác sĩ chuyên nghiệp. Tôi có thể chia sẻ một số thông tin tổng quát về lối sống lành mạnh như ăn uống cân bằng, tập thể dục đều đặn và ngủ đủ giấc.";
        }

        // Education questions
        if (lowerMessage.contains("học") || lowerMessage.contains("giáo dục") || lowerMessage.contains("education") || lowerMessage.contains("kiến thức")) {
            return "📚 Tôi rất vui khi giúp bạn về giáo dục! Tôi có thể hỗ trợ giải thích các khái niệm, gợi ý phương pháp học tập, hoặc thảo luận về nhiều chủ đề khác nhau. Bạn muốn học về gì?";
        }

        // Travel questions  
        if (lowerMessage.contains("du lịch") || lowerMessage.contains("travel") || lowerMessage.contains("đi chơi")) {
            return "✈️ Du lịch thật tuyệt! Tôi có thể chia sẻ một số mẹo du lịch chung như lập kế hoạch chi tiết, đặt phòng sớm để có giá tốt, mang theo các vật dụng cần thiết. Bạn có kế hoạch du lịch nào cụ thể không?";
        }

        // Food questions
        if (lowerMessage.contains("ăn") || lowerMessage.contains("món ăn") || lowerMessage.contains("food") || lowerMessage.contains("nấu")) {
            return "🍽️ Tôi có thể gợi ý một số món ăn đơn giản và lành mạnh! Hãy cố gắng ăn nhiều rau xanh, trái cây, protein từ cá và thịt nạc. Bạn có sở thích ẩm thực nào đặc biệt không?";
        }

        // Financial advice responses (keep existing ones)
        if (lowerMessage.contains("tiết kiệm") || lowerMessage.contains("tiet kiem")) {
            return "🏦 **Lời khuyên tiết kiệm:**\n\n" +
                   "✅ Áp dụng quy tắc 50/30/20: 50% chi tiêu thiết yếu, 30% giải trí, 20% tiết kiệm\n" +
                   "✅ Tự động chuyển tiền tiết kiệm mỗi tháng\n" +
                   "✅ So sánh giá trước khi mua sắm\n" +
                   "✅ Tránh mua sắm theo cảm xúc\n\n" +
                   "💡 Hãy bắt đầu với số tiền nhỏ và tăng dần theo thời gian!";
        }

        if (lowerMessage.contains("đầu tư") || lowerMessage.contains("dau tu")) {
            return "📈 **Hướng dẫn đầu tư cơ bản:**\n\n" +
                   "✅ Học kiến thức tài chính trước khi đầu tư\n" +
                   "✅ Đa dạng hóa danh mục đầu tư\n" +
                   "✅ Đầu tư dài hạn thay vì ngắn hạn\n" +
                   "✅ Chỉ đầu tư số tiền bạn có thể chấp nhận mất\n" +
                   "✅ Xem xét quỹ mở, cổ phiếu blue-chip\n\n" +
                   "⚠️ Lưu ý: Luôn tự nghiên cứu kỹ trước khi đầu tư!";
        }

        if (lowerMessage.contains("ngân sách") || lowerMessage.contains("ngan sach")) {
            return "📊 **Lập ngân sách hiệu quả:**\n\n" +
                   "✅ Ghi chép tất cả thu chi trong tháng\n" +
                   "✅ Phân loại chi tiêu: cần thiết vs muốn có\n" +
                   "✅ Đặt mục tiêu tiết kiệm cụ thể\n" +
                   "✅ Xem xét và điều chỉnh hàng tháng\n" +
                   "✅ Dùng app quản lý tài chính (như app này!)\n\n" +
                   "💰 Ngân sách tốt = Tương lai tài chính ổn định!";
        }

        if (lowerMessage.contains("vay") || lowerMessage.contains("nợ") || lowerMessage.contains("no")) {
            return "💳 **Quản lý nợ thông minh:**\n\n" +
                   "✅ Ưu tiên trả nợ lãi suất cao trước\n" +
                   "✅ Tránh vay để tiêu dùng không cần thiết\n" +
                   "✅ Đàm phán lãi suất với ngân hàng\n" +
                   "✅ Consolidate nợ nếu có thể\n" +
                   "✅ Tạo kế hoạch trả nợ cụ thể\n\n" +
                   "🎯 Mục tiêu: Không nợ = Tự do tài chính!";
        }

        if (lowerMessage.contains("mục tiêu") || lowerMessage.contains("muc tieu")) {
            return "🎯 **Đặt mục tiêu tài chính SMART:**\n\n" +
                   "✅ **S**pecific: Cụ thể rõ ràng\n" +
                   "✅ **M**easurable: Có thể đo lường\n" +
                   "✅ **A**chievable: Khả thi thực hiện\n" +
                   "✅ **R**elevant: Phù hợp với bản thân\n" +
                   "✅ **T**ime-bound: Có thời hạn\n\n" +
                   "💡 Ví dụ: 'Tiết kiệm 50 triệu trong 2 năm để mua xe'";
        }

        if (lowerMessage.contains("bảo hiểm") || lowerMessage.contains("bao hiem")) {
            return "🛡️ **Bảo hiểm cần thiết:**\n\n" +
                   "✅ Bảo hiểm y tế: Ưu tiên số 1\n" +
                   "✅ Bảo hiểm nhân thọ: Nếu có người phụ thuộc\n" +
                   "✅ Bảo hiểm xe: Bắt buộc và cần thiết\n" +
                   "✅ Bảo hiểm nhà: Bảo vệ tài sản lớn\n\n" +
                   "💡 Bảo hiểm là chi phí bảo vệ, không phải đầu tư!";
        }

        // Default responses for any other questions
        String[] defaultResponses = {
            "🤖 Tôi là trợ lý AI đa năng! Tôi có thể giúp bạn về:\n• Tài chính cá nhân và đầu tư\n• Kiến thức tổng quát\n• Công nghệ và máy tính\n• Giáo dục và học tập\n• Cuộc sống và sức khỏe\n• Và nhiều chủ đề khác!\n\nHãy hỏi tôi bất cứ điều gì bạn muốn biết!",
            
            "💡 Tôi sẵn sàng trả lời câu hỏi của bạn! Dù bạn muốn biết về tài chính, công nghệ, giáo dục, sức khỏe hay bất kỳ chủ đề nào khác, tôi sẽ cố gắng hết sức để giúp đỡ.",
            
            "🎯 Câu hỏi thú vị! Mặc dù tôi có thể không có thông tin chi tiết về mọi chủ đề, nhưng tôi sẽ cố gắng cung cấp thông tin hữu ích hoặc hướng dẫn bạn tìm kiếm thông tin chính xác từ nguồn đáng tin cậy.",
            
            "🌟 Tôi có thể trò chuyện về nhiều chủ đề khác nhau! Từ những câu hỏi thường ngày đến các vấn đề phức tạp, tôi sẽ cố gắng hỗ trợ bạn một cách tốt nhất có thể.",
            
            "� Hãy cứ thoải mái hỏi tôi! Tôi có thể thảo luận về tài chính, công nghệ, khoa học, văn hóa, giải trí, và rất nhiều chủ đề khác. Câu hỏi của bạn là gì?"
        };

        return defaultResponses[new Random().nextInt(defaultResponses.length)];
    }
}

package com.example.finance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.MediaType;
import org.json.JSONObject;
import org.json.JSONArray;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class OpenRouterService {
    
    @Value("${openrouter.api-key:}")
    private String apiKey;
    
    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;
    
    @Value("${openrouter.default-model:anthropic/claude-3.5-sonnet}")
    private String defaultModel;
    
    private final WebClient webClient = WebClient.builder()
        .codecs(configurer -> {
            configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB
            configurer.defaultCodecs().enableLoggingRequestDetails(true);
        })
        .defaultHeader("Accept-Charset", "UTF-8")
        .build();
    
    public String chat(String prompt) {
        return chat(prompt, defaultModel);
    }
    
    public String chat(String prompt, String model) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.error("OpenRouter API key not configured");
                return "Xin lỗi, OpenRouter AI chưa được cấu hình. Vui lòng liên hệ admin.";
            }
            
            try {
                // Kiểm tra xem có phải câu hỏi về tài chính không
                if (isFinancialQuestion(prompt)) {
                    return getFinancialAdvice(prompt);
                }
                
                log.info("Sending chat request to OpenRouter with model: {}", model);
                
                // Tạo system prompt để AI trả lời đầy đủ và có format đẹp
                String systemPrompt = "Bạn là chuyên gia tài chính AI. Trả lời tiếng Việt chính xác và đầy đủ.\n\n" +
                    "HƯỚNG DẪN FORMAT:\n" +
                    "1. Luôn chia câu trả lời thành các đoạn văn rõ ràng\n" +
                    "2. Sử dụng dấu xuống dòng để tách các ý chính\n" +
                    "3. Sử dụng bullet points (•) cho các danh sách\n" +
                    "4. Sử dụng số thứ tự (1., 2., 3.) cho các bước hoặc ý chính\n" +
                    "5. Sử dụng **từ khóa** để nhấn mạnh\n" +
                    "6. Sử dụng __gạch chân__ cho các khái niệm quan trọng\n" +
                    "7. Mỗi ý chính nên có khoảng trống để dễ đọc\n\n" +
                    "Với mọi câu hỏi, hãy trả lời một cách toàn diện, bao gồm:\n" +
                    "• Các khía cạnh chính\n" +
                    "• Ví dụ minh họa cụ thể\n" +
                    "• Lời khuyên thực tế\n" +
                    "• Các bước thực hiện (nếu có)\n\n" +
                    "Đảm bảo người dùng hiểu rõ vấn đề và có thể áp dụng kiến thức ngay lập tức.";
                
                // Tạo request payload theo format OpenRouter
                JSONObject payload = new JSONObject();
                payload.put("model", model);
                payload.put("messages", new JSONArray()
                    .put(new JSONObject()
                        .put("role", "system")
                        .put("content", systemPrompt))
                    .put(new JSONObject()
                        .put("role", "user")
                        .put("content", prompt))
                );
                payload.put("max_tokens", 1500);
                payload.put("temperature", 0.7);
                payload.put("top_p", 0.9);
                payload.put("stream", false);
                
                String response = webClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "http://localhost:3000")
                    .header("X-Title", "Finance AI Chat")
                    .acceptCharset(StandardCharsets.UTF_8)
                    .bodyValue(payload.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(120))  // 120 second timeout
                    .block();
                
                log.info("OpenRouter raw response length: {}", response != null ? response.length() : 0);
                
                if (response == null || response.trim().isEmpty()) {
                    log.error("Empty response from OpenRouter");
                    return "Xin lỗi, AI chưa phản hồi. Vui lòng thử lại.";
                }
                
                // Parse JSON response từ OpenRouter
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                
                if (choices.length() == 0) {
                    log.error("No choices in OpenRouter response");
                    return "Xin lỗi, AI chưa có phản hồi. Vui lòng thử câu hỏi khác.";
                }
                
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                String content = message.optString("content", "").trim();
                
                // Debug logging
                log.info("OpenRouter response content: '{}'", content);
                log.info("Content length: {}", content.length());
                
                if (content.isEmpty()) {
                    log.error("Empty content in OpenRouter response");
                    return "Xin lỗi, AI chưa có phản hồi. Vui lòng thử câu hỏi khác.";
                }
                
                log.info("Chat response generated successfully, length: {}", content.length());
                return content;
                
            } catch (WebClientResponseException e) {
                log.error("OpenRouter API error: {} - {}", e.getStatusCode(), e.getMessage());
                if (e.getStatusCode().value() == 401) {
                    return "Xin lỗi, API key OpenRouter không hợp lệ. Vui lòng liên hệ admin.";
                } else if (e.getStatusCode().value() == 402) {
                    log.warn("OpenRouter payment required, using fallback response");
                    return getFinancialAdvice(prompt);
                } else if (e.getStatusCode().value() == 429) {
                    return "Xin lỗi, đã vượt quá giới hạn API calls. Vui lòng thử lại sau.";
                } else {
                    log.warn("OpenRouter technical error, using fallback response");
                    return getFinancialAdvice(prompt);
                }
            } catch (Exception e) {
                log.error("Unexpected error in OpenRouter chat: {}", e.getMessage(), e);
                if (e.getMessage() != null && e.getMessage().contains("TimeoutException")) {
                    return "Xin lỗi, câu hỏi của bạn hơi phức tạp và tôi cần thời gian suy nghĩ. " +
                           "Hãy thử đặt câu hỏi ngắn gọn hơn hoặc chia nhỏ thành nhiều câu hỏi.";
                }
                log.warn("Using fallback response due to error");
                return getFinancialAdvice(prompt);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error in OpenRouter chat: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("TimeoutException")) {
                return "Xin lỗi, câu hỏi của bạn hơi phức tạp và tôi cần thời gian suy nghĩ. " +
                       "Hãy thử đặt câu hỏi ngắn gọn hơn hoặc chia nhỏ thành nhiều câu hỏi.";
            }
            return "Có lỗi xảy ra khi xử lý yêu cầu của bạn. Vui lòng thử lại.";
        }
    }
    
    public boolean isAvailable() {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return false;
            }
            
            String response = webClient.get()
                .uri(baseUrl + "/models")
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();
            
            JSONObject jsonResponse = new JSONObject(response);
            return jsonResponse.has("data");
            
        } catch (Exception e) {
            log.warn("OpenRouter service not available: {}", e.getMessage());
            return false;
        }
    }
    
    public String[] getAvailableModels() {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return new String[0];
            }
            
            String response = webClient.get()
                .uri(baseUrl + "/models")
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();
            
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray models = jsonResponse.getJSONArray("data");
            
            return models.toList()
                .stream()
                .map(model -> ((java.util.Map<?, ?>) model).get("id").toString())
                .toArray(String[]::new);
                
        } catch (Exception e) {
            log.error("Error getting available models: {}", e.getMessage());
            return new String[0];
        }
    }
    
    public String getDefaultModel() {
        return defaultModel;
    }
    
    // Kiểm tra xem có phải câu hỏi về tài chính không
    private boolean isFinancialQuestion(String prompt) {
        if (prompt == null) return false;
        
        String lowerPrompt = prompt.toLowerCase();
        String[] financialKeywords = {
            "tiết kiệm", "tiết kiệm tiền", "đầu tư", "đầu tư như thế nào", 
            "ngân sách", "quản lý tiền", "tài chính", "chi tiêu", "thu nhập",
            "nợ", "vay", "bảo hiểm", "cổ phiếu", "trái phiếu", "quỹ đầu tư",
            "bất động sản", "vàng", "ngoại tệ", "lạm phát", "lãi suất"
        };
        
        for (String keyword : financialKeywords) {
            if (lowerPrompt.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    // Trả lời cơ bản về tài chính khi OpenRouter không khả dụng
    private String getFinancialAdvice(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("đầu tư") || lowerPrompt.contains("đầu tư như thế nào")) {
            return "**Hướng dẫn đầu tư cơ bản:**\n\n" +
                   "1. **Xác định mục tiêu đầu tư**\n" +
                   "   • Tiết kiệm ngắn hạn (1-3 năm)\n" +
                   "   • Đầu tư trung hạn (3-10 năm)\n" +
                   "   • Đầu tư dài hạn (10+ năm)\n\n" +
                   "2. **Đánh giá khả năng chấp nhận rủi ro**\n" +
                   "   • Bảo thủ: Gửi tiết kiệm, trái phiếu\n" +
                   "   • Cân bằng: Quỹ đầu tư, bất động sản\n" +
                   "   • Mạo hiểm: Cổ phiếu, tiền điện tử\n\n" +
                   "3. **Nguyên tắc đầu tư**\n" +
                   "   • Đa dạng hóa danh mục\n" +
                   "   • Đầu tư dài hạn\n" +
                   "   • Không đầu tư tiền cần thiết\n\n" +
                   "4. **Bắt đầu với**\n" +
                   "   • Gửi tiết kiệm ngân hàng\n" +
                   "   • Mua trái phiếu chính phủ\n" +
                   "   • Đầu tư vào quỹ index\n\n" +
                   "**Lời khuyên:** Hãy bắt đầu với số tiền nhỏ và học hỏi dần dần!";
        }
        
        if (lowerPrompt.contains("tiết kiệm") || lowerPrompt.contains("tiết kiệm tiền")) {
            return "**Chiến lược tiết kiệm thông minh:**\n\n" +
                   "1. **Quy tắc 50/30/20**\n" +
                   "   • 50% thu nhập: Chi tiêu cần thiết\n" +
                   "   • 30% thu nhập: Chi tiêu mong muốn\n" +
                   "   • 20% thu nhập: Tiết kiệm và đầu tư\n\n" +
                   "2. **Phương pháp tiết kiệm**\n" +
                   "   • Tiết kiệm tự động mỗi tháng\n" +
                   "   • Sử dụng tài khoản tiết kiệm riêng biệt\n" +
                   "   • Đặt mục tiêu tiết kiệm cụ thể\n\n" +
                   "3. **Cắt giảm chi phí**\n" +
                   "   • Theo dõi chi tiêu hàng ngày\n" +
                   "   • Tìm kiếm ưu đãi và giảm giá\n" +
                   "   • Hạn chế mua sắm bốc đồng\n\n" +
                   "4. **Tăng thu nhập**\n" +
                   "   • Tìm việc làm thêm\n" +
                   "   • Bán đồ không cần thiết\n" +
                   "   • Phát triển kỹ năng mới\n\n" +
                   "**Mẹo:** Hãy bắt đầu tiết kiệm ngay hôm nay, dù chỉ 10% thu nhập!";
        }
        
        if (lowerPrompt.contains("ngân sách") || lowerPrompt.contains("quản lý tiền")) {
            return "**Cách lập ngân sách gia đình:**\n\n" +
                   "1. **Thu thập thông tin**\n" +
                   "   • Tổng thu nhập hàng tháng\n" +
                   "   • Danh sách chi tiêu cố định\n" +
                   "   • Chi tiêu biến động\n\n" +
                   "2. **Phân loại chi tiêu**\n" +
                   "   • **Chi tiêu cố định:** Tiền nhà, điện nước, internet\n" +
                   "   • **Chi tiêu cần thiết:** Ăn uống, đi lại, y tế\n" +
                   "   • **Chi tiêu mong muốn:** Giải trí, mua sắm\n\n" +
                   "3. **Công cụ quản lý**\n" +
                   "   • Sổ sách ghi chép\n" +
                   "   • Ứng dụng quản lý tài chính\n" +
                   "   • Excel/Google Sheets\n\n" +
                   "4. **Nguyên tắc thực hiện**\n" +
                   "   • Lập kế hoạch trước khi chi tiêu\n" +
                   "   • Đánh giá và điều chỉnh hàng tháng\n" +
                   "   • Duy trì tính nhất quán\n\n" +
                   "**Lưu ý:** Ngân sách phải thực tế và linh hoạt!";
        }
        
        // Trả lời chung cho các câu hỏi tài chính khác
        return "**Tư vấn tài chính cơ bản:**\n\n" +
               "Tôi có thể giúp bạn với các vấn đề:\n\n" +
               "• **Tiết kiệm:** Chiến lược tiết kiệm hiệu quả\n" +
               "• **Đầu tư:** Hướng dẫn đầu tư an toàn\n" +
               "• **Ngân sách:** Quản lý chi tiêu gia đình\n" +
               "• **Quản lý nợ:** Chiến lược trả nợ thông minh\n" +
               "• **Bảo hiểm:** Lựa chọn bảo hiểm phù hợp\n\n" +
               "**Hãy đặt câu hỏi cụ thể để nhận tư vấn chi tiết!**";
    }
}

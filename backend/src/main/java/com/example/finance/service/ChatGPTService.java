package com.example.finance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatGPTService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String apiUrl;

    @Value("${spring.ai.openai.chat.model}")
    private String model;

    private final RestTemplate restTemplate;

    public String getChatResponse(String message) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return getFallbackResponse(message);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", "https://finance-app.com");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            List<Map<String, String>> messages = new ArrayList<>();
            
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Bạn là một trợ lý AI tài chính thông minh. Hãy trả lời bằng tiếng Việt ngắn gọn và rõ ràng.");
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                request, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // Kiểm tra null response body
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.warn("Empty response body from API");
                return getFallbackResponse(message);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
                
                if (messageObj != null) {
                    String content = (String) messageObj.get("content");
                    if (content != null && !content.trim().isEmpty()) {
                        return content;
                    }
                }
            }

            return getFallbackResponse(message);

        } catch (Exception e) {
            log.error("Error calling AI API: {}", e.getMessage());
            return getFallbackResponse(message);
        }
    }

    private String getFallbackResponse(String message) {
        String[] defaultResponses = {
            "🤖 Tôi là trợ lý AI đa năng! Tôi có thể giúp bạn về:\n• Tài chính cá nhân và đầu tư\n• Kiến thức tổng quát\n• Công nghệ và máy tính\n• Giáo dục và học tập\n• Cuộc sống và sức khỏe\n• Và nhiều chủ đề khác!\n\nHãy hỏi tôi bất cứ điều gì bạn muốn biết!",
            
            "💡 Tôi sẵn sàng trả lời câu hỏi của bạn! Dù bạn muốn biết về tài chính, công nghệ, giáo dục, sức khỏe hay bất kỳ chủ đề nào khác, tôi sẽ cố gắng hết sức để giúp đỡ.",
            
            "🎯 Câu hỏi thú vị! Mặc dù tôi có thể không có thông tin chi tiết về mọi chủ đề, nhưng tôi sẽ cố gắng cung cấp thông tin hữu ích hoặc hướng dẫn bạn tìm kiếm thông tin chính xác từ nguồn đáng tin cậy.",
            
            "🌟 Tôi có thể trò chuyện về nhiều chủ đề khác nhau! Từ những câu hỏi thường ngày đến các vấn đề phức tạp, tôi sẽ cố gắng hỗ trợ bạn một cách tốt nhất có thể.",
            
            "Hãy cứ thoải mái hỏi tôi! Tôi có thể thảo luận về tài chính, công nghệ, khoa học, văn hóa, giải trí, và rất nhiều chủ đề khác. Câu hỏi của bạn là gì?"
        };

        return defaultResponses[new Random().nextInt(defaultResponses.length)];
    }
}
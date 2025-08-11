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
            
            log.info("Sending chat request to OpenRouter with model: {}", model);
            
            // Tạo system prompt để AI trả lời đầy đủ
            String systemPrompt = "Bạn là chuyên gia tài chính AI. Trả lời tiếng Việt chính xác và đầy đủ. " +
                "Với mọi câu hỏi, hãy trả lời một cách toàn diện, bao gồm các khía cạnh chính, ví dụ minh họa và lời khuyên thực tế. " +
                "Đảm bảo người dùng hiểu rõ vấn đề và có thể áp dụng kiến thức.";
            
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
            payload.put("max_tokens", 2000);
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
            } else if (e.getStatusCode().value() == 429) {
                return "Xin lỗi, đã vượt quá giới hạn API calls. Vui lòng thử lại sau.";
            } else {
                return "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau.";
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
}

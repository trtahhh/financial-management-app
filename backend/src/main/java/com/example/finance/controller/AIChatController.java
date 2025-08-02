package com.example.finance.controller;

import com.example.finance.dto.AIChatRequest;
import com.example.finance.dto.AIChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Slf4j
public class AIChatController {

    @PostMapping("/chat")
    public AIChatResponse chat(@RequestBody AIChatRequest request) {
        try {
            log.info("Received AI chat request: {}", request.getMessage());
            
            // Mock AI response for demo
            String answer = "Đây là phản hồi demo từ AI. Bạn đã hỏi: \"" + request.getMessage() + 
                           "\". Tôi có thể giúp bạn quản lý tài chính cá nhân, theo dõi thu chi, và đưa ra lời khuyên về tiết kiệm.";
            
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
}

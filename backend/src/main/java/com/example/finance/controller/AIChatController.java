package com.example.finance.controller;

import com.example.finance.dto.AIChatRequest;
import com.example.finance.dto.AIChatResponse;
import com.example.finance.service.AIFinanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Slf4j
public class AIChatController {

    @Autowired
    private AIFinanceService aiFinanceService;

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
}

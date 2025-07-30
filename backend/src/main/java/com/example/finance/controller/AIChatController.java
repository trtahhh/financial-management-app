package com.example.finance.controller;

import com.example.finance.dto.AIChatRequest;
import com.example.finance.dto.AIChatResponse;
import com.example.finance.service.AIChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIChatController {

    @Autowired
    private AIChatService aiChatService;

    @PostMapping("/chat")
    public AIChatResponse chat(@RequestBody AIChatRequest request) {
        String answer = aiChatService.askAI(request.getMessage());
        AIChatResponse resp = new AIChatResponse();
        resp.setAnswer(answer);
        return resp;
    }
}

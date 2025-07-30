package com.example.finance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.json.JSONArray;
import org.json.JSONObject;


@Service
public class AIChatService {
    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.model}")
    private String model;

    private final WebClient webClient = WebClient.builder().build();

    public String askAI(String prompt) {
        JSONObject payload = new JSONObject();
        payload.put("model", model);

        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.put(userMsg);

        payload.put("messages", messages);

        String response = webClient.post()
            .uri(apiUrl)
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload.toString())
            .retrieve()
            .bodyToMono(String.class)
            .block();

        JSONObject jsonResponse = new JSONObject(response);
        String content = jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        return content;
    }
}

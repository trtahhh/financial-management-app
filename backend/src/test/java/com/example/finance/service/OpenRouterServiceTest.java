package com.example.finance.service;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for OpenRouterService - AI API integration with WebClient
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class OpenRouterServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @InjectMocks
    private OpenRouterService openRouterService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(openRouterService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(openRouterService, "baseUrl", "https://openrouter.ai/api/v1");
        ReflectionTestUtils.setField(openRouterService, "defaultModel", "anthropic/claude-3.5-sonnet");
        ReflectionTestUtils.setField(openRouterService, "webClient", webClient);
    }

    @Test
    void testChat_WithDefaultModel_Success() {
        // Given: Valid API key and successful response
        String prompt = "How to save money?";
        String mockResponse = createSuccessResponse("Here are 5 tips to save money...");
        
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.acceptCharset(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockResponse));
        
        // When
        String result = openRouterService.chat(prompt);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("tips to save money"));
    }

    @Test
    void testChat_WithCustomModel_Success() {
        // Given: Custom model specified
        String prompt = "Analyze budget";
        String customModel = "gpt-4";
        String mockResponse = createSuccessResponse("Budget analysis: ...");
        
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.acceptCharset(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockResponse));
        
        // When
        String result = openRouterService.chat(prompt, customModel);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("Budget analysis"));
    }

    @Test
    void testChat_MissingApiKey() {
        // Given: No API key configured
        ReflectionTestUtils.setField(openRouterService, "apiKey", "");
        
        // When
        String result = openRouterService.chat("Test prompt");
        
        // Then: Should return error message
        assertTrue(result.contains("chưa được cấu hình"));
    }

    @Test
    void testChat_EmptyResponse() {
        // Given: API returns empty response
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.acceptCharset(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(""));
        
        // When
        String result = openRouterService.chat("Test prompt");
        
        // Then: Should handle empty response
        assertTrue(result.contains("chưa phản hồi"));
    }

    @Test
    void testChat_401Unauthorized() {
        // Given: 401 Unauthorized error
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.acceptCharset(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
            .thenReturn(Mono.error(WebClientResponseException.create(401, "Unauthorized", null, null, null)));
        
        // When
        String result = openRouterService.chat("Test prompt");
        
        // Then: Should return API key error message
        assertTrue(result.contains("API key") && result.contains("không hợp lệ"));
    }

    @Test
    void testChat_402PaymentRequired_UsesFallback() {
        // Given: 402 Payment Required error (quota exceeded)
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.acceptCharset(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
            .thenReturn(Mono.error(WebClientResponseException.create(402, "Payment Required", null, null, null)));
        
        // When
        String result = openRouterService.chat("tiết kiệm như thế nào");
        
        // Then: Should use fallback financial advice
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    void testChat_429RateLimitExceeded() {
        // Given: 429 Rate Limit error
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.acceptCharset(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
            .thenReturn(Mono.error(WebClientResponseException.create(429, "Too Many Requests", null, null, null)));
        
        // When
        String result = openRouterService.chat("Test prompt");
        
        // Then: Should return rate limit message
        assertTrue(result.contains("vượt quá giới hạn"));
    }

    @Test
    void testIsAvailable_Success() {
        // Given: Valid API and successful models endpoint
        String mockModelsResponse = "{\"data\": [{\"id\": \"model1\"}]}";
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockModelsResponse));
        
        // When
        boolean available = openRouterService.isAvailable();
        
        // Then
        assertTrue(available);
    }

    @Test
    void testIsAvailable_NoApiKey() {
        // Given: No API key
        ReflectionTestUtils.setField(openRouterService, "apiKey", "");
        
        // When
        boolean available = openRouterService.isAvailable();
        
        // Then
        assertFalse(available);
    }

    @Test
    void testIsAvailable_ApiError() {
        // Given: API error when checking availability
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("API error")));
        
        // When
        boolean available = openRouterService.isAvailable();
        
        // Then
        assertFalse(available);
    }

    @Test
    void testGetAvailableModels_Success() {
        // Given: Successful models response
        String mockModelsResponse = "{\"data\": [{\"id\": \"model1\"}, {\"id\": \"model2\"}]}";
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockModelsResponse));
        
        // When
        String[] models = openRouterService.getAvailableModels();
        
        // Then
        assertNotNull(models);
        assertEquals(2, models.length);
    }

    @Test
    void testGetAvailableModels_NoApiKey() {
        // Given: No API key
        ReflectionTestUtils.setField(openRouterService, "apiKey", "");
        
        // When
        String[] models = openRouterService.getAvailableModels();
        
        // Then
        assertNotNull(models);
        assertEquals(0, models.length);
    }

    @Test
    void testGetDefaultModel() {
        // When
        String model = openRouterService.getDefaultModel();
        
        // Then
        assertEquals("anthropic/claude-3.5-sonnet", model);
    }

    // Helper method to create success response
    private String createSuccessResponse(String content) {
        JSONObject response = new JSONObject();
        JSONObject choice = new JSONObject();
        JSONObject message = new JSONObject();
        
        message.put("role", "assistant");
        message.put("content", content);
        choice.put("message", message);
        choice.put("finish_reason", "stop");
        
        response.put("choices", new org.json.JSONArray().put(choice));
        response.put("model", "anthropic/claude-3.5-sonnet");
        
        return response.toString();
    }
}

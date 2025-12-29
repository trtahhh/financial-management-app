package com.example.finance.service;

import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for OcrService - OCR.space API integration
 */
@ExtendWith(MockitoExtension.class)
public class OcrServiceTest {

    @Mock
    private OkHttpClient httpClient;

    @InjectMocks
    private OcrService ocrService;

    @BeforeEach
    void setUp() {
        // Set required configuration values
        ReflectionTestUtils.setField(ocrService, "provider", "ocrspace");
        ReflectionTestUtils.setField(ocrService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(ocrService, "endpoint", "https://api.ocr.space/parse/image");
    }

    @Test
    void testParseInvoice_InvalidProvider() {
        // Given: Invalid OCR provider
        ReflectionTestUtils.setField(ocrService, "provider", "invalid");
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "receipt.jpg", "image/jpeg", "test data".getBytes()
        );
        
        // When & Then: Should throw exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ocrService.parseInvoice(file);
        });
        
        assertTrue(exception.getMessage().contains("không được hỗ trợ"));
    }

    @Test
    void testParseInvoice_MissingApiKey() {
        // Given: Missing API key
        ReflectionTestUtils.setField(ocrService, "apiKey", "");
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "receipt.jpg", "image/jpeg", "test data".getBytes()
        );
        
        // When & Then: Should throw exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ocrService.parseInvoice(file);
        });
        
        assertTrue(exception.getMessage().contains("Thiếu OCR API key"));
    }

    @Test
    void testParseInvoice_SuccessFirstTry() throws IOException {
        // Given: Successful OCR response on first try
        MockMultipartFile file = new MockMultipartFile(
            "file", "receipt.jpg", "image/jpeg", "test image data".getBytes()
        );
        
        // Mock successful response
        String mockResponseBody = createSuccessResponse("Invoice text content");
        Response mockResponse = createMockResponse(200, mockResponseBody);
        
        Call mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(httpClient.newCall(any(Request.class))).thenReturn(mockCall);
        
        // Use reflection to inject mocked httpClient
        ReflectionTestUtils.setField(ocrService, "httpClient", httpClient);
        
        // When
        JSONObject result = ocrService.parseInvoice(file);
        
        // Then
        assertNotNull(result);
        assertTrue(result.has("ParsedResults"));
        assertTrue(result.has("_elapsedMs"));
    }

    @Test
    void testParseInvoice_FirstTryEmpty_SecondSuccess() throws IOException {
        // Given: Empty first response, successful second response
        MockMultipartFile file = new MockMultipartFile(
            "file", "receipt.jpg", "image/jpeg", "test image data".getBytes()
        );
        
        // Mock empty first response, successful second response
        String emptyResponse = createEmptyResponse();
        String successResponse = createSuccessResponse("Receipt text");
        
        Response emptyMockResponse = createMockResponse(200, emptyResponse);
        Response successMockResponse = createMockResponse(200, successResponse);
        
        Call mockCall1 = mock(Call.class);
        Call mockCall2 = mock(Call.class);
        when(mockCall1.execute()).thenReturn(emptyMockResponse);
        when(mockCall2.execute()).thenReturn(successMockResponse);
        
        when(httpClient.newCall(any(Request.class)))
            .thenReturn(mockCall1)
            .thenReturn(mockCall2);
        
        ReflectionTestUtils.setField(ocrService, "httpClient", httpClient);
        
        // When
        JSONObject result = ocrService.parseInvoice(file);
        
        // Then
        assertNotNull(result);
        assertTrue(result.has("_fallbackLanguage")); // Second try marker
        assertEquals("auto", result.getString("_fallbackLanguage"));
    }

    @Test
    void testParseInvoice_AllTriesFail_ReturnsFinalResult() throws IOException {
        // Given: All three tries return empty
        MockMultipartFile file = new MockMultipartFile(
            "file", "receipt.jpg", "image/jpeg", "test image data".getBytes()
        );
        
        String emptyResponse = createEmptyResponse();
        Response emptyMockResponse = createMockResponse(200, emptyResponse);
        
        Call mockCall1 = mock(Call.class);
        Call mockCall2 = mock(Call.class);
        Call mockCall3 = mock(Call.class);
        when(mockCall1.execute()).thenReturn(emptyMockResponse);
        when(mockCall2.execute()).thenReturn(emptyMockResponse);
        when(mockCall3.execute()).thenReturn(emptyMockResponse);
        
        when(httpClient.newCall(any(Request.class)))
            .thenReturn(mockCall1)
            .thenReturn(mockCall2)
            .thenReturn(mockCall3);
        
        ReflectionTestUtils.setField(ocrService, "httpClient", httpClient);
        
        // When
        JSONObject result = ocrService.parseInvoice(file);
        
        // Then: Should return third attempt result
        assertNotNull(result);
        assertTrue(result.has("_fallbackLanguage"));
        assertTrue(result.has("_fallbackEngine"));
        assertEquals(1, result.getInt("_fallbackEngine"));
    }

    @Test
    void testParseInvoice_ErrorResponse() throws IOException {
        // Given: OCR API returns error
        MockMultipartFile file = new MockMultipartFile(
            "file", "receipt.jpg", "image/jpeg", "test image data".getBytes()
        );
        
        String errorResponse = createErrorResponse("OCR processing failed");
        Response errorMockResponse = createMockResponse(200, errorResponse);
        
        Call mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(errorMockResponse);
        when(httpClient.newCall(any(Request.class))).thenReturn(mockCall);
        
        ReflectionTestUtils.setField(ocrService, "httpClient", httpClient);
        
        // When
        JSONObject result = ocrService.parseInvoice(file);
        
        // Then: Should handle error response (not considered empty)
        assertNotNull(result);
        assertTrue(result.has("IsErroredOnProcessing"));
    }

    @Test
    void testParseInvoice_HttpErrorStatus() throws IOException {
        // Given: HTTP error status (non-200)
        MockMultipartFile file = new MockMultipartFile(
            "file", "receipt.jpg", "image/jpeg", "test image data".getBytes()
        );
        
        String errorResponse = "{}";
        Response errorMockResponse = createMockResponse(500, errorResponse);
        
        Call mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(errorMockResponse);
        when(httpClient.newCall(any(Request.class))).thenReturn(mockCall);
        
        ReflectionTestUtils.setField(ocrService, "httpClient", httpClient);
        
        // When
        JSONObject result = ocrService.parseInvoice(file);
        
        // Then: Should include HTTP status
        assertNotNull(result);
        assertTrue(result.has("httpStatus"));
        assertEquals(500, result.getInt("httpStatus"));
    }

    // Helper methods to create mock responses

    private String createSuccessResponse(String parsedText) {
        JSONObject response = new JSONObject();
        response.put("ParsedResults", new JSONArray()
            .put(new JSONObject()
                .put("ParsedText", parsedText)
                .put("ErrorMessage", "")
                .put("ErrorDetails", "")
            )
        );
        response.put("OCRExitCode", 1);
        response.put("IsErroredOnProcessing", false);
        return response.toString();
    }

    private String createEmptyResponse() {
        JSONObject response = new JSONObject();
        response.put("ParsedResults", new JSONArray()
            .put(new JSONObject()
                .put("ParsedText", "")
                .put("ErrorMessage", "")
                .put("ErrorDetails", "")
            )
        );
        response.put("OCRExitCode", 1);
        response.put("IsErroredOnProcessing", false);
        return response.toString();
    }

    private String createErrorResponse(String errorMessage) {
        JSONObject response = new JSONObject();
        response.put("IsErroredOnProcessing", true);
        response.put("ErrorMessage", errorMessage);
        response.put("ParsedResults", new JSONArray());
        return response.toString();
    }

    private Response createMockResponse(int code, String body) {
        // OkHttp Response requires body for execute(), mock must return non-null body
        ResponseBody responseBody = mock(ResponseBody.class);
        try {
            when(responseBody.string()).thenReturn(body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return new Response.Builder()
            .request(new Request.Builder().url("http://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(code == 200 ? "OK" : "Error")
            .body(responseBody)
            .build();
    }
}

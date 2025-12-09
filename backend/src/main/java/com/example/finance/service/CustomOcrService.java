package com.example.finance.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Custom OCR Service - Sử dụng mô hình OCR tự huấn luyện (YOLO + EasyOCR)
 * Hỗ trợ tiếng Anh và tiếng Việt
 */
@Service
@Slf4j
public class CustomOcrService {

    @Value("${ocr.custom.endpoint:http://localhost:8001/api/ocr/parse-invoice}")
    private String customOcrEndpoint;

    @Value("${ocr.custom.enabled:true}")
    private boolean customOcrEnabled;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(60))
            .writeTimeout(Duration.ofSeconds(60))
            .callTimeout(Duration.ofSeconds(90))
            .build();

    /**
     * Parse hóa đơn sử dụng custom OCR service
     */
    public JSONObject parseInvoice(MultipartFile file) throws IOException {
        if (!customOcrEnabled) {
            throw new IllegalStateException("Custom OCR service không được bật. Vui lòng cấu hình ocr.custom.enabled=true");
        }

        log.info("Gửi request tới Custom OCR Service: {}", customOcrEndpoint);

        RequestBody fileBody = RequestBody.create(
                file.getBytes(),
                MediaType.parse(file.getContentType() != null ? file.getContentType() : "image/jpeg")
        );

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", 
                        file.getOriginalFilename() != null ? file.getOriginalFilename() : "invoice.jpg", 
                        fileBody)
                .build();

        Request request = new Request.Builder()
                .url(customOcrEndpoint)
                .post(requestBody)
                .build();

        long start = System.currentTimeMillis();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "{}";
            long elapsed = System.currentTimeMillis() - start;
            
            log.info("Custom OCR response received in {}ms, status: {}", elapsed, response.code());
            
            JSONObject json = new JSONObject(body);
            
            if (!response.isSuccessful()) {
                log.error("Custom OCR failed with status {}: {}", response.code(), body);
                throw new IOException("Custom OCR service error: " + response.code());
            }

            // Transform response to match expected format
            return transformCustomOcrResponse(json, elapsed);
            
        } catch (java.net.SocketTimeoutException ste) {
            log.error("Custom OCR timeout after {}ms", System.currentTimeMillis() - start);
            throw new IOException("Custom OCR service timeout. Vui lòng thử lại.", ste);
        }
    }

    /**
     * Transform response từ custom OCR service sang format chuẩn
     */
    private JSONObject transformCustomOcrResponse(JSONObject response, long elapsed) {
        JSONObject result = new JSONObject();
        
        try {
            // Check if successful
            boolean success = response.optBoolean("success", false);
            
            if (!success) {
                result.put("IsErroredOnProcessing", true);
                result.put("ErrorMessage", response.optString("message", "Unknown error"));
                return result;
            }

            // Extract data
            JSONObject data = response.optJSONObject("data");
            if (data == null) {
                result.put("IsErroredOnProcessing", true);
                result.put("ErrorMessage", "No data in response");
                return result;
            }

            // Build parsed results - Deduplicate to avoid repetition
            JSONArray parsedResults = new JSONArray();
            JSONObject parsedText = new JSONObject();
            
            // Use LinkedHashSet to deduplicate while preserving order
            Set<String> seenLines = new LinkedHashSet<>();
            
            // Add main fields FIRST (these are most important)
            String company = normalizeText(data.optString("company", ""));
            String date = normalizeText(data.optString("date", ""));
            String total = normalizeText(data.optString("total", ""));
            String address = normalizeText(data.optString("address", ""));
            
            if (!company.isEmpty()) {
                seenLines.add(company);
            }
            if (!date.isEmpty()) {
                seenLines.add(date);
            }
            if (!total.isEmpty()) {
                seenLines.add(total);
            }
            if (!address.isEmpty()) {
                seenLines.add(address);
            }
            
            // Add other detection texts (skip duplicates and near-duplicates)
            JSONArray detections = data.optJSONArray("detections");
            if (detections != null) {
                for (int i = 0; i < detections.length(); i++) {
                    JSONObject det = detections.getJSONObject(i);
                    String detText = normalizeText(det.optString("text", ""));
                    
                    if (!detText.isEmpty()) {
                        // Check if this text is not a duplicate or substring of existing lines
                        if (!isDuplicate(detText, seenLines)) {
                            seenLines.add(detText);
                        }
                    }
                }
            }
            
            // Add rawText if available (check for duplicates)
            String rawText = data.optString("rawText", "");
            if (!rawText.isEmpty()) {
                String[] rawLines = rawText.split("\n");
                for (String line : rawLines) {
                    String normalizedLine = normalizeText(line);
                    if (!normalizedLine.isEmpty()) {
                        if (!isDuplicate(normalizedLine, seenLines)) {
                            seenLines.add(normalizedLine);
                        }
                    }
                }
            }
            
            // Build final text from deduplicated lines
            StringBuilder fullText = new StringBuilder();
            for (String line : seenLines) {
                fullText.append(line).append("\n");
            }
            
            parsedText.put("ParsedText", fullText.toString().trim());
            parsedResults.put(parsedText);
            
            result.put("ParsedResults", parsedResults);
            result.put("IsErroredOnProcessing", false);
            result.put("_elapsedMs", elapsed);
            result.put("_source", "custom-ocr");
            
            // Store structured data for easier extraction
            result.put("_structuredData", data);
            
            log.info("Transformed OCR response: {} chars extracted", fullText.length());
            
        } catch (Exception e) {
            log.error("Error transforming custom OCR response", e);
            result.put("IsErroredOnProcessing", true);
            result.put("ErrorMessage", "Error transforming response: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Normalize text để so sánh
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        return text.trim();
    }
    
    /**
     * Check nếu một dòng text là duplicate hoặc gần giống với các dòng đã có
     */
    private boolean isDuplicate(String text, Set<String> seenLines) {
        for (String seenLine : seenLines) {
            // Exact match
            if (text.equalsIgnoreCase(seenLine)) {
                return true;
            }
            // One is substring of the other
            if (text.toLowerCase().contains(seenLine.toLowerCase()) ||
                seenLine.toLowerCase().contains(text.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if custom OCR service is available
     */
    public boolean isAvailable() {
        if (!customOcrEnabled) {
            return false;
        }
        
        try {
            // Try to connect to health endpoint
            String healthUrl = customOcrEndpoint.replace("/api/ocr/parse-invoice", "/health");
            
            Request request = new Request.Builder()
                    .url(healthUrl)
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.warn("Custom OCR service health check failed: {}", e.getMessage());
            return false;
        }
    }
}

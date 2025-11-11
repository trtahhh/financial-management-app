package com.example.finance.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

@Service
@Slf4j
public class OcrService {

 @Value("${ocr.provider:ocrspace}")
 private String provider;

 @Value("${ocr.ocrspace.api-key:}")
 private String apiKey;

 @Value("${ocr.ocrspace.endpoint:https://api.ocr.space/parse/image}")
 private String endpoint;

 // Khởi tạo client với timeout cấu hình để tránh treo lâu gây SocketTimeoutException
 private final OkHttpClient httpClient = new OkHttpClient.Builder()
 .connectTimeout(Duration.ofSeconds(15))
 .readTimeout(Duration.ofSeconds(45))
 .writeTimeout(Duration.ofSeconds(45))
 .callTimeout(Duration.ofSeconds(60))
 .build();

 public JSONObject parseInvoice(MultipartFile file) throws IOException {
 if (!"ocrspace".equalsIgnoreCase(provider)) {
 throw new IllegalStateException("OCR provider không được hỗ trợ: " + provider);
 }

 if (apiKey == null || apiKey.isBlank()) {
 throw new IllegalStateException("Thiếu OCR API key. Vui lòng cấu hình ocr.ocrspace.api-key");
 }

 RequestBody fileBody = RequestBody.create(file.getBytes(), MediaType.parse(file.getContentType() != null ? file.getContentType() : "application/octet-stream"));

 String filename = file.getOriginalFilename();
 // Lần 1: eng + engine 2
 JSONObject firstTry = doOcrRequest(filename, fileBody, "eng", "2");
 if (!isEmptyResult(firstTry)) return firstTry;

 // Lần 2: auto detect + engine 2
 JSONObject second = doOcrRequest(filename, fileBody, "", "2");
 if (!isEmptyResult(second)) {
 second.put("_fallbackLanguage", "auto");
 return second;
 }

 // Lần 3: auto detect + engine 1 (nhanh hơn / khác model)
 JSONObject third = doOcrRequest(filename, fileBody, "", "1");
 third.put("_fallbackLanguage", "auto");
 third.put("_fallbackEngine", 1);
 return third;
 }

 private JSONObject doOcrRequest(String filename, RequestBody fileBody, String language, String engine) throws IOException {
 MultipartBody.Builder builder = new MultipartBody.Builder()
 .setType(MultipartBody.FORM)
 .addFormDataPart("file", filename != null ? filename : "invoice.jpg", fileBody)
 .addFormDataPart("isOverlayRequired", "false")
 .addFormDataPart("OCREngine", engine)
 .addFormDataPart("scale", "true")
 .addFormDataPart("isTable", "true")
 .addFormDataPart("detectOrientation", "true");
 
 // Chỉ thêm language nếu không rỗng
 if (language != null && !language.trim().isEmpty()) {
 builder.addFormDataPart("language", language);
 }
 
 MultipartBody requestBody = builder.build();

 Request request = new Request.Builder()
 .url(endpoint)
 .addHeader("apikey", apiKey)
 .post(requestBody)
 .build();

 long start = System.currentTimeMillis();
 try (Response response = httpClient.newCall(request).execute()) {
 String body = response.body() != null ? response.body().string() : "{}";
 JSONObject json = new JSONObject(body);
 json.put("_elapsedMs", System.currentTimeMillis() - start);
 if (!response.isSuccessful()) {
 json.put("httpStatus", response.code());
 }
 return json;
 } catch (java.net.SocketTimeoutException ste) {
 JSONObject err = new JSONObject();
 err.put("IsErroredOnProcessing", true);
 err.put("ErrorMessage", "Timeout khi gọi OCR provider (quá thời gian chờ). Hãy thử lại hoặc dùng ảnh rõ hơn.");
 err.put("_timeout", true);
 throw new IOException("OCR timeout", ste);
 }
 }

 private boolean isEmptyResult(JSONObject json) {
 try {
 if (json.optBoolean("IsErroredOnProcessing", false)) return false; // có lỗi, không coi là rỗng
 org.json.JSONArray arr = json.optJSONArray("ParsedResults");
 if (arr == null || arr.isEmpty()) return true;
 for (int i = 0; i < arr.length(); i++) {
 org.json.JSONObject it = arr.getJSONObject(i);
 String t = it.optString("ParsedText", "");
 if (t != null && !t.trim().isEmpty()) return false;
 }
 return true;
 } catch (Exception e) {
 return true;
 }
 }
}




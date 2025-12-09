package com.example.finance.controller;

import com.example.finance.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

 @Value("${app.upload.dir:uploads}")
 private String uploadDir;

 @PostMapping("/upload")
 public ResponseEntity<ApiResponse> uploadFile(@RequestParam("file") MultipartFile file) {
 try {
 if (file.isEmpty()) {
 return ResponseEntity.badRequest()
 .body(new ApiResponse(false, "File không được để trống", null));
 }

 // Kiểm tra loại file
 String contentType = file.getContentType();
 if (contentType == null || !contentType.startsWith("image/")) {
 return ResponseEntity.badRequest()
 .body(new ApiResponse(false, "Chỉ chấp nhận file ảnh", null));
 }

 // Tạo thư mục uploads nếu chưa có
 Path uploadPath = Paths.get(uploadDir);
 if (!Files.exists(uploadPath)) {
 Files.createDirectories(uploadPath);
 }

 // Tạo tên file unique
 String originalFilename = file.getOriginalFilename();
 String extension = "";
 if (originalFilename != null && originalFilename.contains(".")) {
 extension = originalFilename.substring(originalFilename.lastIndexOf("."));
 }
 String filename = UUID.randomUUID().toString() + extension;

 // Lưu file
 Path filePath = uploadPath.resolve(filename);
 Files.copy(file.getInputStream(), filePath);

 // Trả về URL của file - sử dụng /uploads để frontend có thể proxy
 String fileUrl = "/uploads/" + filename;
 
 return ResponseEntity.ok(
 new ApiResponse(true, "Upload file thành công", fileUrl)
 );

 } catch (IOException e) {
 return ResponseEntity.internalServerError()
 .body(new ApiResponse(false, "Lỗi lưu file: " + e.getMessage(), null));
 }
 }

 @GetMapping("/uploads/{filename:.+}")
 public ResponseEntity<byte[]> getFile(@PathVariable String filename) {
 try {
 Path filePath = Paths.get(uploadDir).resolve(filename);
 if (!Files.exists(filePath)) {
 return ResponseEntity.notFound().build();
 }

 byte[] fileContent = Files.readAllBytes(filePath);
 
 // Xác định content type
 String contentType = Files.probeContentType(filePath);
 if (contentType == null) {
 contentType = "application/octet-stream";
 }

 return ResponseEntity.ok()
 .header("Content-Type", contentType)
 .body(fileContent);

 } catch (IOException e) {
 return ResponseEntity.internalServerError().build();
 }
 }
}

package com.example.finance.exception;

import com.example.finance.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

 @ExceptionHandler(CustomException.class)
 public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex, WebRequest request) {
 log.error("Custom exception occurred: {}", ex.getMessage(), ex);
 
 ErrorResponse error = ErrorResponse.builder()
 .timestamp(LocalDateTime.now())
 .status(HttpStatus.BAD_REQUEST.value())
 .error("Bad Request")
 .message(ex.getMessage())
 .path(request.getDescription(false))
 .build();
 
 return ResponseEntity.badRequest().body(error);
 }

 @ExceptionHandler(ResourceNotFoundException.class)
 public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
 log.error("Resource not found: {}", ex.getMessage(), ex);
 
 ErrorResponse error = ErrorResponse.builder()
 .timestamp(LocalDateTime.now())
 .status(HttpStatus.NOT_FOUND.value())
 .error("Not Found")
 .message(ex.getMessage())
 .path(request.getDescription(false))
 .build();
 
 return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
 }

 @ExceptionHandler(AuthenticationException.class)
 public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
 log.error("Authentication failed: {}", ex.getMessage(), ex);
 
 ErrorResponse error = ErrorResponse.builder()
 .timestamp(LocalDateTime.now())
 .status(HttpStatus.UNAUTHORIZED.value())
 .error("Unauthorized")
 .message("Authentication failed")
 .path(request.getDescription(false))
 .build();
 
 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
 }

 @ExceptionHandler(BadCredentialsException.class)
 public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
 log.error("Bad credentials: {}", ex.getMessage(), ex);
 
 ErrorResponse error = ErrorResponse.builder()
 .timestamp(LocalDateTime.now())
 .status(HttpStatus.UNAUTHORIZED.value())
 .error("Unauthorized")
 .message("Invalid username or password")
 .path(request.getDescription(false))
 .build();
 
 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
 }

 @ExceptionHandler(AccessDeniedException.class)
 public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
 log.error("Access denied: {}", ex.getMessage(), ex);
 
 ErrorResponse error = ErrorResponse.builder()
 .timestamp(LocalDateTime.now())
 .status(HttpStatus.FORBIDDEN.value())
 .error("Forbidden")
 .message("Access denied")
 .path(request.getDescription(false))
 .build();
 
 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
 }

 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
 log.error("Validation failed: {}", ex.getMessage(), ex);
 
 Map<String, String> errors = new HashMap<>();
 ex.getBindingResult().getAllErrors().forEach((error) -> {
 String fieldName = ((FieldError) error).getField();
 String errorMessage = error.getDefaultMessage();
 errors.put(fieldName, errorMessage);
 });
 
 ErrorResponse error = ErrorResponse.builder()
 .timestamp(LocalDateTime.now())
 .status(HttpStatus.BAD_REQUEST.value())
 .error("Validation Failed")
 .message("Invalid input data")
 .details(errors)
 .path(request.getDescription(false))
 .build();
 
 return ResponseEntity.badRequest().body(error);
 }

 @ExceptionHandler(Exception.class)
 public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
 log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
 
 ErrorResponse error = ErrorResponse.builder()
 .timestamp(LocalDateTime.now())
 .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
 .error("Internal Server Error")
 .message("An unexpected error occurred")
 .path(request.getDescription(false))
 .build();
 
 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
 }
}

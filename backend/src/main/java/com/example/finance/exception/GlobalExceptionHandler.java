package com.example.finance.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestControllerAdvice
@Order(100)
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustom(CustomException ex) {
        log.error("Handled CustomException", ex);
        return new ResponseEntity<>(Map.of(
            "code", HttpStatus.BAD_REQUEST.value(),
            "message", ex.getMessage()
        ), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex, HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri != null && uri.startsWith("/actuator")) {
            throw new RuntimeException(ex);
        }
        log.error("Unhandled exception", ex);
        return new ResponseEntity<>(Map.of(
            "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "message", "Internal server error"
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

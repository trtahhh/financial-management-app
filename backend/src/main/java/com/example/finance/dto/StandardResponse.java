package com.example.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandardResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;
    
    public static <T> StandardResponse<T> success(T data) {
        return new StandardResponse<>(true, "Success", data, null);
    }
    
    public static <T> StandardResponse<T> success(String message, T data) {
        return new StandardResponse<>(true, message, data, null);
    }
    
    public static <T> StandardResponse<T> error(String message) {
        return new StandardResponse<>(false, message, null, message);
    }
    
    public static <T> StandardResponse<T> error(String message, String error) {
        return new StandardResponse<>(false, message, null, error);
    }
}
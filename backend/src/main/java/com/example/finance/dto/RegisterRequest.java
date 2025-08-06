package com.example.finance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phone;
    private LocalDate birthday;
    private String gender;
    private String address;
    private String imageUrl;
}
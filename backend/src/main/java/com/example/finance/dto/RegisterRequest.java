package com.example.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RegisterRequest {
 private String username;
 private String password;
 private String email;
 private String fullName;
 private String phone;
 
 @JsonFormat(pattern = "yyyy-MM-dd")
 private LocalDate birthday;
 
 private String gender;
 private String address;
 private String imageUrl;
}

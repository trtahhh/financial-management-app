package com.example.finance.dto;

import com.example.finance.entity.User;
import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private UserDto user;

    public AuthResponse(String accessToken, User user) {
        this.accessToken = accessToken;
        this.user = new UserDto(user);
    }

    @Data
    public static class UserDto {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String role;
        private String imageUrl;

        public UserDto(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.fullName = user.getFullName();
            this.role = user.getRole();
            this.imageUrl = user.getImageUrl();
        }
    }
}

package com.example.finance.dto;

import com.example.finance.entity.User;
import com.example.finance.entity.UserProfile;
import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private UserDto user;
    private UserProfileDto profile;

    public AuthResponse(String accessToken, User user, UserProfile profile) {
        this.accessToken = accessToken;
        this.user = new UserDto(user);
        this.profile = profile != null ? new UserProfileDto(profile) : null;
    }

    @Data
    public static class UserDto {
        private Long id;
        private String username;
        private String email;
        private String role;

        public UserDto(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.role = user.getRole();
        }
    }

    @Data
    public static class UserProfileDto {
        private String fullName;
        private String phone;
        private String gender;
        private String address;
        private String imageUrl;
        private java.time.LocalDate birthday;

        public UserProfileDto(UserProfile profile) {
            this.fullName = profile.getFullName();
            this.phone = profile.getPhone();
            this.gender = profile.getGender();
            this.address = profile.getAddress();
            this.imageUrl = profile.getImageUrl();
            this.birthday = profile.getBirthday();
        }
    }
}
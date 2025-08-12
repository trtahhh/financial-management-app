package com.example.finance.service;

import com.example.finance.entity.User;
import com.example.finance.entity.UserProfile;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.UserProfileRepository;
import com.example.finance.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;  
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
@Transactional
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public UserProfile saveProfile(Long userId, Map<String, Object> profileData) {
        try {
            // Tìm profile hiện tại
            UserProfile existingProfile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Tạo mới nếu chưa có
                    UserProfile newProfile = new UserProfile();
                    User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    newProfile.setUser(user);
                    return newProfile;
                });

            // Cập nhật từng field một cách an toàn
            if (profileData.containsKey("fullName")) {
                existingProfile.setFullName((String) profileData.get("fullName"));
            }
            if (profileData.containsKey("phone")) {
                existingProfile.setPhone((String) profileData.get("phone"));
            }
            if (profileData.containsKey("birthday")) {
                String birthdayStr = (String) profileData.get("birthday");
                if (birthdayStr != null && !birthdayStr.trim().isEmpty()) {
                    try {
                        LocalDate birthday = LocalDate.parse(birthdayStr);
                        existingProfile.setBirthday(birthday);
                    } catch (DateTimeParseException e) {
                        log.warn("Invalid birthday format: {}", birthdayStr);
                    }
                }
            }
            if (profileData.containsKey("gender")) {
                existingProfile.setGender((String) profileData.get("gender"));
            }
            if (profileData.containsKey("address")) {
                existingProfile.setAddress((String) profileData.get("address"));
            }
            if (profileData.containsKey("imageUrl")) {
                existingProfile.setImageUrl((String) profileData.get("imageUrl"));
            }

            // Lưu profile
            return userProfileRepository.save(existingProfile);
            
        } catch (Exception e) {
            log.error("Error updating profile for userId: {}", userId, e);
            throw new RuntimeException("Lỗi lưu profile: " + e.getMessage());
        }
    }

    public Optional<UserProfile> findProfileByUserId(Long userId) {
        return userProfileRepository.findByUserId(userId);
    }

    /**
     * Tìm user theo email
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElse(null);
    }

    /**
     * Tìm user theo password reset token
     */
    public User findByPasswordResetToken(String token) {
        return userRepository.findByPasswordResetToken(token)
            .orElse(null);
    }

    /**
     * Tìm user theo verification token
     */
    public User findByVerificationToken(String token) {
        return userRepository.findByEmailVerificationToken(token)
            .orElse(null);
    }

    @Transactional
    public User updateUser(User user){
        return userRepository.save(user);
    }
}
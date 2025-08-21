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
import java.util.ArrayList;
import java.util.List;

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
            log.info("Starting to save profile for user ID: {} with data: {}", userId, profileData);
            
            // Tìm profile hiện tại
            UserProfile existingProfile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Tạo mới nếu chưa có
                    UserProfile newProfile = new UserProfile();
                    User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    newProfile.setUser(user);
                    log.info("Created new profile for user ID: {}", userId);
                    return newProfile;
                });

            log.info("Existing profile found: {}", existingProfile);

            // Cập nhật từng field một cách an toàn
            if (profileData.containsKey("fullName") && profileData.get("fullName") != null) {
                String fullName = (String) profileData.get("fullName");
                existingProfile.setFullName(fullName);
                log.info("Set fullName: '{}'", fullName);
            }
            
            if (profileData.containsKey("phone") && profileData.get("phone") != null) {
                String phone = (String) profileData.get("phone");
                existingProfile.setPhone(phone);
                log.info("Set phone: '{}'", phone);
            }
            
            if (profileData.containsKey("birthday") && profileData.get("birthday") != null) {
                Object birthdayObj = profileData.get("birthday");
                if (birthdayObj instanceof LocalDate) {
                    LocalDate birthday = (LocalDate) birthdayObj;
                    existingProfile.setBirthday(birthday);
                    log.info("Set birthday: '{}'", birthday);
                } else if (birthdayObj instanceof String) {
                    String birthdayStr = (String) birthdayObj;
                    if (!birthdayStr.trim().isEmpty()) {
                        try {
                            LocalDate birthday = LocalDate.parse(birthdayStr);
                            existingProfile.setBirthday(birthday);
                            log.info("Set birthday from string: '{}' -> '{}'", birthdayStr, birthday);
                        } catch (DateTimeParseException e) {
                            log.warn("Invalid birthday format: {}", birthdayStr);
                        }
                    }
                }
            }
            
            if (profileData.containsKey("gender") && profileData.get("gender") != null) {
                String gender = (String) profileData.get("gender");
                existingProfile.setGender(gender);
                log.info("Set gender: '{}'", gender);
            } else {
                log.info("Gender not found in profileData or is null");
            }
            
            if (profileData.containsKey("address") && profileData.get("address") != null) {
                String address = (String) profileData.get("address");
                existingProfile.setAddress(address);
                log.info("Set address: '{}'", address);
            }
            
            if (profileData.containsKey("imageUrl") && profileData.get("imageUrl") != null) {
                String imageUrl = (String) profileData.get("imageUrl");
                existingProfile.setImageUrl(imageUrl);
                log.info("Set imageUrl: '{}'", imageUrl);
            }

            log.info("Profile before saving: {}", existingProfile);

            // Lưu profile
            UserProfile savedProfile = userProfileRepository.save(existingProfile);
            log.info("Profile saved successfully for user ID: {} with data: {}", userId, profileData);
            log.info("Final saved profile: {}", savedProfile);
            return savedProfile;
            
        } catch (Exception e) {
            log.error("Error saving profile for user ID: {}", userId, e);
            throw new RuntimeException("Failed to save profile: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách user có mục tiêu đang hoạt động
     */
    public List<User> getUsersWithActiveGoals() {
        try {
            return userRepository.findUsersWithActiveGoals();
        } catch (Exception e) {
            log.error("Error getting users with active goals", e);
            return new ArrayList<>();
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

    /**
     * Xóa user theo ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        try {
            userRepository.deleteById(userId);
            log.info("User deleted successfully with ID: {}", userId);
        } catch (Exception e) {
            log.error("Error deleting user with ID: {}", userId, e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage());
        }
    }
}